package fi.dy.masa.litematica.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.Message.MessageType;
import static fi.dy.masa.litematica.gui.GuiQuickPlace.button;
import static fi.dy.masa.litematica.gui.GuiQuickPlace.text;

/** The choice is explicit: no registration until Place or Load only is pressed. */
public class GuiQuickPlacePosition extends GuiBase
{
    private final GuiQuickPlace browser;
    private final LitematicaSchematic schematic;
    private boolean coordinates;
    private boolean committed;
    private final List<UxButton> actions = new ArrayList<>();
    private int focused = -1;
    private final GuiTextFieldGeneric[] fields = new GuiTextFieldGeneric[3];

    GuiQuickPlacePosition(GuiQuickPlace browser, LitematicaSchematic schematic)
    {
        this.browser = browser;
        this.schematic = schematic;
        this.useTitleHierarchy = false;
        this.title = text("position");
        this.setParent(browser);
    }

    @Override
    protected void drawScreenBackground(net.minecraft.client.gui.DrawContext ctx, int mouseX, int mouseY)
    {
        fi.dy.masa.malilib.render.RenderUtils.drawRect(0, 0, this.width, this.height, 0xF2111923);
        fi.dy.masa.malilib.render.RenderUtils.drawRect(0, 0, this.width, 24, 0xFF192633);
    }

    @Override
    public void initGui()
    {
        String[] previous = new String[3];
        for (int i = 0; i < 3; ++i) previous[i] = this.fields[i] == null ? null : this.fields[i].getText();
        this.actions.clear();
        this.focused = -1;
        super.initGui();
        int width = Math.min(360, this.width - 24);
        int x = (this.width - width) / 2;
        this.addLabel(x, 26, width, 12, 0xFFFFFFFF, text("ghost_only"));
        this.addLabel(x, 40, width, 12, 0xFFFFFFFF,
                      this.textRenderer.trimToWidth(this.schematic.getMetadata().getName(), width));
        var size = this.schematic.getMetadata().getEnclosingSize();
        this.addLabel(x, 52, width, 12, 0xFFFFFFFF,
                      this.textRenderer.trimToWidth(text("metadata", size.getX(), size.getY(), size.getZ(),
                              this.schematic.getMetadata().getAuthor()), width));
        if (this.coordinates)
        {
            BlockPos pos = this.mc.player == null ? BlockPos.ORIGIN : this.mc.player.getBlockPos();
            int[] values = { pos.getX(), pos.getY(), pos.getZ() };
            for (int i = 0; i < 3; ++i)
            {
                int column = x + i * (width / 3);
                this.addLabel(column, 74, 16, 14, 0xFFFFFFFF, "XYZ".substring(i, i + 1));
                GuiTextFieldGeneric field = new GuiTextFieldGeneric(column + 14, 72, width / 3 - 20, 18, this.textRenderer);
                field.setMaxLength(12);
                field.setText(previous[i] == null ? Integer.toString(values[i]) : previous[i]);
                this.addTextField(field, null);
                this.addWidget(new fi.dy.masa.malilib.gui.widgets.WidgetHoverInfo(field.getX(), field.getY(), field.getWidth(), field.getHeight(), text("coordinate_tip", "XYZ".substring(i, i + 1))));
                this.fields[i] = field;
            }
            button(this, x, 98, width, "confirm_coordinates", this::placeCoordinates);
            button(this, x, 124, width, "back", () -> { this.coordinates = false; this.initGui(); });
        }
        else
        {
            button(this, x, 72, width, "here", () -> {
                if (this.browser.checkWorld(this)) this.place(this.mc.player.getBlockPos());
            });
            button(this, x, 98, width, "coordinates", () -> { this.coordinates = true; this.initGui(); });
            button(this, x, 124, width, "load_only", () -> {
                if (!this.committed && this.browser.checkWorld(this))
                {
                    if (this.register()) openGui(this.browser.getParent());
                }
            });
            button(this, x, 150, width, "back", () -> openGui(this.browser));
        }
    }

    @Override
    public <T extends ButtonBase> T addButton(T button, IButtonActionListener listener)
    {
        if (button instanceof UxButton action) this.actions.add(action);
        return super.addButton(button, listener);
    }

