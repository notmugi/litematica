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
        GuiBase gui = manager.getEditGui();

        // Manual mode with nothing selected still opens the menu: an empty multi-box
        // editor with the mode toggle and an Add Box button. No selection is created
        // until the user asks for one.
        if (gui == null && manager.getSelectionMode() == SelectionMode.NORMAL &&
                !DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            gui = new GuiAreaSelectionEditorNormal(null);
        }

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
