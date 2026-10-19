package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.widgets.WidgetListLoadedSchematics;
import fi.dy.masa.litematica.gui.widgets.WidgetListSchematicPlacements;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import net.minecraft.client.gui.DrawContext;
import fi.dy.masa.malilib.util.StringUtils;

/** One workspace for previews and their loaded source objects, using the existing lists. */
public class GuiGhosts extends GuiUxBase implements ISelectionListener<SchematicPlacement>
{
    public final SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
    private final WidgetListBase<?, ?>[] lists = new WidgetListBase<?, ?>[2];
    private boolean loaded;

    public GuiGhosts() { this(false); }

    public GuiGhosts(boolean loaded)
    {
        this.loaded = loaded;
        this.title = StringUtils.translate("litematica.gui.baby.ghosts");
    }

    private WidgetListBase<?, ?> list() { return this.lists[this.loaded ? 1 : 0]; }

    @Override
    public void initGui()
    {
        super.initGui();
        int width = this.width - 24;
        int index = this.loaded ? 1 : 0;
        if (this.lists[index] == null)
        {
            this.lists[index] = this.loaded
                    ? new WidgetListLoadedSchematics(12, 66, width, this.height - 102, null)
                    : new WidgetListSchematicPlacements(12, 66, width, this.height - 102, this);
        }
        this.list().setSize(width, this.height - 102);
        this.list().initGui();
        int half = (width - 6) / 2;
        GuiMainMenu.action(this, 12, 28, half, 20, StringUtils.translate("litematica.gui.baby.ghosts.previews"),
                "litematica.gui.baby.ghosts.previews.tip", () -> this.showTab(false)).setEnabled(this.loaded);
        GuiMainMenu.action(this, 18 + half, 28, half, 20, StringUtils.translate("litematica.gui.baby.ghosts.loaded"),
                "litematica.gui.baby.ghosts.loaded.tip", () -> this.showTab(true)).setEnabled(!this.loaded);
        GuiMainMenu.label(this, 12, 52, width, this.loaded ? "litematica.gui.baby.ghosts.loaded_help" : "litematica.gui.baby.ghosts.help");
        GuiMainMenu.action(this, 18 + half, this.height - 26, half, 20, StringUtils.translate("litematica.gui.ux.back"),
                "litematica.gui.ux.back.tip", () -> this.closeGui(true));
    }

    private void showTab(boolean loaded)
    {
        this.list().removed();
        this.loaded = loaded;
        this.initGui();
    }

    @Override
    public void onSelectionChange(SchematicPlacement entry)
    {
        // Clicking a selected row must not silently deselect it.
        this.manager.setSelectedSchematicPlacement(entry);
    }

    @Override
    protected void drawContents(DrawContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(ctx, mouseX, mouseY, partialTicks);
        if (this.list() != null) this.list().drawContents(ctx, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        return super.onMouseClicked(mouseX, mouseY, mouseButton) || this.list().onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean onMouseReleased(int mouseX, int mouseY, int mouseButton)
    {
        return super.onMouseReleased(mouseX, mouseY, mouseButton) || this.list().onMouseReleased(mouseX, mouseY, mouseButton);
    }


    @Override
    public boolean onMouseScrolled(int x, int y, double horizontal, double vertical)
    {
        return super.onMouseScrolled(x, y, horizontal, vertical) || this.list().onMouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers)
    {
        // Native search/list owns editing keys before task-screen button navigation.
        if (keyCode != 258 && this.list().onKeyTyped(keyCode, scanCode, modifiers)) return true;
        return super.onKeyTyped(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean onCharTyped(char charIn, int modifiers)
    {
        return this.list().onCharTyped(charIn, modifiers) || super.onCharTyped(charIn, modifiers);
    }

    @Override
    public void removed()
    {
        if (this.list() != null) this.list().removed();
        super.removed();
    }
}
