package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.CraftingMachineMode;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;

public class MultiblockSelectCraftPacket extends Packet {
	public MultiblockSelectCraftPacket() {
		// Forge Reflection
	}
	public MultiblockSelectCraftPacket(Vec3i tilePreviewPos, ItemStack selected, CraftingMachineMode mode) {
		data.setVec3i("pos", tilePreviewPos);
		data.setStack("stack", selected);
		data.setEnum("mode", mode);
	}

	@Override
	public void handle() {
		Vec3i pos = data.getVec3i("pos");
		ItemStack stack = data.getStack("stack");
		CraftingMachineMode mode = data.getEnum("mode", CraftingMachineMode.class);

		TileMultiblock tile = getWorld().getTileEntity(pos, TileMultiblock.class);
		if (tile == null) {
			ImmersiveRailroading.warn("Got invalid craft update packet at %s", pos);
			return;
		}
		tile.setCraftItem(stack);
		tile.setCraftMode(mode);
	}
}
