package fi.dy.masa.litematica.gui;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.widgets.WidgetListLoadedSchematics;
import fi.dy.masa.litematica.gui.widgets.WidgetListSchematicPlacements;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.render.GuiContext;
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
        int width = this.getScreenWidth() - 24;
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
        GuiMainMenu.action(this, 12, this.height - 26, width, 20, StringUtils.translate("litematica.gui.ux.back"),
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
    protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(ctx, mouseX, mouseY, partialTicks);
        if (this.list() != null) this.list().drawContents(ctx, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        return super.onMouseClicked(event, doubleClick) || this.list().onMouseClicked(event, doubleClick);
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent event)
    {
        return super.onMouseReleased(event) || this.list().onMouseReleased(event);
    }

    @Override
    public boolean onMouseDragged(MouseButtonEvent event, double deltaX, double deltaY)
    {
        return super.onMouseDragged(event, deltaX, deltaY) || this.list().onMouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean onMouseScrolled(double x, double y, double horizontal, double vertical)
    {
        return super.onMouseScrolled(x, y, horizontal, vertical) || this.list().onMouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public boolean onKeyTyped(KeyEvent event)
    {
        // Native search/list owns editing keys before task-screen button navigation.
        if (event.key() != 258 && this.list().onKeyTyped(event)) return true;
        return super.onKeyTyped(event);
    }

    @Override
    public boolean onCharTyped(CharacterEvent event)
    {
        return this.list().onCharTyped(event) || super.onCharTyped(event);
    }

    @Override
    public void removed()
    {
        if (this.list() != null) this.list().removed();
        super.removed();
    }
}
