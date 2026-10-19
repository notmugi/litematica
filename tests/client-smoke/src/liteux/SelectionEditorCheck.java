package liteux;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.*;
import fi.dy.masa.litematica.selection.*;
import fi.dy.masa.malilib.gui.*;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.util.StringUtils;

/** Real client/dialog lifecycle regression. Only the isolated smoke game directory is used. */
public final class SelectionEditorCheck
{
    private static Object field(Object object, Class<?> owner, String name) throws Exception
    {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(object);
    }

    private static void check(boolean value, String reason)
    {
        if (!value) throw new AssertionError(reason);
    }

    private static GuiBase screen()
    {
        return (GuiBase) Minecraft.getInstance().gui.screen();
    }

    private static ButtonBase button(String label) throws Exception
    {
        for (Object obj : (List<?>) field(screen(), GuiBase.class, "buttons"))
        {
            ButtonBase b = (ButtonBase) obj;
            if (label.equals(field(b, ButtonBase.class, "displayString"))) return b;
        }
        throw new AssertionError("Missing button: " + label + " in " + screen().getClass());
    }

    private static void click(String label) throws Exception
    {
        ButtonBase b = button(label);
        GuiBase current = screen();
        var event = new MouseButtonEvent(b.getX() + b.getWidth() / 2.0,
                b.getY() + b.getHeight() / 2.0, new MouseButtonInfo(0, 0));
        current.onMouseClicked(event, false);
        if (screen() == current) current.onMouseReleased(event);
    }

    private static String text(String suffix)
    {
        return StringUtils.translate("litematica.gui.button.area_editor." + suffix);
    }

    private static void toggle() throws Exception
    {
        click(StringUtils.translate("litematica.gui.button.area_editor.change_selection_mode",
                DataManager.getSelectionManager().getSelectionMode().getDisplayName()));
    }

    private static void checkEditor(AreaSelection expected) throws Exception
    {
        check(screen() instanceof GuiAreaSelectionEditorNormal, "editor open");
        check(field(screen(), GuiAreaSelectionEditorNormal.class, "selection") == expected,
                "editor must track the manager, not a stale selection");
        button(StringUtils.translate("litematica.gui.ux.back"));
        button(StringUtils.translate("litematica.gui.baby.ghosts.clear"));
    }

    public static GuiBase run() throws Exception
    {
        SelectionManager manager = DataManager.getSelectionManager();
        if (manager.getSelectionMode() != SelectionMode.NORMAL) manager.switchSelectionMode();
        manager.setCurrentSelection(null);
        GuiBase home = new GuiMainMenu();
        GuiSaveSelection.open(home);
        checkEditor(null);
        button(text("create_sub_region"));
        check(manager.getCurrentSelection() == null, "opening must not create anything");
        // Cancel must return to the empty editor without creating a file/box.
        GuiBase empty = screen();
        click(text("create_sub_region"));
        check(screen() instanceof GuiTextInput, "Add Box must open its input");
        screen().onKeyTyped(new KeyEvent(256, 0, 0));
        check(screen() == empty && manager.getCurrentSelection() == null, "cancel is side-effect free");

        // Confirm through the REAL malilib Enter handler, not a direct callback call.
        click(text("create_sub_region"));
        GuiTextFieldGeneric input = (GuiTextFieldGeneric) field(screen(), GuiTextInputBase.class, "textField");
        input.setValueWrapper("smoke-first-box-" + System.nanoTime());
        screen().onKeyTyped(new KeyEvent(257, 0, 0));
        AreaSelection named = manager.getCurrentSelection();
        check(named != null && named.getAllSubRegionBoxes().size() == 1, "first Add Box creates exactly one box");
        check(screen() == empty, "dialog returns to parent, which must refresh its model");
        checkEditor(named);
        button(text("create_schematic"));
        button(text("create_sub_region"));

        // Model setup for the two-box clear case (no player/world in this UI smoke test).
        named.createNewSubRegionBox(new BlockPos(10, 64, 10), "second");
        screen().initGui();
        click(StringUtils.translate("litematica.gui.baby.ghosts.clear"));
        check(manager.getCurrentSelection() == null, "normal Clear unselects");
        checkEditor(null);
        check(named.getAllSubRegionBoxes().size() == 2, "Clear must not delete saved boxes");
        toggle();
        checkEditor(manager.getCurrentSelection());
        toggle();
        checkEditor(null);
        click(StringUtils.translate("litematica.gui.ux.back"));
        check(screen() == home, "Back returns to home");
        GuiSaveSelection.open(home);
        checkEditor(null);

        // A selected normal area with all boxes removed is valid too.
        manager.createNewSelection(DataManager.getAreaSelectionsBaseDirectory(), "smoke-empty-" + System.nanoTime());
        manager.getCurrentSelection().removeAllSubRegionBoxes();
        GuiSaveSelection.open(home);
        checkEditor(manager.getCurrentSelection());
        button(text("create_sub_region"));
        toggle();
        checkEditor(manager.getCurrentSelection());
        toggle();
        checkEditor(manager.getCurrentSelection());
        // Save browser gets a real footer Back button, outside its list area.
        GuiBase editor = screen();
        GuiSchematicSave save = new GuiSchematicSave();
        save.setParent(editor);
        GuiBase.openGui(save);
        ButtonBase back = button(StringUtils.translate("litematica.gui.ux.back"));
        check(back.getY() == save.getScreenHeight() - 26, "save Back bottom aligned");
        check(back.getX() + back.getWidth() == save.getScreenWidth() - 10, "save Back right aligned");
        check(80 + save.getBrowserHeight() <= back.getY(), "save list does not overlap Back");
        click(StringUtils.translate("litematica.gui.ux.back"));
        check(screen() == editor, "save Back returns to editor without saving");
        System.out.println("LITEUX SELECTION EDITOR PASS: empty, cancel, Add Box/Enter return, navigation, clear, mode switches, zero boxes");
        return screen();
    }
}
