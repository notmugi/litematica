package fi.dy.masa.litematica.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.MutableComponent;

import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IConfigResettable;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiConfirmAction;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ConfigButtonBoolean;
import fi.dy.masa.malilib.gui.interfaces.IConfigGuiAllTab;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;
import fi.dy.masa.malilib.gui.widgets.WidgetHoverInfo;
import fi.dy.masa.malilib.gui.widgets.WidgetLabel;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import fi.dy.masa.malilib.interfaces.IConfirmationListener;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiConfigs extends GuiConfigsBase
{
    public GuiConfigs()
    {
        super(10, 50, Reference.MOD_ID, null, "litematica.gui.title.configs", Reference.MOD_VERSION);
    }

    /** Direct entry point for the home screen's Keys button. */
    public static void openHotkeys(Screen parent)
    {
        DataManager.setConfigGuiTab(ConfigGuiTab.HOTKEYS);
        GuiBase.openGui(new GuiConfigs().setParent(parent));
    }

    @Override
    public void initGui()
    {
        DataManager.setConfigGuiTab(UxConfigCategories.canonicalTab(DataManager.getConfigGuiTab()));
        this.clearOptions();
        super.initGui();

        // One fixed-height navigation row at every GUI scale; all groups remain in the scrollable menu.
        int available = Math.max(40, this.width - 20);
        int resetWidth = Math.min(110, available / 3);
        WidgetDropDownList<ConfigGuiTab> groups = new WidgetDropDownList<>(
                10, 26, available - resetWidth - 4, 20, Math.max(20, this.height - 76), 10,
                UxConfigCategories.GROUPS, ConfigGuiTab::getDisplayName)
        {
            @Override
            protected String getDisplayString(ConfigGuiTab entry)
            {
                String label = entry == null ? "" : entry.getDisplayName();
                return GuiConfigs.this.font.plainSubstrByWidth(label, Math.max(0, this.width - 24));
            }

            @Override
            protected void setSelectedEntry(int index)
            {
                super.setSelectedEntry(index);
                if (this.selectedEntry != null) GuiConfigs.this.selectTab(this.selectedEntry);
            }
        };
        // The object overload initializes selection without invoking the click/index callback.
        groups.setSelectedEntry(DataManager.getConfigGuiTab());
        this.addWidget(groups);
        this.addButton(new UxButton(10 + available - resetWidth, 26, resetWidth, 20,
                StringUtils.translate("litematica.gui.button.ux.reset_group"), "litematica.gui.hover.ux.reset_group"),
                (button, mouseButton) -> this.confirmReset());

        String searchHint = StringUtils.translate("litematica.gui.label.ux.search_all_settings");
        this.addWidget(new WidgetLabel(10, this.height - 20, available + 10, 14, 0xFFB6C7D6,
                this.font.plainSubstrByWidth(searchHint, Math.max(1, available + 10))));
    }

    @Override
    protected void drawTitle(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        int right = this.modSwitchWidget == null ? this.width - 10 : this.modSwitchWidget.getX() - 6;
        this.drawString(ctx, this.font.plainSubstrByWidth(this.getTitleString(),
                Math.max(1, right - 10)), 10, 10, 0xFFFFFFFF);
    }

    private void selectTab(ConfigGuiTab tab)
    {
        if (tab == DataManager.getConfigGuiTab()) return;
        // Commit text fields and dirty keybinds before replacing their widget, not only when closing.
        this.removed();
        DataManager.setConfigGuiTab(tab);
        this.reCreateListWidget();
        Objects.requireNonNull(this.getListWidget()).resetScrollbarPosition();
        this.initGui();
    }

    private void confirmReset()
    {
        ConfigGuiTab tab = DataManager.getConfigGuiTab();
        GuiBase.openGui(new GuiConfirmAction(Math.min(400, this.width - 20),
                "litematica.gui.title.ux.reset_group", new IConfirmationListener()
        {
            @Override
            public boolean onActionConfirmed()
            {
                // Includes hidden search results: reset the group, not just the visible rows.
                for (IConfigBase config : UxConfigCategories.getConfigs(tab))
                {
                    if (config instanceof IConfigResettable resettable) resettable.resetToDefault();
                }
                InputEventHandler.getKeybindManager().updateUsedKeys();
                GuiConfigs.this.onSettingsChanged();
                GuiConfigs.this.reCreateListWidget();
                return true;
            }

            @Override
            public boolean onActionCancelled()
            {
                return true;
            }
        }, this, "litematica.gui.message.ux.reset_group", tab.getDisplayName()));
    }

    @Override
    public void removed()
    {
        Objects.requireNonNull(this.getListWidget()).applyPendingModifications();
        super.removed();
        Configs.checkBaseLanguage();
    }

    @Override
    protected int getConfigWidth()
    {
        return Math.min(180, Math.max(140, this.width / 3));
    }

    @Override
    protected boolean useKeybindSearch()
    {
        return true;
    }

    @Override
    protected WidgetListConfigOptions createListWidget(int listX, int listY)
    {
        return new WidgetListConfigOptions(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(),
                this.getConfigWidth(), 0.f, this.useKeybindSearch(), this)
        {
            @Override
            protected List<ConfigOptionWrapper> getAllEntries()
            {
                // Native search refresh re-reads this source on typing, key search and closing search.
                // No All screen: only a nonempty text/key filter broadens the source.
                return this.hasFilter() ? ConfigOptionWrapper.createFor(UxConfigCategories.allConfigs())
                        : GuiConfigs.this.getConfigs();
            }

            private int resetWidth()
            {
                return Math.min(64, this.getStringWidth(StringUtils.translate("malilib.gui.button.reset.caps")) + 18);
            }

            @Override
            public int getMaxNameLengthWrapped(List<ConfigOptionWrapper> entries)
            {
                // Native rows put reset AFTER configWidth; reserve it before measuring names.
                // Recompute on resize as well as category changes, without replacing pending editors.
                this.configWidth = GuiConfigs.this.getConfigWidth();
                return Math.max(1, Math.min(super.getMaxNameLengthWrapped(entries),
                        this.browserEntryWidth - this.configWidth - this.resetWidth() - 12));
            }

            @Override
            protected WidgetConfigOption createListEntryWidget(int x, int y, int index,
                    boolean odd, ConfigOptionWrapper entry)
            {
                int resetWidth = this.resetWidth();
                assert this.maxLabelWidth + 12 + this.configWidth + resetWidth <= this.browserEntryWidth;
                return new WidgetConfigOption(x, y, this.browserEntryWidth, this.browserEntryHeight,
                        this.maxLabelWidth, this.configWidth, entry, index, this.parent, this)
                {
                    {
                        // Runnable layout check: enable -ea while opening/resizing every config group.
                        for (WidgetBase widget : this.subWidgets)
                        {
                            assert widget.getX() >= this.x && widget.getX() + widget.getWidth() <= this.x + this.width
                                    : "Config control outside row: " + entry.getConfig();
                        }
                    }

                    @Override
                    protected void addLabel(int x, int y, int width, int height, int color, String... lines)
                    {
                        // Headers use the whole row; option names stay out of the native editors.
                        int available = entry.getConfig() == null ? this.width : width;
                        for (String line : lines)
                        {
                            String full = StringUtils.translate(line);
                            String label = this.getStringWidth(full) <= available ? full
                                    : GuiConfigs.this.font.plainSubstrByWidth(full, Math.max(1, available - this.getStringWidth("..."))) + "...";
                            this.addWidget(new WidgetLabel(x, y, available, height, color, label));
                            this.addWidget(new WidgetHoverInfo(x, y, available, height, full));
                        }
                    }

                    @Override
                    protected void addConfigComment(int x, int y, int width, int height, String comment)
                    {
                        super.addConfigComment(x, y, width, height,
                                entry.getConfig().getConfigGuiDisplayName() + "\n" + comment);
                    }

                    @Override
                    protected void addConfigComment(int x, int y, int width, int height, MutableComponent comment)
                    {
                        // Copy: the shared config's component must not gain a name on every refresh.
                        super.addConfigComment(x, y, width, height,
                                net.minecraft.network.chat.Component.literal(entry.getConfig().getConfigGuiDisplayName() + "\n")
                                        .append(comment.copy()));
                    }

                    @Override
                    protected void drawSubWidgets(GuiContext ctx, int mouseX, int mouseY)
                    {
                        this.hoveredSubWidget = null;
                        for (WidgetBase widget : this.subWidgets)
                        {
                            if (widget instanceof ConfigButtonBoolean button && entry.getConfig() instanceof IConfigBoolean config)
                            {
                                button.setDisplayString(StringUtils.translate(config.getBooleanValue() ? "options.on" : "options.off"));
                            }
                            // Native option/keybind captions can exceed their button width too.
                            // Clip only their rendering; editors, hit boxes and tooltips stay native.
                            boolean button = widget instanceof ButtonBase;
                            if (button) ctx.pushScissor(new ScreenRectangle(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight()));
                            widget.render(ctx, mouseX, mouseY, false);
                            if (button) ctx.popScissor();
                            if (widget.isMouseOver(mouseX, mouseY)) this.hoveredSubWidget = widget;
                        }
                    }

                    @Override
                    protected ButtonGeneric createResetButton(int x, int y, IConfigResettable config)
                    {
                        String label = StringUtils.translate("malilib.gui.button.reset.caps");
                        ButtonGeneric button = new UxButton(x, y, resetWidth, 20, label, label);
                        button.setEnabled(config.isModified());
                        return button;
                    }
                };
            }

            @Override
            protected List<String> getEntryStringsForFilter(ConfigOptionWrapper entry)
            {
                // malilib already searches internal/display names and "modified", but not comments.
                List<String> strings = new ArrayList<>(super.getEntryStringsForFilter(entry));
                IConfigBase config = entry.getConfig();
                if (config != null)
                {
                    strings.add(config.getConfigGuiDisplayName().toLowerCase(Locale.ROOT));
                    if (config.getComment() != null) strings.add(config.getComment().toLowerCase(Locale.ROOT));
                    if (config instanceof ConfigHotkey hotkey)
                    {
                        strings.add(StringUtils.translate(hotkeyGroup(hotkey)).toLowerCase(Locale.ROOT));
                    }
                }
                return strings;
            }

            @Override
            protected boolean getShouldSortList()
            {
                // Preserve group headers in the hotkey view; other views retain malilib's sorting.
                return (this.hasFilter() || DataManager.getConfigGuiTab() != ConfigGuiTab.HOTKEYS) && super.getShouldSortList();
            }
        };
    }

    private static String hotkeyGroup(ConfigHotkey hotkey)
    {
        String group;
        if (hotkey.getDefaultHotkeyStringValue().isEmpty()) group = "expert";
        else if (hotkey.getName().startsWith("openGui")) group = "menus";
        else if (hotkey.getName().startsWith("selection") || hotkey.getName().contains("Selection") ||
                 hotkey.getName().startsWith("tool") || hotkey.getName().startsWith("operation")) group = "selection";
        else group = "building";
        return "litematica.gui.label.ux.hotkeys_" + group;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs()
    {
        ConfigGuiTab tab = DataManager.getConfigGuiTab();
        if (tab == ConfigGuiTab.HOTKEYS)
        {
            List<ConfigOptionWrapper> configs = new ArrayList<>();
            for (String group : List.of("menus", "building", "selection", "expert"))
            {
                String key = "litematica.gui.label.ux.hotkeys_" + group;
                configs.add(new ConfigOptionWrapper(StringUtils.translate(key)));
                for (ConfigHotkey hotkey : Hotkeys.HOTKEY_LIST)
                {
                    if (hotkeyGroup(hotkey).equals(key)) configs.add(new ConfigOptionWrapper(hotkey));
                }
            }
            return configs;
        }
        List<ConfigOptionWrapper> configs = new ArrayList<>();
        if (tab == ConfigGuiTab.PASTE_COMMANDS)
        {
            configs.add(new ConfigOptionWrapper(StringUtils.translate("litematica.gui.label.ux.world_edit_warning")));
        }
        configs.addAll(ConfigOptionWrapper.createFor(UxConfigCategories.getConfigs(tab)));
        return configs;
    }

    @Override
    protected void onSettingsChanged()
    {
        super.onSettingsChanged();
        SchematicWorldRefresher.INSTANCE.updateAll();
    }

    // Keep legacy names (and order) for persisted config_gui_tab compatibility.
    public enum ConfigGuiTab
    {
        ALL             (IConfigGuiAllTab.getTranslationKey()),
        GENERIC         ("litematica.gui.button.ux.config_getting_started"),
        INFO_OVERLAYS   ("litematica.gui.button.config_gui.info_overlays"),
        VISUALS         ("litematica.gui.button.config_gui.visuals"),
        COLORS          ("litematica.gui.button.config_gui.colors"),
        HOTKEYS         ("litematica.gui.button.config_gui.hotkeys"),
        RENDER_LAYERS   ("litematica.gui.button.config_gui.render_layers"),
        BUILDING        ("litematica.gui.button.ux.config_building"),
        PASTE_COMMANDS  ("litematica.gui.button.ux.config_paste_commands"),
        FILES           ("litematica.gui.button.ux.config_files"),
        ADVANCED        ("litematica.gui.button.ux.config_advanced");

        private final String translationKey;

        ConfigGuiTab(String translationKey)
        {
            this.translationKey = translationKey;
        }

        public String getDisplayName()
        {
            return StringUtils.translate(this.translationKey);
        }
    }
}
