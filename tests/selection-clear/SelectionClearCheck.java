import java.nio.file.*;
import java.lang.reflect.*;
import fi.dy.masa.litematica.selection.*;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.schematic.projects.*;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.malilib.util.position.PositionUtils.CoordinateType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class SelectionClearCheck {
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    static void set(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
    static void grab(SelectionManager manager, AreaSelection area, Box box) throws Exception {
        Class<?> type = Class.forName(SelectionManager.class.getName() + "$GrabbedElement");
        var ctor = type.getDeclaredConstructor(AreaSelection.class, Box.class, Corner.class, Vec3.class, double.class);
        ctor.setAccessible(true);
        set(manager, "grabbedElement", ctor.newInstance(area, box, Corner.CORNER_1, Vec3.ZERO, 3d));
        check(manager.hasGrabbedElement(), "grab setup");
    }
    static void fill(AreaSelection area) {
        Box box = area.getSelectedSubRegionBox();
        if (box == null) {
            area.createNewSubRegionBox(new BlockPos(1, 2, 3), "test");
            box = area.getSelectedSubRegionBox();
        }
        area.setSubRegionCornerPos(box, Corner.CORNER_1, new BlockPos(1, 2, 3));
        area.setSubRegionCornerPos(box, Corner.CORNER_2, new BlockPos(4, 5, 6));
        box.setSelectedCorner(Corner.CORNER_2);
        area.setExplicitOrigin(new BlockPos(7, 8, 9));
        area.setOriginSelected(true);
        check(area.hasSelectionGeometry(), "fill setup");
    }
    static void emptySimple(AreaSelectionSimple area) {
        check(area.getAllSubRegionBoxes().size() == 1, "simple keeps one box");
        Box box = area.getSelectedSubRegionBox();
        check(box != null && box.getPos1() == null && box.getPos2() == null, "simple has selected empty box");
        check(box.getSize().equals(BlockPos.ZERO), "empty size");
        check(box.getSelectedCorner() == Corner.NONE, "corner unselected");
        check(!area.hasSelectionGeometry(), "empty geometry");
        check(area.getExplicitOrigin() == null && !area.isOriginSelected(), "origin reset");
        check(area.getEffectiveOrigin().equals(BlockPos.ZERO), "calculated origin reset: " + area.getEffectiveOrigin());
    }
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        check(net.minecraft.client.Minecraft.getInstance() == null, "no Minecraft client started");

        var simple = new AreaSelectionSimple(true);
        emptySimple(simple);
        fill(simple);
        Box original = simple.getSelectedSubRegionBox();
        simple.clearSelectionGeometry();
        emptySimple(simple);
        check(simple.getSelectedSubRegionBox() == original, "reset preserves box identity");
        emptySimple(AreaSelectionSimple.fromJson(simple.toJson()));
        emptySimple(simple.copy());
        original.setCoordinate(17, Corner.CORNER_1, CoordinateType.X);
        check(original.getPos1().equals(new BlockPos(17, 0, 0)), "editing empty corner initializes at zero");
        check(original.getPos2() == null, "editing one corner preserves other null");
        check(original.getCoordinate(Corner.CORNER_2, CoordinateType.Y) == 0, "read null coordinate is zero");
        Box roundTrip = Box.fromJson(original.toJson());
        check(roundTrip.getPos1().equals(original.getPos1()) && roundTrip.getPos2() == null, "partial box JSON preserves null");
        original.setCoordinate(-11, Corner.CORNER_2, CoordinateType.Z);
        check(original.getPos2().equals(new BlockPos(0, 0, -11)), "second empty corner editing");
        System.out.println("PASS simple reset, empty JSON roundtrip, null coordinate editing");

        check(fi.dy.masa.litematica.util.PositionUtils.getEnclosingAreaCorners(java.util.List.of(new Box(null, null, "empty"))) == null, "empty bounds");
        check(fi.dy.masa.litematica.util.PositionUtils.getEnclosingAreaSize(java.util.List.of(new Box(null, null, "empty"))).equals(BlockPos.ZERO), "empty size");
        var manager = DataManager.getSelectionManager();
        if (manager.getSelectionMode() != SelectionMode.SIMPLE) manager.switchSelectionMode();
        simple = DataManager.getSimpleArea();
        fill(simple);
        grab(manager, simple, simple.getSelectedSubRegionBox());
        manager.setCurrentSelection(null);
        check(manager.hasSelectionToClear(), "reproduce old bug: null named ID cannot clear Simple");
        manager.clearCurrentSelection();
        check(!manager.hasGrabbedElement(), "simple clear releases grab");
        emptySimple(simple);
        check(!manager.hasSelectionToClear(), "simple no repeat clear");
        manager.clearCurrentSelection();
        emptySimple(simple);
        simple.setSelectedSubRegionCornerPos(new BlockPos(9, 64, 12), Corner.CORNER_1);
        simple.setSelectedSubRegionCornerPos(new BlockPos(14, 68, 17), Corner.CORNER_2);
        check(manager.hasSelectionToClear(), "stick corner setters still select after clear");
        manager.clearCurrentSelection();
        emptySimple(simple);
        System.out.println("PASS real DataManager/SelectionManager simple clear and select again");

        manager.switchSelectionMode();
        var named = new AreaSelection();
        fill(named);
        Path file = Path.of("selection-headless-saved.json").toAbsolutePath();
        Files.writeString(file, named.toJson().toString());
        String saved = Files.readString(file);
        manager.setCurrentSelection(file.toString());
        var loaded = manager.getCurrentSelection();
        check(loaded != null && loaded.hasSelectionGeometry(), "named load setup");
        grab(manager, loaded, loaded.getSelectedSubRegionBox());
        manager.clearCurrentSelection();
        check(!manager.hasGrabbedElement() && manager.getCurrentSelection() == null, "named clear releases/unselects");
        check(manager.getCurrentNormalSelectionId() == null, "named ID cleared");
        check(manager.getOrLoadSelection(file.toString()) == loaded && loaded.hasSelectionGeometry(), "named object/geometry retained");
        check(Files.readString(file).equals(saved), "named file untouched");
        check(!manager.hasSelectionToClear(), "named no repeat clear");
        System.out.println("PASS named unselect preserves loaded object and saved file");

        // Generic config defaults read Minecraft.gameDirectory; no constructor/render/window startup.
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        var unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        Object directoryOnlyClient = unsafe.allocateInstance(net.minecraft.client.Minecraft.class);
        set(directoryOnlyClient, "gameDirectory", Path.of(".").toAbsolutePath().toFile());
        Field clientInstance = net.minecraft.client.Minecraft.class.getDeclaredField("instance");
        clientInstance.setAccessible(true);
        clientInstance.set(null, directoryOnlyClient);
        try {
            Configs.Generic.UNHIDE_SCHEMATIC_PROJECTS.setBooleanValue(true);
        } finally {
            clientInstance.set(null, null);
        }
        var project = new SchematicProject(Path.of("."), Path.of("selection-headless-project.json"));
        set(DataManager.getSchematicProjectsManager(), "currentProject", project);
        for (SelectionMode mode : SelectionMode.values()) {
            if (project.getSelectionMode() != mode) project.switchSelectionMode();
            AreaSelection active = project.getSelection();
            fill(active);
            grab(manager, active, active.getSelectedSubRegionBox());
            set(project, "dirty", false);
            manager.clearCurrentSelection();
            check(!manager.hasGrabbedElement() && !active.hasSelectionGeometry(), "project clears geometry and grab: " + mode);
            check(DataManager.getSchematicProjectsManager().getCurrentProject() == project && manager.getCurrentSelection() == active, "project kept active");
            check(!manager.hasSelectionToClear(), "project no repeat clear");
            if (active instanceof AreaSelectionSimple s) emptySimple(s);
            else check(active.getAllSubRegionBoxes().isEmpty() && active.getSelectedSubRegionBox() == null, "project normal boxes removed");
            Field dirty = project.getClass().getDeclaredField("dirty");
            dirty.setAccessible(true);
            check(dirty.getBoolean(project), "project dirty for persistence");
        }
        check(Files.readString(file).equals(saved), "project clear doesn't affect saved area");
        check(net.minecraft.client.Minecraft.getInstance() == null, "still no client");
        System.out.println("PASS project NORMAL/SIMPLE clear; all headless selection checks passed");
    }
}
