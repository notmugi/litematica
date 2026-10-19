package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiUxConfirm;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;

/** Confirmation is an operation-local continuation, never a global bypass. */
public final class UxSafety
{
    private static final Set<Integer> heldTriggers = new HashSet<>();

    private UxSafety() {}

    /** Each dialog owns its ticket; rejected and cancelled tickets cannot be reused. */
    public static final class Ticket
    {
        private boolean consumed;

        public synchronized boolean accept(boolean confirm, BooleanSupplier valid)
        {
            if (this.consumed) return false;
            this.consumed = true;
            return confirm && valid.getAsBoolean();
        }
    }

    public static void released(int key) { heldTriggers.remove(key); }

    public static boolean inputBlocked()
    {
        // GUI input handlers may not receive releases: also reconcile against physical keys.
        pollHeldTriggers();
        return fi.dy.masa.malilib.util.GuiUtils.getCurrentScreen() instanceof GuiUxConfirm || !heldTriggers.isEmpty();
    }

    public static void pollHeldTriggers()
    {
        heldTriggers.removeIf(code -> !KeybindMulti.isKeyDown(code));
    }

    /** Modifier keys belong to chords, not to the action's release latch. */
    public static boolean isTriggerKey(int code) { return code < 340 || code > 347; }

    // ponytail: schematic content uses the existing metadata revision (millisecond timestamp),
    // not an expensive full-volume hash. A true monotonic edit revision would remove that ceiling.
    public static Object placementState(SchematicPlacement placement)
    {
        if (placement == null) return "";
        return java.util.Arrays.asList(placement.toJson().deepCopy(), placement.isEnabled(),
                placement.ignoreEntities(), placement.getSelectedSubRegionName(),
                placement.getSchematic(), placement.getSchematic().getMetadata(), placement.getSchematic().getMetadata().getTimeModified(),
                placement.getSchematic().getMetadata().wasModifiedSinceSaved());
    }

    public static Object areaState(AreaSelection area) { return area == null ? "" : area.toJson().deepCopy(); }

    public static int boxCount(AreaSelection area)
    {
        return area == null ? 0 : area.getSelectedSubRegionBox() != null ? 1 : area.getAllSubRegionBoxes().size();
    }

    private static List<Object> context()
    {
        List<Object> state = new ArrayList<>();
        state.add(DataManager.getToolMode());
        state.add(DataManager.getToolMode().getPrimaryBlock());
        state.add(DataManager.getToolMode().getSecondaryBlock());
        state.add(fi.dy.masa.litematica.world.SchematicWorldHandler.getSchematicWorld());
        state.add(fi.dy.masa.litematica.tool.ToolModeData.DELETE.getUsePlacement());
        state.add(Configs.Visuals.ENABLE_RENDERING.getBooleanValue());
        var area = DataManager.getSelectionManager().getCurrentSelection();
        state.add(area);
        state.add(areaState(area));
        var placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
        state.add(placement);
        state.add(placementState(placement));
        var project = DataManager.getSchematicProjectsManager().getCurrentProject();
        state.add(project);
        if (project != null)
        {
            state.add(project.getCurrentVersionId());
            state.add(project.getCurrentPlacement());
            state.add(placementState(project.getCurrentPlacement()));
        }
        var range = DataManager.getRenderLayerRange();
        state.add(range.toJson().deepCopy());
        // Configs can change while a dialog is open (including via hotkeys).
        Configs.Generic.OPTIONS.forEach(config -> state.add(config.getAsJsonElement().deepCopy()));
        return state;
    }

    public static void confirm(boolean worldEdit, String key, Supplier<Object> targetState,
                               BooleanSupplier valid, Runnable action, Object... args)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (fi.dy.masa.malilib.util.GuiUtils.getCurrentScreen() instanceof GuiUxConfirm) return;
        if (!(worldEdit ? Configs.Generic.UX_CONFIRM_WORLD_EDITS : Configs.Generic.UX_CONFIRM_DISCARD).getBooleanValue())
        {
            if (valid.getAsBoolean()) action.run();
            return;
        }
        BooleanSupplier stillCurrent = captureContext(targetState, valid);
        // Poll while the dialog is open too; release events may be consumed by another GUI.
        for (var hotkey : Hotkeys.HOTKEY_LIST)
            for (int code : hotkey.getKeybind().getKeys())
                if (isTriggerKey(code) && KeybindMulti.isKeyDown(code)) heldTriggers.add(code);
        for (int code : new int[] { KeybindMulti.getKeyCode(mc.options.attackKey), KeybindMulti.getKeyCode(mc.options.useKey) })
            if (isTriggerKey(code) && KeybindMulti.isKeyDown(code)) heldTriggers.add(code);
        GuiBase.openGui(new GuiUxConfirm(fi.dy.masa.malilib.util.GuiUtils.getCurrentScreen(), StringUtils.translate(key, args),
                stillCurrent, action));
    }

    /** A snapshot for one approved operation and its queued stages, never a global bypass. */
    public static BooleanSupplier captureContext(Supplier<Object> targetState, BooleanSupplier valid)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        var world = mc.world;
        var player = mc.player;
        var connection = mc.getNetworkHandler();
        var server = mc.getServer();
        var state = context();
        Object target = targetState.get();
        boolean creative = player != null && EntityUtils.isCreativeMode(player);
        boolean tool = player != null && EntityUtils.hasToolItem(player);
        return () -> mc.world == world && mc.player == player && mc.getNetworkHandler() == connection &&
                mc.getServer() == server &&
                creative == (mc.player != null && EntityUtils.isCreativeMode(mc.player)) &&
                tool == (mc.player != null && EntityUtils.hasToolItem(mc.player)) &&
                state.equals(context()) && Objects.equals(target, targetState.get()) && valid.getAsBoolean();
    }

    /** Validate queued project follow-ups before they consult mutable current-placement fields. */
    public static fi.dy.masa.malilib.interfaces.ICompletionListener checkedContinuation(
            fi.dy.masa.malilib.interfaces.ICompletionListener listener)
    {
        if (listener == null) return null;
        BooleanSupplier valid = captureContext(() -> "", () -> true);
        Ticket ticket = new Ticket();
        return new fi.dy.masa.malilib.interfaces.ICompletionListener()
        {
            @Override
            public void onTaskCompleted()
            {
                MinecraftClient.getInstance().execute(() ->
                {
                    if (ticket.accept(true, valid)) listener.onTaskCompleted();
                    else fi.dy.masa.malilib.util.InfoUtils.showGuiOrInGameMessage(
                        fi.dy.masa.malilib.gui.Message.MessageType.WARNING, "litematica.gui.ux.safety.stale_move");
                });
            }

            @Override
            public void onTaskAborted() { ticket.accept(false, () -> false); listener.onTaskAborted(); }
        };
    }

    public static String pasteSettings()
    {
        return StringUtils.translate("litematica.gui.ux.safety.paste_settings",
                Configs.Generic.PASTE_REPLACE_BEHAVIOR.getStringValue(), Configs.Generic.PASTE_NBT_BEHAVIOR.getStringValue(),
                Configs.Generic.PASTE_IGNORE_ENTITIES.getBooleanValue(), Configs.Generic.PASTE_IGNORE_INVENTORY.getBooleanValue());
    }
}
