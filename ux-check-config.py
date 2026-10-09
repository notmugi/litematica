#!/usr/bin/env python3
"""Run: JAVA_HOME=/usr/lib/jvm/java-25-openjdk python3 ux-check-config.py
Checks persisted declarations against HEAD, then runs the actual category mapper with
small config stubs (no Minecraft client or test framework needed).
Verifies six exclusive groups, legacy tab aliases, and the global-search source.
"""
import os
from pathlib import Path
import re
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parent
BASE = Path('src/main/java/fi/dy/masa/litematica')

def clean(text):
    return re.sub(r'/\*.*?\*/|//[^\n]*', '', text, flags=re.S)

def declarations(text):
    return {name: re.sub(r'\s+', '', value) for name, value in re.findall(
        r'public static final Config\w+\s+(\w+)\s*=\s*(.*?);', clean(text), re.S)}

for filename in ('config/Configs.java', 'config/Hotkeys.java'):
    path = BASE / filename
    original = subprocess.check_output(['git', 'show', f'HEAD:{path}'], cwd=ROOT, text=True)
    current = (ROOT / path).read_text()
    old, new = declarations(original), declarations(current)
    assert all(new.get(key) == value for key, value in old.items()), f'Changed key/default: {filename}'
    old_lists = re.findall(r'(?:OPTIONS|HOTKEY_LIST)\s*=\s*ImmutableList.of\((.*?)\);', clean(original), re.S)
    new_lists = re.findall(r'(?:OPTIONS|HOTKEY_LIST)\s*=\s*ImmutableList.of\((.*?)\);', clean(current), re.S)
    for old_list, new_list in zip(old_lists, new_lists, strict=True):
        assert set(re.findall(r'\w+', old_list)) <= set(re.findall(r'\w+', new_list)), filename

source = clean((ROOT / BASE / 'config/Configs.java').read_text())
gui_source = (ROOT / BASE / 'gui/GuiConfigs.java').read_text()
tabs_source = gui_source.split('public enum ConfigGuiTab')[1]
tabs = re.findall(r'^\s*(\w+)\s*\(', tabs_source.split('private final')[0], re.M)
old_gui = subprocess.check_output(['git', 'show', f'HEAD:{BASE}/gui/GuiConfigs.java'], cwd=ROOT, text=True)
old_tabs = re.findall(r'^\s*(\w+)\s*\(', old_gui.split('public enum ConfigGuiTab')[1].split('private final')[0], re.M)
assert set(old_tabs) <= set(tabs), 'Removed persisted tab name'
assert 'UxConfigCategories.GROUPS, ConfigGuiTab::getDisplayName' in gui_source
hotkey_entry = re.search(r'public static void openHotkeys\(Screen parent\)\s*\{(.*?)\}', gui_source, re.S).group(1)
assert 'DataManager.setConfigGuiTab(ConfigGuiTab.HOTKEYS)' in hotkey_entry
assert 'new GuiConfigs().setParent(parent)' in hotkey_entry
assert gui_source.count('new GuiRenderLayer()') == 1
assert 'new GuiRenderLayer().setParent(this)' in gui_source
layers_source = (ROOT / BASE / 'gui/GuiRenderLayer.java').read_text()
assert 'this.closeGui(true)' in layers_source and 'new GuiConfigs' not in layers_source
assert 'DataManager.setConfigGuiTab(UxConfigCategories.canonicalTab(DataManager.getConfigGuiTab()))' in gui_source
search_method = re.search(r'protected List<ConfigOptionWrapper> getAllEntries\(\)\s*\{(.*?)\n            \}', gui_source, re.S).group(1)


