package liteux;

import java.lang.reflect.Field;
import java.util.List;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiConfigs;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;
import fi.dy.masa.malilib.gui.wrappers.TextFieldWrapper;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

/** Exercises the real mixin-transformed widgets and native click/filter/render paths. */
public class DropdownCheck extends GuiConfigs
{
    private boolean checked;

    public DropdownCheck()
    {
        DataManager.setConfigGuiTab(ConfigGuiTab.GENERIC);
    }

    @Override
    public String getModId() { return "liteux_smoke"; }

    @Override
    public void initGui()
    {
        super.initGui();
        // First initialization registers this dev-only subclass with malilib.
        if (this.modSwitchWidget == null) this.buildConfigSwitcher();
    }

    private static Object field(Object object, Class<?> owner, String name) throws Exception
    {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static boolean open(WidgetDropDownList<?> widget) throws Exception
    {
        return (boolean) field(widget, WidgetDropDownList.class, "isOpen");
    }

    private static int bottom(WidgetDropDownList<?> widget) throws Exception
    {
        int limit = (int) field(widget, WidgetDropDownList.class, "maxVisibleEntries");
        int count = ((List<?>) field(widget, WidgetDropDownList.class, "filteredEntries")).size();
        return widget.getY() + widget.getHeight() * (1 + Math.min(limit, count)) + 1;
    }

    private static void click(WidgetDropDownList<?> widget, int x, int y)
    {
        var event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
        widget.onMouseClicked(event, false);
        widget.onMouseReleased(event);
    }

    private static void check(WidgetDropDownList<?> widget, GuiContext ctx) throws Exception
    {
        Object selection = widget.getSelectedEntry();
        int x = widget.getX() + 4;
        int y = widget.getY() + 4;
        for (int edge = 0; edge < 4; ++edge)
        {
            click(widget, x, y);
            assert open(widget) : "Click must open dropdown";
            widget.render(ctx, x, y, false);
            assert open(widget) : "Header hover must keep it open";
            widget.render(ctx, x, bottom(widget) - 2, false);
            assert open(widget) : "Last visible row must stay selectable";
            widget.render(ctx, widget.getX() + widget.getWidth() - 2, bottom(widget) - 2, false);
            assert open(widget) : "Scrollbar hover must keep it open";
            int outsideX = edge == 0 ? widget.getX() - 1 : edge == 1 ? widget.getX() + widget.getWidth() : x;
            int outsideY = edge == 2 ? widget.getY() - 1 : edge == 3 ? bottom(widget) : y;
            widget.render(ctx, outsideX, outsideY, false);
            assert !open(widget) : "Must close on leaving edge " + edge;
            assert widget.getSelectedEntry() == selection : "Hover exit must not select";
        }
        click(widget, x, y);
        widget.onCharTyped(new CharacterEvent('z'));
        widget.onCharTyped(new CharacterEvent('z'));
        widget.onCharTyped(new CharacterEvent('z'));
        var search = (TextFieldWrapper<?>) field(widget, WidgetDropDownList.class, "searchBar");
        assert !search.textField().getValueWrapper().isEmpty() : "Filter input must work";
        widget.render(ctx, x, widget.getY() - 10, false);
        assert open(widget) : "Visible filter above header belongs to popup";
        widget.render(ctx, x, bottom(widget), false);
        assert !open(widget) : "Filtered list uses visible height, not maximum height";
        assert search.textField().getValueWrapper().isEmpty() : "Close must clear hidden filter";
        click(widget, x, y);
        assert ((List<?>) field(widget, WidgetDropDownList.class, "filteredEntries")).size() ==
                ((List<?>) field(widget, WidgetDropDownList.class, "entries")).size() : "Reopen must restore entries";
        widget.render(ctx, widget.getX() - 1, y, false);
    }

    @Override
    public void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(ctx, mouseX, mouseY, partialTicks);
        if (this.checked) return;
        this.checked = true;
        try
        {
            int count = 0;
            for (Object widget : (List<?>) field(this, GuiBase.class, "widgets"))
            {
                if (widget instanceof WidgetDropDownList<?> dropdown)
                {
                    check(dropdown, ctx);
                    ++count;
                }
            }
            assert count >= 1 : "Missing category selector";
            assert this.modSwitchWidget != null : "Missing mod selector";
            check(this.modSwitchWidget, ctx);
            var widgets = (List<?>) field(this, GuiBase.class, "widgets");
            for (Object value : widgets)
            {
                if (value instanceof WidgetDropDownList<?> dropdown && dropdown != this.modSwitchWidget)
                {
                    click(dropdown, dropdown.getX() + 4, dropdown.getY() + 4);
                    click(dropdown, dropdown.getX() + 4, dropdown.getY() + 2 * dropdown.getHeight() + 4);
                    assert DataManager.getConfigGuiTab() == ConfigGuiTab.BUILDING : "Category selection must still work";
                    break; // selecting a category rebuilds the screen's widget list
                }
            }
            System.out.println("LITEUX DROPDOWN PASS: category + mod selector, four edges, rows, scrollbar, filter, reopen; " + this.width + "x" + this.height);
        }
        catch (Exception e) { throw new AssertionError(e); }
    }
}
