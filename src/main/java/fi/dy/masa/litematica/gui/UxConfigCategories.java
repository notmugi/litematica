package fi.dy.masa.litematica.gui;

import java.util.ArrayList;
import java.util.List;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.gui.GuiConfigs.ConfigGuiTab;
import fi.dy.masa.malilib.config.IConfigBase;

/** Display-only groups: the original option objects and persistence lists stay authoritative. */
public final class UxConfigCategories
{
    public static final List<ConfigGuiTab> GROUPS = List.of(
            ConfigGuiTab.GENERIC, ConfigGuiTab.BUILDING, ConfigGuiTab.PASTE_COMMANDS,
            ConfigGuiTab.FILES, ConfigGuiTab.ADVANCED, ConfigGuiTab.HOTKEYS);

    private UxConfigCategories() {}

    /** Old persisted tab names remain readable, but never become extra screens. */
    public static ConfigGuiTab canonicalTab(ConfigGuiTab tab)
    {
        return switch (tab)
        {
            case ALL -> ConfigGuiTab.GENERIC;
            case VISUALS, INFO_OVERLAYS, COLORS, RENDER_LAYERS -> ConfigGuiTab.BUILDING;
            default -> tab;
        };
    }

    public static ConfigGuiTab genericGroup(IConfigBase config)
    {
        String name = config.getName();

        if (List.of("executeRequireHoldingTool", "toolItemEnabled", "toolItem", "easyPlaceMode",
                    "translationLanguage", "translationMode", "uxStatusHud", "uxWelcomeDismissed",
                    "uxConfirmWorldEdits", "uxConfirmDiscard").contains(name))
        {
            return ConfigGuiTab.GENERIC;
        }
        if (name.startsWith("paste") || name.startsWith("command"))
        {
            return ConfigGuiTab.PASTE_COMMANDS;
        }
        if (name.startsWith("easyPlace") || name.startsWith("pickBlock") || name.startsWith("materialList") ||
            name.startsWith("placementRestriction") || name.equals("highlightBlockInInventory") ||
            name.equals("layerModeFollowsPlayer") || name.equals("signTextPaste") || name.equals("renderMaterialListInGuis"))
        {
            return ConfigGuiTab.BUILDING;
        }
        if (name.startsWith("customSchematic") || name.startsWith("datafixer") ||
            name.equals("areaSelectionsPerWorld") || name.equals("generateLowercaseNames") ||
            name.equals("displayFileOpsFeedback") || name.equals("loadEntireSchematics"))
        {
            return ConfigGuiTab.FILES;
        }

        // Unknown/new options remain reachable without maintaining a second exhaustive option list.
        return ConfigGuiTab.ADVANCED;
    }

    public static List<? extends IConfigBase> getConfigs(ConfigGuiTab tab)
    {
        tab = canonicalTab(tab);
        if (tab == ConfigGuiTab.HOTKEYS) return Hotkeys.HOTKEY_LIST;

        List<IConfigBase> options = new ArrayList<>();
        for (IConfigBase config : Configs.Generic.OPTIONS)
        {
            if (genericGroup(config) == tab) options.add(config);
        }
        if (tab == ConfigGuiTab.BUILDING)
        {
            options.addAll(Configs.Visuals.OPTIONS);
            options.addAll(Configs.InfoOverlays.OPTIONS);
            options.addAll(Configs.Colors.OPTIONS);
        }
        if (tab == ConfigGuiTab.GENERIC)
        {
            options.sort(java.util.Comparator.comparingInt(config -> {
                int index = List.of("toolItemEnabled", "toolItem", "easyPlaceMode", "uxStatusHud",
                        "executeRequireHoldingTool", "uxConfirmWorldEdits", "uxConfirmDiscard",
                        "translationMode", "translationLanguage", "uxWelcomeDismissed").indexOf(config.getName());
                return index < 0 ? 100 : index;
            }));
        }
        return options;
    }

    public static List<IConfigBase> allConfigs()
    {
        List<IConfigBase> configs = new ArrayList<>();
        configs.addAll(Configs.Generic.OPTIONS);
        configs.addAll(Configs.InfoOverlays.OPTIONS);
        configs.addAll(Configs.Visuals.OPTIONS);
        configs.addAll(Configs.Colors.OPTIONS);
        configs.addAll(Hotkeys.HOTKEY_LIST);
        return configs;
    }
}
