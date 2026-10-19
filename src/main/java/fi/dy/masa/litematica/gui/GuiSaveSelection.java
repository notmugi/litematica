package fi.dy.masa.litematica.gui;

import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.Screen;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.malilib.gui.GuiBase;

/** Compatibility entry point; the selection editor is the Copy-a-build screen. */
public class GuiSaveSelection extends GuiBase
{
    public GuiSaveSelection()
    {
    }

    public static void open(@Nullable Screen parent)
    {
        SelectionManager manager = DataManager.getSelectionManager();

        // Manual mode without a current selection still opens the full editor:
        // create a default named selection (the saved-selections browser stays removed).
        if (manager.getCurrentSelection() == null &&
                !DataManager.getSchematicProjectsManager().hasProjectOpen() &&
                manager.getSelectionMode() == SelectionMode.NORMAL)
        {
            manager.createNewSelection(DataManager.getAreaSelectionsBaseDirectory(), "selection");
        }

        GuiBase gui = manager.getEditGui();

        if (gui != null)
        {
            gui.setParent(parent);
            GuiBase.openGui(gui);
        }
    }

    @Override
    public void initGui()
    {
        open(this.getParent());
    }
}