    @Override
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers)
    {
        int count = this.coordinates ? this.fields.length : 0;
        int fieldFocus = -1;
        for (int i = 0; i < count; ++i)
            if (this.fields[i].isFocused()) fieldFocus = i;
        if (keyCode == 258)
        {
            int current = fieldFocus >= 0 ? fieldFocus : this.focused;
            int step = GuiBase.isShiftDown() ? -1 : 1;
            int total = count + this.actions.size();
            this.focused = current < 0 ? (step > 0 ? 0 : total - 1) : Math.floorMod(current + step, total);
            for (int i = 0; i < count; ++i) this.fields[i].setFocused(i == this.focused);
            for (int i = 0; i < this.actions.size(); ++i) this.actions.get(i).setKeyboardFocused(i + count == this.focused);
            this.mc.getNarratorManager().narrate(this.focused < count
                    ? text("coordinate_tip", "XYZ".substring(this.focused, this.focused + 1))
                    : String.join(". ", this.actions.get(this.focused - count).getHoverStrings()));
            return true;
        }
        // Editing keys, including Space and Enter, belong to the focused field.
        if (fieldFocus >= 0) return super.onKeyTyped(keyCode, scanCode, modifiers);
        if ((keyCode == 257 || keyCode == 335 || keyCode == 32) && this.focused >= count)
        {
            this.actions.get(this.focused - count).activate();
            return true;
        }
        return super.onKeyTyped(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        // A second click arriving after a screen transition must not confirm coordinates.
        this.focused = -1;
        for (UxButton action : this.actions) action.setKeyboardFocused(false);
        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void addGuiMessage(MessageType type, int duration, String key, Object... args)
    {
        super.addGuiMessage(type, duration, key, args);
        GuiQuickPlace.keepError(type, duration, key, args);
    }

    private void placeCoordinates()
    {
        if (!this.browser.checkWorld(this)) return;
        try
        {
            int[] p = QuickPlaceCoordinates.parse(this.fields[0].getText(), this.fields[1].getText(),
                    this.fields[2].getText(), this.browser.world.getBottomY(), this.browser.world.getBottomY() + this.browser.world.getHeight());
            this.place(new BlockPos(p[0], p[1], p[2]));
        }
        catch (IllegalArgumentException e)
        {
            this.addMessage(MessageType.ERROR, "litematica.gui.quick_place.invalid_coordinates",
                            this.browser.world.getBottomY(), this.browser.world.getBottomY() + this.browser.world.getHeight() - 1);
        }
    }

    private boolean register()
    {
        // Mark before mutation: even an unexpected partial failure must not allow a second commit.
        this.committed = true;
        try
        {
            // Identity matters: converted files can have the same name or no backing file.
            if (!SchematicHolder.getInstance().getAllSchematics().contains(this.schematic))
            {
                SchematicHolder.getInstance().addSchematic(this.schematic, true);
            }
            return true;
        }
        catch (RuntimeException e)
        {
            this.commitFailed(e);
            return false;
        }
    }

    private void commitFailed(RuntimeException e)
    {
        fi.dy.masa.litematica.Litematica.LOGGER.warn("Failed to commit schematic placement", e);
        this.addMessage(MessageType.ERROR, "litematica.gui.quick_place.commit_failed");
    }

    private void place(BlockPos pos)
    {
        if (this.committed || !this.browser.checkWorld(this)) return;
        if (this.browser.world.isOutOfHeightLimit(pos) || !this.browser.world.getWorldBorder().contains(pos))
        {
            this.addMessage(MessageType.ERROR, "litematica.gui.quick_place.outside_world");
            return;
        }
        if (!this.register()) return;
        try
        {
            SchematicPlacement placement = SchematicPlacement.createFor(this.schematic, pos, this.schematic.getMetadata().getName(), true, true);
            this.browser.manager.addSchematicPlacement(placement, true);
            this.browser.manager.setSelectedSchematicPlacement(placement);
            GuiPlacementConfiguration gui = new GuiPlacementConfiguration(placement);
            gui.setParent(this.browser.getParent());
            openGui(gui);
        }
        catch (RuntimeException e)
        {
            this.commitFailed(e);
        }
    }
}
