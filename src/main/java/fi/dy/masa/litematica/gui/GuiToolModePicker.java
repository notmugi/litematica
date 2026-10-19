package fi.dy.masa.litematica.gui;

import java.util.EnumSet;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;

/** Chooses a mode only; it never executes an operation or changes the tool item. */
public class GuiToolModePicker extends GuiUxBase
{
    public GuiToolModePicker()
    {
        this.title = StringUtils.translate("litematica.gui.ux.picker.title");
    }

    @Override
    public void initGui()
    {
        super.initGui();
        int width = this.width - 24;
        GuiMainMenu.label(this, 12, 30, width, "litematica.gui.ux.picker.explain");
        GuiMainMenu.label(this, 12, 44, width, "litematica.gui.ux.picker.creative");
        EnumSet<ToolMode> allowed = this.availableModes();
        int columns = this.height < 310 || width >= 480 ? 2 : 1;
        int buttonWidth = (width - (columns - 1) * 6) / columns;
        for (ToolMode mode : ToolMode.values())
        {
            int i = mode.ordinal();
            String name = (DataManager.getToolMode() == mode ? "> " : "") + mode.getName();
            String tip = "litematica.gui.ux.picker." + mode.name().toLowerCase(java.util.Locale.ROOT);
            ButtonGeneric button = GuiMainMenu.action(this, 12 + (i % columns) * (buttonWidth + 6),
                    64 + (i / columns) * 24, buttonWidth, 20, name, tip, () -> {
                        // Recheck permissions at click time; no action is executed here.
                        if (this.availableModes().contains(mode))
                        {
                            DataManager.setToolMode(mode);
                            this.closeGui(true);
                        }
                    });
            button.setEnabled(allowed.contains(mode));
            if (!allowed.contains(mode))
            {
                button.setHoverStrings(mode.getName(), StringUtils.translate(tip),
                        StringUtils.translate("litematica.gui.ux.picker.creative"));
            }
        }
        GuiMainMenu.action(this, 12, this.height - 26, width, 20,
                StringUtils.translate("litematica.gui.ux.back"), "litematica.gui.ux.back.tip", () -> this.closeGui(true));
    }

    private EnumSet<ToolMode> availableModes()
    {
        // Reuse cycle's eligibility rules instead of duplicating its private creativeOnly flag.
        EnumSet<ToolMode> allowed = EnumSet.noneOf(ToolMode.class);
        if (this.mc.player != null)
        {
            ToolMode next = ToolMode.AREA_SELECTION;
            do
            {
                allowed.add(next);
                next = next.cycle(this.mc.player, true);
            }
            while (!allowed.contains(next));
        }
        return allowed;
    }
}
