package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.mod.entity.Player;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;

public class KeyPressPacket extends Packet {
    @TagField
    private KeyTypes type;

    public KeyPressPacket() { }
    public KeyPressPacket(KeyTypes type) {
        this.type = type;
    }
    @Override
    protected void handle() {
        Player player = getPlayer();
        if (player.getWorld().isServer && player.getRiding() instanceof EntityRollingStock && player.hasPermission(Permissions.LOCOMOTIVE_CONTROL)) {
            player.getRiding().as(EntityRollingStock.class).handleKeyPress(player, type);
        }
    }
}
