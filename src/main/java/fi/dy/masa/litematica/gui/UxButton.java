package fi.dy.masa.litematica.gui;

import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.minecraft.client.gui.DrawContext;
import fi.dy.masa.malilib.render.RenderUtils;

/** Native widgets and hit testing, with a quiet, high-contrast presentation. */
public class UxButton extends ButtonGeneric
{
    private boolean keyboardFocused;
    private String subtitle;

    public UxButton(int x, int y, int width, int height, String label, String... hover)
    {
        super(x, y, width, height, label, hover);
    }

    public UxButton subtitle(String text)
    {
        this.subtitle = text;
        return this;
    }

    public boolean isEnabled() { return this.enabled; }
    public void setKeyboardFocused(boolean focused) { this.keyboardFocused = focused; }
    public void activate() { if (this.enabled && this.actionListener != null) this.actionListener.actionPerformedWithButton(this, 0); }

    private String fit(String text)
    {
        int width = Math.max(1, this.width - 18);
        return this.getStringWidth(text) <= width ? text
                : this.textRenderer.trimToWidth(text, Math.max(1, width - this.getStringWidth("..."))) + "...";
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected, DrawContext ctx)
    {
        if (!this.visible) return;

        this.hovered = this.isMouseOver(mouseX, mouseY);
        boolean active = this.enabled && (this.hovered || this.keyboardFocused);
        int background = !this.enabled ? 0xFF19212B : active ? 0xFF273F53 : 0xFF202D3A;
        int border = active ? 0xFF82D9CF : 0xFF526579;
        int foreground = this.enabled ? 0xFFF2F5F8 : 0xFF9AA6B3;
        RenderUtils.drawOutlinedBox(this.x, this.y, this.width, this.height, background, border);
        if (this.height >= 32) RenderUtils.drawRect(this.x, this.y, 3, this.height, border);
        boolean detail = this.subtitle != null && this.height >= 38;
        String label = this.fit(this.displayString);
        if (detail)
        {
            this.drawString(this.x + 10, this.y + 8, foreground, label, ctx);
            String hint = this.fit(this.subtitle);
            this.drawString(this.x + 10, this.y + 23, 0xFFB6C7D6, hint, ctx);
        }
        else
        {
            this.drawCenteredString(this.x + this.width / 2, this.y + (this.height - this.textRenderer.fontHeight) / 2, foreground, label, ctx);
        }
    }
}
