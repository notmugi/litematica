package fi.dy.masa.litematica.mixin.screen;

import java.util.List;
import fi.dy.masa.malilib.gui.GuiScrollBar;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;
import fi.dy.masa.malilib.gui.wrappers.TextFieldWrapper;
import net.minecraft.client.gui.DrawContext;
import fi.dy.masa.malilib.util.GuiUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Both category and inherited mod selectors; do not alter other mods' screens. */
@Mixin(value = WidgetDropDownList.class, remap = false)
public abstract class MixinWidgetDropDownList
{
    @Shadow protected boolean isOpen;
    @Shadow @Final protected int maxVisibleEntries;
    @Shadow @Final protected List<?> filteredEntries;
    @Shadow @Final protected TextFieldWrapper<GuiTextFieldGeneric> searchBar;
    @Shadow @Final protected GuiScrollBar scrollBar;
    @Shadow protected abstract void updateFilteredEntries();

    @Unique
    private boolean litematica$isOurScreen()
    {
        var screen = GuiUtils.getCurrentScreen();
        // Walk superclasses too: test screens and third-party subclasses keep the same behavior.
        for (Class<?> type = screen == null ? null : screen.getClass(); type != null; type = type.getSuperclass())
            if (type.getName().startsWith("fi.dy.masa.litematica.gui.")) return true;
        return false;
    }

    @Inject(method = "isMouseOver", at = @At("HEAD"), cancellable = true)
    private void litematica$visibleBounds(int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir)
    {
        if (!this.isOpen || !this.litematica$isOurScreen()) return;
        var widget = (WidgetDropDownList<?>) (Object) this;
        int top = widget.getY();
        // malilib displays a search field above the header only after typing a filter.
        if (!this.searchBar.getTextField().getText().isEmpty()) top -= 18;
        int bottom = widget.getY() + widget.getHeight() *
                (1 + Math.min(this.maxVisibleEntries, this.filteredEntries.size())) + 1;
        cir.setReturnValue(mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth() &&
                mouseY >= top && mouseY < bottom);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void litematica$closeOnLeave(int mouseX, int mouseY, boolean selected, DrawContext ctx, CallbackInfo ci)
    {
        var widget = (WidgetDropDownList<?>) (Object) this;
        if (this.isOpen && this.litematica$isOurScreen() && !widget.isMouseOver(mouseX, mouseY))
        {
            // Match native close cleanup; otherwise reopening retains a hidden filter/drag.
            this.isOpen = false;
            this.scrollBar.setIsDragging(false);
            this.searchBar.getTextField().setText("");
            this.updateFilteredEntries();
        }
    }
}
