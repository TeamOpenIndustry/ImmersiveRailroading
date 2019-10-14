package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.mod.entity.Player;
import cam72cam.mod.net.Packet;

public class KeyPressPacket extends Packet {
    public KeyPressPacket() {
        
    }
    public KeyPressPacket(KeyTypes type) {
        data.setEnum("type", type);
    }
    @Override
    protected void handle() {
        Player player = getPlayer();
        KeyTypes type = data.getEnum("type", KeyTypes.class);
        if (player.getWorld().isServer && player.getRiding() instanceof EntityRollingStock) {
            player.getRiding().as(EntityRollingStock.class).handleKeyPress(player, type);
        }
    }
}
