package fi.dy.masa.litematica.gui;

import fi.dy.masa.malilib.gui.GuiRenderLayerEditBase;
import fi.dy.masa.malilib.gui.interfaces.IGuiIcon;
import fi.dy.masa.malilib.gui.widgets.WidgetCheckBox;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.position.LayerRange;
import fi.dy.masa.litematica.data.DataManager;

public class GuiRenderLayer extends GuiRenderLayerEditBase
{
    @Override
    public void initGui()
    {
        super.initGui();

        this.addButton(new UxButton(10, 26, 100, 20, StringUtils.translate("litematica.gui.ux.back"),
                "litematica.gui.ux.back.tip"), (button, mouseButton) -> this.closeGui(true));
        this.createLayerEditControls(10, 60, this.getLayerRange());
    }

    @Override
    protected LayerRange getLayerRange()
    {
        return DataManager.getRenderLayerRange();
    }

    @Override
    protected IGuiIcon getValueAdjustButtonIcon()
    {
        return Icons.BUTTON_PLUS_MINUS_16;
    }

    @Override
    protected int createHotkeyCheckBoxes(int x, int y, LayerRange layerRange)
    {
        String label = StringUtils.translate("litematica.gui.label.render_layers.hotkey");
        String hover = StringUtils.translate("litematica.gui.label.render_layers.hover.hotkey");

        WidgetCheckBox cb = new WidgetCheckBox(x, y + 4, Icons.CHECKBOX_UNSELECTED, Icons.CHECKBOX_SELECTED, label, hover);
        cb.setChecked(layerRange.getMoveLayerRangeMax(), false);
        cb.setListener(new RangeHotkeyListener(layerRange, true));
        this.addWidget(cb);

        y += 23;
        cb = new WidgetCheckBox(x, y + 4, Icons.CHECKBOX_UNSELECTED, Icons.CHECKBOX_SELECTED, label, hover);
        cb.setChecked(layerRange.getMoveLayerRangeMin(), false);
        cb.setListener(new RangeHotkeyListener(layerRange, false));
        this.addWidget(cb);

        return y;
    }

}
