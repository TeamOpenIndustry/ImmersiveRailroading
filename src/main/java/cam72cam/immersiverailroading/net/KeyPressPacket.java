package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.umc.api.MinecraftClient;
import cam72cam.umc.api.entity.Player;
import cam72cam.umc.api.net.Packet;
import cam72cam.umc.api.serialization.TagField;

public class KeyPressPacket extends Packet {
    @TagField
    private boolean disableIndependentThrottle;
    @TagField
    private KeyTypes type;

    public KeyPressPacket() { }
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
    }
}
