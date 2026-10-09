package fi.dy.masa.litematica.gui;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfirmAction;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.gui.Message.MessageType;

/** Uses malilib's wrapping/layout, with Cancel as the keyboard default. */
public class GuiUxConfirm extends GuiConfirmAction
{
    private final BooleanSupplier valid;
    private final Runnable action;
    private final fi.dy.masa.litematica.util.UxSafety.Ticket ticket = new fi.dy.masa.litematica.util.UxSafety.Ticket();
    private boolean confirmFocused;
    private final String message;
    private final String baseTitle;
    private int page;
    private int pages;

    public GuiUxConfirm(Screen parent, String message, BooleanSupplier valid, Runnable action)
    {
        super(420, "litematica.gui.ux.safety.title", null, parent, message);
        this.message = message;
        this.baseTitle = this.title;
        this.valid = valid;
        this.action = action;
    }

    @Override
    public void initGui()
    {
        this.clearButtons();
        int width = Math.min(420, this.getScreenWidth() - 20);
        var lines = new java.util.ArrayList<String>();
        fi.dy.masa.malilib.util.StringUtils.splitTextToLines(lines, this.message, width - 30);
        int perPage = Math.max(1, (this.getScreenHeight() - 80) / (this.fontHeight + 1));
        this.pages = Math.max(1, (lines.size() + perPage - 1) / perPage);
        this.page = Math.min(this.page, this.pages - 1);
        this.messageLines.clear();
        this.messageLines.addAll(lines.subList(this.page * perPage, Math.min(lines.size(), (this.page + 1) * perPage)));
        this.title = this.baseTitle + (this.pages > 1 ? " (" + (this.page + 1) + "/" + this.pages + ")" : "");
        this.setWidthAndHeight(width, this.getMessageHeight() + 50);
        this.centerOnScreen();
        super.initGui();
        if (this.pages > 1)
        {
            int y = this.dialogTop + this.dialogHeight - 24;
            this.addButton(new UxButton(this.dialogLeft + width - 58, y, 22, 20, "‹"),
                    (button, mouse) -> this.changePage(-1));
            this.addButton(new UxButton(this.dialogLeft + width - 32, y, 22, 20, "›"),
                    (button, mouse) -> this.changePage(1));
        }
    }

    @Override
    protected int getButtonWidth() { return Math.min(super.getButtonWidth() + 16, (this.dialogWidth - 90) / 2); }

    private void changePage(int delta)
    {
        this.page = Math.clamp(this.page + delta, 0, this.pages - 1);
        this.initGui();
    }

    @Override
    public void tick()
    {
        super.tick();
        fi.dy.masa.litematica.util.UxSafety.pollHeldTriggers();
    }

    @Override
    public boolean keyReleased(KeyEvent input)
    {
        fi.dy.masa.litematica.util.UxSafety.released(input.key());
        return super.keyReleased(input);
    }

    @Override
    public boolean onMouseReleased(net.minecraft.client.input.MouseButtonEvent input)
    {
        fi.dy.masa.litematica.util.UxSafety.released(input.button() - 100);
        return super.onMouseReleased(input);
    }

    @Override
    public boolean onMouseScrolled(double x, double y, double horizontal, double vertical)
    {
        if (vertical != 0) this.changePage(vertical > 0 ? -1 : 1);
        return true;
    }

    @Override
    protected void createButton(int x, int y, int width, ButtonType type)
    {
        boolean confirm = type == ButtonType.OK;
        String label = (confirm == this.confirmFocused ? "> " : "") + type.getDisplayName();
        this.addButton(new UxButton(x, y, width, 20, label), (button, mouse) -> this.finish(confirm));
    }

    private void finish(boolean confirm)
    {
        if (fi.dy.masa.malilib.util.GuiUtils.getCurrentScreen() != this) return;
        boolean allowed = this.ticket.accept(confirm, this.valid);
        // Close first: a chained operation may legitimately open another screen.
        GuiBase.openGui(this.getParent());
        if (confirm && allowed) this.action.run();
        else if (confirm) InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.gui.ux.safety.stale");
    }

    @Override
    public boolean onKeyTyped(KeyEvent input)
    {
        switch (input.key())
        {
            case 256 -> this.finish(false); // Escape
            case 266 -> this.changePage(-1); // Page Up
            case 267 -> this.changePage(1); // Page Down
            case 258 -> { this.confirmFocused = !this.confirmFocused; this.initGui(); } // Tab
            case 257, 335, 32 -> this.finish(this.confirmFocused); // Enter / keypad / Space
            default -> { return super.onKeyTyped(input); }
        }
        return true;
    }
}
