package fi.dy.masa.litematica.gui;

import net.minecraft.client.MinecraftClient;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;

/** A screen owns its original world and placement, never the mutable current selection. */
public final class PlacementUiContext
{
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final net.minecraft.client.world.ClientWorld world = this.mc.world;
    private final net.minecraft.client.network.ClientPlayerEntity player = this.mc.player;
    private final net.minecraft.client.network.ClientPlayNetworkHandler connection = this.mc.getNetworkHandler();
    private final fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
    private final SchematicPlacement placement;

    public PlacementUiContext(SchematicPlacement placement)
    {
        this.placement = placement;
    }

    public boolean valid(GuiBase gui)
    {
        if (this.world == null || this.player == null || this.mc.world != this.world || this.mc.player != this.player ||
            this.mc.getNetworkHandler() != this.connection || DataManager.getSchematicPlacementManager() != this.manager)
        {
            gui.addMessage(MessageType.ERROR, "litematica.gui.quick_place.world_changed");
            return false;
        }
        // Project placements are registered with the manager but not SchematicHolder.
        // Unloading a source already removes all of its manager placements.
        if (!this.manager.getAllSchematicsPlacements().contains(this.placement))
        {
            gui.addMessage(MessageType.ERROR, "litematica.gui.quick_place.removed");
            return false;
        }
        return true;
    }
}
