package cam72cam.immersiverailroading.net;

import java.util.UUID;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.items.ItemWirelessRemotecontrol;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.Player.Hand;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;

public class KeyPressPacket extends Packet {
    @TagField
    private boolean disableIndependentThrottle;
    @TagField
    private KeyTypes type;
    @TagField
    private UUID loco;

    public KeyPressPacket() { }
    
    public KeyPressPacket(KeyTypes type, UUID loco) {
        this.type = type;
        this.loco = loco;   
    }
    
    public KeyPressPacket(KeyTypes type) {
        this.disableIndependentThrottle = Config.ImmersionConfig.disableIndependentThrottle;
        this.type = type;
        Player player = MinecraftClient.getPlayer();
        if (player.getRiding() instanceof EntityRollingStock) {
            // Do it client side, expect server to overwrite
            player.getRiding().as(EntityRollingStock.class).handleKeyPress(player, type, disableIndependentThrottle);
        }
    }
    @Override
    protected void handle() {
        Player player = getPlayer();
        if (player.getRiding() instanceof EntityRollingStock && player.hasPermission(Permissions.LOCOMOTIVE_CONTROL)) {
            player.getRiding().as(EntityRollingStock.class).handleKeyPress(player, type, disableIndependentThrottle);
        }
        
        // Player controls with Wireless Remote Control
        if (loco == null || type == null) {
            return;
        }

        EntityRollingStock stock = getWorld().getEntity(loco, LocomotiveDiesel.class); 
                                                                                        
        if (stock instanceof LocomotiveDiesel || player.getRiding() instanceof EntityRollingStock && player.hasPermission(Permissions.LOCOMOTIVE_CONTROL)) {
            ItemStack held = player.getHeldItem(Hand.SECONDARY); 
            
            if (stock instanceof LocomotiveDiesel) {
                ItemWirelessRemotecontrol.Data data = new ItemWirelessRemotecontrol.Data(held);

                if (loco.equals(data.linked)) {
                    ((LocomotiveDiesel) stock).handleKeyPress(player, type, disableIndependentThrottle);
                }
            }
        }
    }
}
