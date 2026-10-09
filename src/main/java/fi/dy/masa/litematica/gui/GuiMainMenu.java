package fi.dy.masa.litematica.gui;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.Screen;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetHoverInfo;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiMainMenu extends GuiUxBase
{
    private boolean welcomeShown;
    private boolean advanced;

    public GuiMainMenu()
    {
        this.title = StringUtils.translate("litematica.gui.title.litematica_main_menu", "v" + Reference.MOD_VERSION);
    }

    @Override
    public void initGui()
    {
        super.initGui();
        if (!this.welcomeShown && !Configs.Generic.UX_WELCOME_DISMISSED.getBooleanValue())
        {
            this.welcomeShown = true;
            openChild(this, new GuiUxHelp());
            return;
        }

        // ponytail: targets Minecraft's usual minimum 320x240 GUI; smaller forced scales need scrolling.
        int x = 12;
        int width = this.getScreenWidth() - 24;
        boolean compact = this.getScreenHeight() < 280;
        int y = 30;
        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
        label(this, x, y, width, "litematica.gui.ux.home.counts",
                SchematicHolder.getInstance().getAllSchematics().size(),
                DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().size());
        y += 13;
        label(this, x, y, width, "litematica.gui.ux.home.active_layer",
                placement != null ? placement.getName() : StringUtils.translate("litematica.gui.ux.none"),
                DataManager.getRenderLayerRange().getLayerMode().getDisplayName(),
                DataManager.getRenderLayerRange().getCurrentLayerString());
        y += 13;
        String tool = toolName();
        String held = StringUtils.translate(this.mc.player != null && EntityUtils.hasToolItem(this.mc.player)
                ? "litematica.gui.ux.held" : "litematica.gui.ux.not_held");
        if (!compact)
        {
            label(this, x, y, width, "litematica.gui.ux.home.tool_status", tool, held,
                    onOff(Configs.Generic.EXECUTE_REQUIRE_TOOL.getBooleanValue()));
            y += 16;
        }
        int half = (width - 6) / 2;
        action(this, x, y, half, 20,
                StringUtils.translate("litematica.gui.ux.home.mode", DataManager.getToolMode().getName()),
                "litematica.gui.ux.home.mode.tip", () -> openChild(this, new GuiToolModePicker()));
        action(this, x + half + 6, y, half, 20,
                StringUtils.translate("litematica.gui.ux.home.tool", onOff(Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue())),
                "litematica.gui.ux.home.tool.tip", () -> {
                    Configs.Generic.TOOL_ITEM_ENABLED.setBooleanValue(!Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue());
                    Configs.saveToFile();
                    this.initGui();
                });
        y += 28;

        int columns = width >= 356 ? 2 : 1;
        if (this.advanced)
        {
            this.menuButton(x, y, width, ButtonListenerChangeMenu.ButtonType.TASK_MANAGER);
            if (Configs.Generic.UNHIDE_SCHEMATIC_PROJECTS.getBooleanValue())
                this.menuButton(x, y + 26, width, ButtonListenerChangeMenu.ButtonType.SCHEMATIC_PROJECTS_MANAGER);

        }
        else
        {
            int rows = 4 / columns;
            int cardHeight = Math.max(20, Math.min(42, (this.getScreenHeight() - 38 - y) / rows - 5));
            int cardWidth = (width - (columns - 1) * 6) / columns;
            String[] keys = { "place", "save", "build", "files" };
            Runnable[] actions = {
                () -> openChild(this, new GuiQuickPlace()),
                () -> GuiSaveSelection.open(this),
                () -> openChild(this, new GuiGhosts()),
                () -> openChild(this, new GuiSchematicManager())
            };
            for (int i = 0; i < keys.length; ++i)
            {
                action(this, x + (i % columns) * (cardWidth + 6), y + (i / columns) * (cardHeight + 5),
                        cardWidth, cardHeight, StringUtils.translate("litematica.gui.ux.home." + keys[i]),
                        "litematica.gui.ux.home." + keys[i] + ".tip", actions[i])
                        .subtitle(StringUtils.translate("litematica.gui.ux.home." + keys[i] + ".short"));
            }
        }

        int footerWidth = (width - 12) / 3;
        int bottom = this.getScreenHeight() - 26;
        action(this, x, bottom, footerWidth, 20, StringUtils.translate("litematica.gui.ux.settings"),
                "litematica.gui.ux.settings.tip", () -> {
                    DataManager.setConfigGuiTab(GuiConfigs.ConfigGuiTab.GENERIC);
                    openChild(this, new GuiConfigs());
                });
        action(this, x + (footerWidth + 6), bottom, footerWidth, 20, StringUtils.translate("litematica.gui.ux.help"),
                "litematica.gui.ux.help.tip", () -> openChild(this, new GuiUxHelp()));
        action(this, x + 2 * (footerWidth + 6), bottom, footerWidth, 20,
                StringUtils.translate(this.advanced ? "litematica.gui.ux.home.tasks" : "litematica.gui.ux.home.advanced"),
                "litematica.gui.ux.home.advanced.tip", () -> { this.advanced = !this.advanced; this.initGui(); });
    }

    private void menuButton(int x, int y, int width, ButtonListenerChangeMenu.ButtonType type)
    {
        ButtonGeneric button = action(this, x, y, width, 20, type.getDisplayName(),
                "litematica.gui.ux.advanced." + type.name().toLowerCase(Locale.ROOT),
                () -> new ButtonListenerChangeMenu(type, this).actionPerformedWithButton(null, 0));
        if (type == ButtonListenerChangeMenu.ButtonType.AREA_SELECTION_BROWSER && DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            button.setEnabled(false);
            button.setHoverStrings("litematica.gui.button.hover.schematic_projects.area_browser_disabled_currently_in_projects_mode");
        }
        else if (type == ButtonListenerChangeMenu.ButtonType.SCHEMATIC_PROJECTS_MANAGER)
        {
            button.setHoverStrings("litematica.gui.button.hover.schematic_projects.menu_warning");
        }
    }

    static void openChild(GuiBase parent, @Nullable GuiBase child)
    {
        if (child != null)
        {
            child.setParent(parent);
            GuiBase.openGui(child);
        }
    }

    static String toolName()
    {
        // Item components are bound only after entering a world in Minecraft 26.2.
        if (net.minecraft.client.Minecraft.getInstance().level == null)
            return Configs.Generic.TOOL_ITEM.getStringValue();
        return (DataManager.getInstance().hasToolItemComponents()
                ? DataManager.getInstance().getToolItemComponents() : DataManager.getToolItem()).getHoverName().getString();
    }

    static String onOff(boolean value)
    {
        return StringUtils.translate(value ? "litematica.gui.ux.on" : "litematica.gui.ux.off");
    }

    static UxButton action(GuiBase gui, int x, int y, int width, int height, String text, String hover, Runnable action)
    {
        UxButton button = new UxButton(x, y, width, height,
                text);
        button.setHoverStrings(text, StringUtils.translate(hover));
        gui.addButton(button, (b, mouseButton) -> action.run());
        return button;
    }

    static void label(GuiBase gui, int x, int y, int width, String key, Object... args)
    {
        String text = StringUtils.translate(key, args);
        gui.addLabel(x, y, width, 12, 0xFFCCCCCC,
                gui.font.plainSubstrByWidth(text, Math.max(1, width)));
        if (gui.getStringWidth(text) > width)
        {
            gui.addWidget(new WidgetHoverInfo(x, y, width, 12, text));
        }
    }

    public static class ButtonListenerChangeMenu implements IButtonActionListener
    {
        private final ButtonType type;
        @Nullable
        private final Screen parent;

        public ButtonListenerChangeMenu(ButtonType type, @Nullable Screen parent)
        {
            this.type = type;
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            GuiBase gui = null;

            switch (this.type)
            {
                case AREA_EDITOR:
                case AREA_SELECTION_BROWSER:
                    GuiSaveSelection.open(this.parent);
                    return;
                case CONFIGURATION:
                    gui = new GuiConfigs();
                    break;
                case LOAD_SCHEMATICS:
                    gui = new GuiQuickPlace();
                    break;
                case LOADED_SCHEMATICS:
                    gui = new GuiGhosts(true);
                    break;
                case MAIN_MENU:
                    gui = new GuiMainMenu();
                    break;
                case SCHEMATIC_MANAGER:
                    gui = new GuiSchematicManager();
                    break;
                case SCHEMATIC_PLACEMENTS:
                    gui = new GuiGhosts();
                    break;
                case TASK_MANAGER:
                    gui = new GuiTaskManager();
                    break;
                case SCHEMATIC_PROJECTS_MANAGER:
                    DataManager.getSchematicProjectsManager().openSchematicProjectsGui();
                    return;
            }

            if (gui != null)
            {
                gui.setParent(this.parent);
                GuiBase.openGui(gui);
            }
        }

        public enum ButtonType
        {
            // List loaded Schematics in SchematicHolder
            LOADED_SCHEMATICS           ("litematica.gui.button.change_menu.show_loaded_schematics", ButtonIcons.LOADED_SCHEMATICS),
            // List Schematics placements
            SCHEMATIC_PLACEMENTS        ("litematica.gui.button.change_menu.show_schematic_placements", ButtonIcons.SCHEMATIC_PLACEMENTS),
            // Open the Area Selection browser
            AREA_SELECTION_BROWSER      ("litematica.gui.button.change_menu.show_area_selections", ButtonIcons.AREA_SELECTION),
            // Open the Area Editor GUI
            AREA_EDITOR                 ("litematica.gui.button.change_menu.area_editor", ButtonIcons.AREA_EDITOR),
            // Load Schematics from file to memory
            LOAD_SCHEMATICS             ("litematica.gui.button.change_menu.load_schematics_to_memory", ButtonIcons.SCHEMATIC_BROWSER),
            // Edit Schematics (description or icon), or convert between formats
            SCHEMATIC_MANAGER           ("litematica.gui.button.change_menu.schematic_manager", ButtonIcons.SCHEMATIC_MANAGER),
            // Open the Task Manager
            TASK_MANAGER                ("litematica.gui.button.change_menu.task_manager", ButtonIcons.TASK_MANAGER),
            // Open the Schematic Projects browser
            SCHEMATIC_PROJECTS_MANAGER  ("litematica.gui.button.change_menu.schematic_projects_manager", ButtonIcons.SCHEMATIC_PROJECTS),
            // In-game Configuration GUI
            CONFIGURATION               ("litematica.gui.button.change_menu.configuration_menu", ButtonIcons.CONFIGURATION),
            // Switch to the Litematica main menu
            MAIN_MENU                   ("litematica.gui.button.change_menu.to_main_menu", null);

            private final String labelKey;
            private final ButtonIcons icon;

            ButtonType(String labelKey, ButtonIcons icon)
            {
                this.labelKey = labelKey;
                this.icon = icon;
            }

            public String getLabelKey()
            {
                return this.labelKey;
            }

            public String getDisplayName()
            {
                return StringUtils.translate(this.getLabelKey());
            }

            public ButtonIcons getIcon()
            {
                return this.icon;
            }
        }
    }

}
