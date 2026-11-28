package cam72cam.immersiverailroading.net.offline;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.mod.entity.EntityPool;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.StrictTagMapper;
import cam72cam.mod.serialization.TagField;

import java.util.UUID;

public class MapLocomotiveLinkPacket extends Packet {
    @TagField(value = "loco", mapper = StrictTagMapper.class)
    private UUID loco;

    public MapLocomotiveLinkPacket() {
    }

    public MapLocomotiveLinkPacket(UUID loco) {
        this.loco = loco;
    }

    @Override
    protected void handle() {
        Player player = getPlayer();
        if (loco == null) {
            return;
        }
        Player.Hand hand = null;
        if (player.getHeldItem(Player.Hand.PRIMARY).is(IRItems.ITEM_WIRELESS_REMOTECONTROL)) {
            hand = Player.Hand.PRIMARY;
        } else if (player.getHeldItem(Player.Hand.SECONDARY).is(IRItems.ITEM_WIRELESS_REMOTECONTROL)) {
            hand = Player.Hand.SECONDARY;
        }
        if (hand == null) return;

        Locomotive stock = null;
        ModdedEntity pooled = EntityPool.get(getWorld()).getEntity(loco);
        if (pooled != null && pooled.getSelf() instanceof Locomotive l) {
            stock = l;
        } else {
            stock = getWorld().getEntity(loco, Locomotive.class);
        }

        if (stock != null) {
            // 与直接右键机车完全复用同一套链接逻辑（权限、无线电能力、NBT 写入）。
            stock.onClick(player, hand);
        }
    }
}
