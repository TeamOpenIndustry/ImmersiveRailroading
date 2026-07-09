package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.CraftingMachineMode;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.umc.api.item.ItemStack;
import cam72cam.umc.api.math.Vec3i;
import cam72cam.umc.api.net.Packet;
import cam72cam.umc.api.serialization.TagField;

public class MultiblockSelectCraftPacket extends Packet {
	@TagField
	private Vec3i pos;
	@TagField
	private ItemStack stack;
	@TagField
	private CraftingMachineMode mode;

	public MultiblockSelectCraftPacket() { }

	public MultiblockSelectCraftPacket(Vec3i tilePreviewPos, ItemStack selected, CraftingMachineMode mode) {
		this.pos = tilePreviewPos;
		this.stack = selected;
		this.mode = mode;
	}

	@Override
	public void handle() {
		TileMultiblock tile = getWorld().getBlockEntity(pos, TileMultiblock.class);
		if (tile == null) {
			ImmersiveRailroading.warn("Got invalid craft update packet at %s", pos);
			return;
		}
		tile.setCraftItem(stack);
		tile.setCraftMode(mode);
	}
}
