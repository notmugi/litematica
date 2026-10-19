package liteux;

import java.nio.file.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import fi.dy.masa.malilib.event.TickHandler;

/** Isolated dev-client check. No user instance or world is opened. */
public class Smoke implements ClientModInitializer {
    private int ticks;
    private int scale = 3;
    private boolean done;
    public void onInitializeClient() {
        TickHandler.getInstance().registerClientTickHandler(this::tick);
    }
    private void tick(MinecraftClient mc) {
        if (done || mc.getOverlay() != null || ++ticks < 50) return;
        Path out = Path.of("..", "screenshots");
        try {
            Files.createDirectories(out);
            if (scale == 3) {
                Files.deleteIfExists(out.resolve("PASS.txt"));
                Files.deleteIfExists(out.resolve("FAIL.txt"));
            }
            mc.options.getGuiScale().setValue(scale);
            mc.onResolutionChanged();
            SelectionEditorCheck.run();
            if (scale == 3) { scale = 2; ticks = 0; return; }
            Files.writeString(out.resolve("PASS.txt"), "Legacy selection dialog lifecycle passed at scales 3 and 2. No world/tool-click test.\n");
            done = true;
            mc.scheduleStop();
        } catch (Throwable e) {
            e.printStackTrace();
            try { Files.writeString(out.resolve("FAIL.txt"), e.toString()); } catch (Exception ignored) {}
            done = true;
            mc.scheduleStop();
        }
    }
}
