package fi.dy.masa.litematica.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.input.KeyEvent;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;

/** Shared presentation and keyboard access for the task-oriented screens. */
public abstract class GuiUxBase extends GuiBase
{
    private final List<UxButton> actions = new ArrayList<>();
    private int focused = -1;

    @Override
    public void initGui()
    {
        this.actions.clear();
        this.focused = -1;
        super.initGui();
        this.useTitleHierarchy = false;
    }

    @Override
    public <T extends ButtonBase> T addButton(T button, IButtonActionListener listener)
    {
        if (button instanceof UxButton action) this.actions.add(action);
        return super.addButton(button, listener);
    }

    @Override
    public boolean onMouseClicked(net.minecraft.client.input.MouseButtonEvent input, boolean doubleClick)
    {
        this.focused = -1;
        this.actions.forEach(action -> action.setKeyboardFocused(false));
        return super.onMouseClicked(input, doubleClick);
    }

    @Override
    protected void drawScreenBackground(GuiContext ctx, int mouseX, int mouseY)
    {
        RenderUtils.drawRect(ctx, 0, 0, this.width, this.height, 0xF2111923);
        RenderUtils.drawRect(ctx, 0, 0, this.width, 24, 0xFF192633);
        RenderUtils.drawRect(ctx, 0, 23, this.width, 1, 0xFF526579);
    }

    @Override
    public boolean onKeyTyped(KeyEvent input)
    {
        if (input.key() == 258 && !this.actions.isEmpty())
        {
            int step = GuiBase.isShiftDown() ? -1 : 1;
            for (int i = 0; i < this.actions.size(); ++i)
            {
                this.focused = this.focused < 0 ? (step > 0 ? 0 : this.actions.size() - 1)
                        : Math.floorMod(this.focused + step, this.actions.size());
                if (this.actions.get(this.focused).isEnabled()) break;
            }
            for (int i = 0; i < this.actions.size(); ++i) this.actions.get(i).setKeyboardFocused(i == this.focused);
            UxButton current = this.actions.get(this.focused);
            if (current.isEnabled()) this.mc.getNarrator().saySystemNow(String.join(". ", current.getHoverStrings()));
            return true;
        }
        if ((input.key() == 257 || input.key() == 335 || input.key() == 32) && this.focused >= 0)
        {
            this.actions.get(this.focused).activate();
            return true;
        }
        return super.onKeyTyped(input);
    }
}
