package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.items.ItemMultiblockBlueprint;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;

public class ItemMultiblockUpdatePacket extends Packet {
    @TagField
    private String name;

    public ItemMultiblockUpdatePacket() {
    }

    public ItemMultiblockUpdatePacket(String name) {
        this.name = name;
    }

    @Override
    protected void handle() {
        Player player = this.getPlayer();
        ItemStack stack = player.getHeldItem(Player.Hand.PRIMARY);
        ItemMultiblockBlueprint.Data data = new ItemMultiblockBlueprint.Data(stack);
        data.multiblock = MultiblockRegistry.get(name);
        data.write();
        player.setHeldItem(Player.Hand.PRIMARY, stack);
    }
}
