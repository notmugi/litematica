package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.StringUtils;

/** Shown on the first menu visit, never from a world-load callback. */
public class GuiUxHelp extends GuiUxBase
{
    private int page;

    public GuiUxHelp()
    {
        this.title = StringUtils.translate("litematica.gui.ux.help.title");
    }

    @Override
    public void initGui()
    {
        super.initGui();
        int width = this.width - 24;
        GuiMainMenu.label(this, 12, 32, width, "litematica.gui.ux.help.page", this.page + 1, 3);
        GuiMainMenu.label(this, 12, 52, width, "litematica.gui.ux.help.page" + this.page + ".title");
        GuiMainMenu.label(this, 12, 72, width, "litematica.gui.ux.help.page" + this.page + ".line1");
        GuiMainMenu.label(this, 12, 86, width, "litematica.gui.ux.help.page" + this.page + ".line2");
        if (this.page == 0)
        {
            GuiMainMenu.label(this, 12, 106, width, "litematica.gui.ux.help.menu_key", Hotkeys.OPEN_GUI_MAIN_MENU.getKeybind().getStringValue());
        }
        else if (this.page == 2)
        {
            GuiMainMenu.label(this, 12, 106, width, "litematica.gui.ux.help.tool", GuiMainMenu.toolName());
        }
        int half = (width - 6) / 2;
        int bottom = this.height - 76;
        GuiMainMenu.action(this, 12, bottom, half, 20, StringUtils.translate("litematica.gui.ux.help.previous"),
                "litematica.gui.ux.help.previous.tip", () -> { --this.page; this.initGui(); }).setEnabled(this.page > 0);
        GuiMainMenu.action(this, 18 + half, bottom, half, 20, StringUtils.translate("litematica.gui.ux.help.next"),
                "litematica.gui.ux.help.next.tip", () -> { ++this.page; this.initGui(); }).setEnabled(this.page < 2);
        GuiMainMenu.action(this, 12, bottom + 48, width, 20, StringUtils.translate("litematica.gui.ux.help.dismiss"),
                "litematica.gui.ux.help.dismiss.tip", () -> { dismiss(); this.closeGui(true); });
    }

    private static void dismiss()
    {
        Configs.Generic.UX_WELCOME_DISMISSED.setBooleanValue(true);
        Configs.saveToFile();
    }
}
