package fi.dy.masa.litematica.gui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.InfoUtils;

/** Guided browser. Merely picking a file never registers a schematic or placement. */
public class GuiQuickPlace extends GuiSchematicLoad
{
    final ClientLevel world = this.mc.level;
    final SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
    private final List<UxButton> actions = new ArrayList<>();
    private int focused = -1;
    private Path selectedFile;
    private LitematicaSchematic selectedSchematic;
    private GuiQuickPlacePosition positionScreen;

    public GuiQuickPlace()
    {
        this.useTitleHierarchy = false;
        this.title = text("browse");
    }

    static String text(String key, Object... args)
    {
        return StringUtils.translate("litematica.gui.quick_place." + key, args);
    }

    boolean checkWorld(GuiBase gui)
    {
        if (this.world == null || this.mc.level != this.world || this.mc.player == null ||
            DataManager.getSchematicPlacementManager() != this.manager)
        {
            gui.addMessage(MessageType.ERROR, "litematica.gui.quick_place.world_changed");
            return false;
        }
        return true;
    }

    @Override
    protected void createButtons()
    {
        this.actions.clear();
        this.focused = -1;
        int width = (this.getScreenWidth() - 32) / 3;
        int y = this.getScreenHeight() - 26;
        DirectoryEntry entry = this.getListWidget().getLastSelectedEntry();
        FileType type = entry == null ? null : FileType.fromFile(entry.getFullPath());
        boolean valid = type == FileType.LITEMATICA_SCHEMATIC || type == FileType.SCHEMATICA_SCHEMATIC ||
                        type == FileType.SPONGE_SCHEMATIC || type == FileType.VANILLA_STRUCTURE;
        UxButton next = new UxButton(12, y, width, 20, text("next"), text("next_tip"), text("files_path", DataManager.getSchematicsBaseDirectory().toAbsolutePath()));
        next.setEnabled(valid);
        this.actions.add(next);
        this.addButton(next, (button, mouse) -> this.next());
        // File manager owns renames and JSON/TXT lists; only schematic previews need this action.
        UxButton materials = new UxButton(16 + width, y, width, 20, text("materials"),
                StringUtils.translate("litematica.gui.button.hover.material_list_shift_to_select_sub_regions"));
        materials.setEnabled(valid);
        this.actions.add(materials);
        this.addButton(materials, (button, mouse) -> {
            if (this.checkWorld(this)) this.previewSelectedMaterials();
        });
        UxButton cancel = new UxButton(20 + width * 2, y, width, 20, text("cancel"), text("cancel_tip"));
        this.actions.add(cancel);
        this.addButton(cancel,
                       (button, mouse) -> openGui(this.getParent()));
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.addLabel(12, this.getScreenHeight() - 42, this.getScreenWidth() - 24, 12, 0xFFFFFFFF,
                      text("browser_hint"));
    }

    @Override
    public boolean onKeyTyped(KeyEvent input)
    {
        if (input.key() == 258)
        {
            int step = input.hasShiftDown() ? -1 : 1;
            do
            {
                // -1 returns keyboard input to the native file list/search field.
                this.focused = Math.floorMod(this.focused + 1 + step, this.actions.size() + 1) - 1;
            }
            while (this.focused >= 0 && !this.actions.get(this.focused).isEnabled());
            for (int i = 0; i < this.actions.size(); ++i) this.actions.get(i).setKeyboardFocused(i == this.focused);
            if (this.focused >= 0)
                this.mc.getNarrator().saySystemNow(String.join(". ", this.actions.get(this.focused).getHoverStrings()));
            return true;
        }
        if ((input.key() == 257 || input.key() == 335 || input.key() == 32) && this.focused >= 0)
        {
            this.actions.get(this.focused).activate();
            return true;
        }
        // Do not edit a still-open search box behind a focused footer button.
        if (this.focused >= 0 && input.key() != 256) return true;
        return super.onKeyTyped(input);
    }

    @Override
    public boolean onCharTyped(CharacterEvent input)
    {
        return this.focused >= 0 || super.onCharTyped(input);
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent input, boolean doubleClick)
    {
        this.focused = -1;
        for (UxButton action : this.actions) action.setKeyboardFocused(false);
        return super.onMouseClicked(input, doubleClick);
    }

    static ButtonGeneric button(GuiBase gui, int x, int y, int width, String key, Runnable action)
    {
        return gui.addButton(new UxButton(x, y, width, 20, text(key), text(key + "_tip")),
                             (button, mouse) -> action.run());
    }

    static void keepError(MessageType type, int duration, String key, Object... args)
    {
        // GuiBase messages disappear when changing screens; keep failures visible after closing.
        if (type == MessageType.ERROR || type == MessageType.WARNING)
            InfoUtils.showInGameMessage(type, (long) duration, key, args);
    }

    @Override
    public void addGuiMessage(MessageType type, int duration, String key, Object... args)
    {
        super.addGuiMessage(type, duration, key, args);
        keepError(type, duration, key, args);
    }

    @Override
    public void setString(String message)
    {
        this.addMessage(MessageType.ERROR, message);
    }

    private void next()
    {
        if (!this.checkWorld(this)) return;
        DirectoryEntry entry = this.getListWidget().getLastSelectedEntry();
        if (entry != null && entry.getFullPath().equals(this.selectedFile) && this.selectedSchematic != null)
        {
            this.showPosition();
        }
        else
        {
            this.loadSelectedSchematic();
        }
    }

    @Override
    protected void onSchematicLoaded(LitematicaSchematic schematic, Path file)
    {
        if (!this.checkWorld(this)) return;
        this.positionScreen = null;
        this.selectedFile = file;
        this.selectedSchematic = schematic;
        this.showPosition();
    }

    private void showPosition()
    {
        if (this.positionScreen == null) this.positionScreen = new GuiQuickPlacePosition(this, this.selectedSchematic);
        openGui(this.positionScreen);
    }
}
