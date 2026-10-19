package fi.dy.masa.litematica.gui;

import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.Screen;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextInput;

/** Compatibility entry point; the selection editor is the Copy-a-build screen. */
public class GuiSaveSelection extends GuiBase
{
    public GuiSaveSelection()
    {
    }

    public static void open(@Nullable Screen parent)
    {
        SelectionManager manager = DataManager.getSelectionManager();
        GuiBase gui = manager.getEditGui();

        if (gui != null)
        {
            gui.setParent(parent);
            GuiBase.openGui(gui);
            return;
        }

        // Manual mode needs a named selection but the old saved-selections browser is gone:
        // instead of leaving the user stuck on a no-selection warning, let them name a
        // new one right here. Nothing is created unless the user confirms.
        if (manager.getSelectionMode() == SelectionMode.NORMAL &&
                !DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            GuiTextInput input = new GuiTextInput(256, "litematica.gui.title.create_area_selection", "",
                    parent, string -> {
                        manager.createNewSelection(DataManager.getAreaSelectionsBaseDirectory(), string);
                        open(parent);
                    });
            input.setParent(parent);
            GuiBase.openGui(input);
        }
    }

    @Override
    public void initGui()
    {
        open(this.getParent());
    }
}
