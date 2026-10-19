package liteux;

import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.*;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.render.RenderUtils;

/** Opens only UI in an isolated dev client. No user world, account, or installation is touched. */
public class Smoke implements ClientModInitializer
{
    private int ticks;
    private int screenIndex;
    private int scale = 3;
    private GuiBase shown;
    private static final Path OUTPUT = Path.of("..", "screenshots");
    private final List<Supplier<GuiBase>> screens = List.of(
        () -> { try { return SelectionEditorCheck.run(); } catch (Exception e) { throw new RuntimeException(e); } }, DropdownCheck::new, GuiMainMenu::new, GuiUxHelp::new, GuiToolModePicker::new, GuiGhosts::new,
        () -> new GuiGhosts(true), GuiQuickPlace::new,
        () -> { DataManager.setConfigGuiTab(GuiConfigs.ConfigGuiTab.GENERIC); return new GuiConfigs(); },
        () -> { DataManager.setConfigGuiTab(GuiConfigs.ConfigGuiTab.HOTKEYS); return new GuiConfigs(); },
        () -> { DataManager.setConfigGuiTab(GuiConfigs.ConfigGuiTab.COLORS); return new GuiConfigs(); },
        () -> { DataManager.setConfigGuiTab(GuiConfigs.ConfigGuiTab.ALL); return new GuiConfigs(); },
        GuiRenderLayer::new,
        () -> new GuiUxConfirm(null, "Delete world blocks in 2 selected boxes?\nBlock contents may be lost. There is no automatic world undo.", () -> true, () -> { throw new AssertionError("Smoke must never execute a world edit"); })
    );

    @Override
    public void onInitializeClient()
    {
        TickHandler.getInstance().registerClientTickHandler(this::tick);
    }

    private void tick(MinecraftClient mc)
    {
        if (mc.getOverlay() != null) return;
        try
        {
            if (++this.ticks < 30) return;
            if (this.shown == null)
            {
                Configs.Generic.UX_WELCOME_DISMISSED.setBooleanValue(true);
                Files.createDirectories(OUTPUT);
                if (this.screenIndex == 0 && this.scale == 3) { Files.deleteIfExists(OUTPUT.resolve("FAIL.txt")); Files.deleteIfExists(OUTPUT.resolve("PASS.txt")); }
                mc.options.getGuiScale().setValue(this.scale);
                mc.onResolutionChanged();
                this.shown = this.screens.get(this.screenIndex).get();
                GuiBase.openGui(this.shown);
                this.ticks = 0;
                return;
            }
            checkBounds(this.shown);
            if (this.shown instanceof GuiUxBase)
            {
                this.shown.onKeyTyped(258, 0, 0);
                if (mc.currentScreen != this.shown) throw new AssertionError("Tab executed an action");
            }
            String name = String.format("scale%d-%02d-%s.png", this.scale, this.screenIndex, this.shown.getClass().getSimpleName());
            try (var image = ScreenshotRecorder.takeScreenshot(mc.getFramebuffer())) {
                image.writeTo(OUTPUT.resolve(name));
            }
            System.out.println("LITEUX SMOKE: " + name + " " + fi.dy.masa.malilib.util.GuiUtils.getScaledWindowWidth() + "x" + fi.dy.masa.malilib.util.GuiUtils.getScaledWindowHeight());
            if (this.shown instanceof GuiUxConfirm)
            {
                this.shown.onKeyTyped(257, 0, 0);
                if (mc.currentScreen == this.shown) throw new AssertionError("Enter did not cancel default confirmation");
            }
            ++this.screenIndex;
            if (this.screenIndex >= this.screens.size() && this.scale == 3)
            {
                this.scale = 2;
                this.screenIndex = 0;
            }
            else if (this.screenIndex >= this.screens.size())
            {
                Files.writeString(OUTPUT.resolve("PASS.txt"), "All smoke screens opened and rendered. Buttons within bounds. This is not a world-edit integration test.\n");
                mc.scheduleStop();
                return;
            }
            this.shown = null;
            this.ticks = 25;
        }
        catch (Throwable failure)
        {
            failure.printStackTrace();
            try { Files.writeString(OUTPUT.resolve("FAIL.txt"), failure.toString()); } catch (Exception ignored) {}
            mc.scheduleStop();
        }
    }

    private static void checkBounds(GuiBase screen) throws Exception
    {
        var field = GuiBase.class.getDeclaredField("buttons");
        field.setAccessible(true);
        for (Object value : (List<?>) field.get(screen))
        {
            WidgetBase button = (WidgetBase) value;
            if (button.getX() < 0 || button.getY() < 0 ||
                button.getX() + button.getWidth() > fi.dy.masa.malilib.util.GuiUtils.getScaledWindowWidth() ||
                button.getY() + button.getHeight() > fi.dy.masa.malilib.util.GuiUtils.getScaledWindowHeight())
                throw new AssertionError(screen.getClass().getSimpleName() + " button outside viewport at " + button.getX() + "," + button.getY());
        }
    }
}
