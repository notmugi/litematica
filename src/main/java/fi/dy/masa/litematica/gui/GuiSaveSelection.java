package fi.dy.masa.litematica.gui;

import javax.annotation.Nullable;
import net.minecraft.client.gui.screen.Screen;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.SelectionManager;
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