(ROOT / '.work').mkdir(exist_ok=True)
with tempfile.TemporaryDirectory(prefix='litematica-config-check-', dir=ROOT / '.work') as directory:
    tmp = Path(directory)
    def write(name, text):
        p = tmp / name
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(text)

    write('fi/dy/masa/malilib/config/IConfigBase.java',
          'package fi.dy.masa.malilib.config; public interface IConfigBase { String getName(); }')
    stub = 'package fi.dy.masa.litematica.config; import java.util.*; import fi.dy.masa.malilib.config.IConfigBase; public class Configs { record Opt(String getName) implements IConfigBase {}'
    for group in ('Generic', 'InfoOverlays', 'Visuals', 'Colors'):
        section = source.split(f'public static class {group}')[1].split('public static class ')[0]
        fields = re.findall(r'public static final Config\w+\s+(\w+)\s*=\s*new Config\w+\(\s*"([^"]+)"', section)
        options = re.search(r'OPTIONS\s*=\s*ImmutableList.of\((.*?)\);', section, re.S).group(1)
        stub += f'public static class {group} {{'
        for name, key in fields:
            stub += f'public static final IConfigBase {name} = new Opt("{key}");'
        stub += f'public static final List<IConfigBase> OPTIONS = List.of({options}); }}'
    write('fi/dy/masa/litematica/config/Configs.java', stub + '}')
    hotkeys = clean((ROOT / BASE / 'config/Hotkeys.java').read_text())
    fields = dict(re.findall(r'public static final ConfigHotkey\s+(\w+)\s*=\s*new ConfigHotkey\("([^"]+)"', hotkeys))
    hotkey_list = re.search(r'HOTKEY_LIST\s*=\s*ImmutableList.of\((.*?)\);', hotkeys, re.S).group(1)
    keys = [fields[name] for name in re.findall(r'\w+', hotkey_list)]
    write('fi/dy/masa/litematica/config/Hotkeys.java',
          'package fi.dy.masa.litematica.config; import java.util.*; import fi.dy.masa.malilib.config.IConfigBase; public class Hotkeys { public static final List<IConfigBase> HOTKEY_LIST = List.of('
          + ','.join(f'new Configs.Opt("{key}")' for key in keys) + '); }')
    write('fi/dy/masa/litematica/gui/GuiConfigs.java',
          'package fi.dy.masa.litematica.gui; public class GuiConfigs { public enum ConfigGuiTab {' + ','.join(tabs) + '} }')
    write('fi/dy/masa/litematica/gui/UxConfigCategories.java', (ROOT / BASE / 'gui/UxConfigCategories.java').read_text())
    # Compile the real search-source method, not a parallel Python model of it.
    write('SearchSource.java', """
import java.util.*;
import fi.dy.masa.litematica.gui.UxConfigCategories;
import fi.dy.masa.litematica.gui.GuiConfigs.ConfigGuiTab;
import fi.dy.masa.malilib.config.IConfigBase;
class GuiConfigs {
    ConfigGuiTab tab = ConfigGuiTab.GENERIC;
    List<ConfigOptionWrapper> getConfigs() { return ConfigOptionWrapper.createFor(UxConfigCategories.getConfigs(tab)); }
    record ConfigOptionWrapper(IConfigBase config) {
        static List<ConfigOptionWrapper> createFor(List<? extends IConfigBase> options) {
            return options.stream().map(ConfigOptionWrapper::new).toList();
        }
    }
    class SearchSource {
        boolean active;
        boolean hasFilter() { return active; }
        List<ConfigOptionWrapper> getAllEntries() {""" + search_method + """}
    }
}
""")
    write('Check.java', '''
import java.util.*;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.gui.UxConfigCategories;
import fi.dy.masa.litematica.gui.GuiConfigs.ConfigGuiTab;
public class Check {
    public static void main(String[] args) {
        var groups = UxConfigCategories.GROUPS;
        assert groups.equals(List.of(ConfigGuiTab.GENERIC, ConfigGuiTab.BUILDING, ConfigGuiTab.PASTE_COMMANDS,
                ConfigGuiTab.FILES, ConfigGuiTab.ADVANCED, ConfigGuiTab.HOTKEYS)) : "Wrong visible groups";
        var all = UxConfigCategories.allConfigs();
        var unique = Collections.newSetFromMap(new IdentityHashMap<>());
        unique.addAll(all);
        assert unique.size() == all.size() : "Duplicate authoritative option";
        var reachable = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var tab : groups) {
            for (var option : UxConfigCategories.getConfigs(tab)) {
                assert reachable.add(option) : option.getName() + " duplicated in " + tab;
            }
        }
        assert reachable.equals(unique) : "Unreachable or unregistered options";
        for (var options : List.of(Configs.Visuals.OPTIONS, Configs.InfoOverlays.OPTIONS, Configs.Colors.OPTIONS)) {
            assert UxConfigCategories.getConfigs(ConfigGuiTab.BUILDING).containsAll(options);
        }
        for (var tab : ConfigGuiTab.values()) {
            // Exercise the exact valueOf entry point used by persisted config_gui_tab.
            var expected = switch (tab) {
                case ALL -> ConfigGuiTab.GENERIC;
                case VISUALS, INFO_OVERLAYS, COLORS, RENDER_LAYERS -> ConfigGuiTab.BUILDING;
                default -> tab;
            };
            var canonical = UxConfigCategories.canonicalTab(ConfigGuiTab.valueOf(tab.name()));
            assert canonical == expected : "Wrong legacy destination: " + tab;
            assert UxConfigCategories.canonicalTab(canonical) == canonical;
            assert UxConfigCategories.getConfigs(tab).equals(UxConfigCategories.getConfigs(canonical));
        }
        var gui = new GuiConfigs();
        var search = gui.new SearchSource();
        for (var tab : groups) {
            gui.tab = tab;
            search.active = false;
            assert search.getAllEntries().equals(gui.getConfigs()) : "Unfiltered category changed";
            search.active = true;
            var results = search.getAllEntries();
            assert results.stream().map(GuiConfigs.ConfigOptionWrapper::config).toList().equals(all)
                    : "Search source is not global: " + tab;
            search.active = false;
            assert search.getAllEntries().equals(gui.getConfigs()) : "Closing search lost category";
        }
        assert UxConfigCategories.allConfigs().equals(all) : "Navigation mutated authoritative options";
        System.out.println("PASS: " + all.size() + " options in exactly one of six groups; legacy tabs and global search preserved.");
    }
}
''')
    java = Path(os.environ.get('JAVA_HOME', '/usr/lib/jvm/java-25-openjdk')) / 'bin'
    subprocess.run([str(java / 'javac'), '-d', str(tmp / 'out'), *map(str, tmp.rglob('*.java'))], check=True)
    subprocess.run([str(java / 'java'), '-ea', '-cp', str(tmp / 'out'), 'Check'], check=True)
print('PASS: all original config/hotkey declarations, defaults and persistence-list entries preserved.')
