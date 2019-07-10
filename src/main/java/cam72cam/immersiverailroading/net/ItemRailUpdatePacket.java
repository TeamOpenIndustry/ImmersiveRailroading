package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.items.ItemTrackBlueprint;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;

public class ItemRailUpdatePacket extends Packet {
	public ItemRailUpdatePacket() {
		// Forge Reflection
	}

	public ItemRailUpdatePacket(int slot, RailSettings settings) {
		super();
		data.setInteger("slot", slot);
		data.set("settings", settings.toNBT());
	}

	public ItemRailUpdatePacket(Vec3i tilePreviewPos, RailSettings settings) {
		super();
		data.setVec3i("pos", tilePreviewPos);
		data.set("settings", settings.toNBT());
	}

	@Override
	public void handle() {
		RailSettings settings = new RailSettings(data.get("settings"));
		if (data.hasKey("pos")) {
			Vec3i pos = data.getVec3i("pos");
			TileRailPreview tile = this.getWorld().getBlockEntity(pos, TileRailPreview.class);
			ItemStack stack = tile.getItem();
			ItemTrackBlueprint.settings(stack, settings);
			tile.setItem(stack);
		} else {
			int slot = data.getInteger("slot");
			Player player = this.getPlayer();
			ItemStack stack = new ItemStack(player.internal.inventory.getStackInSlot(slot));
			ItemTrackBlueprint.settings(stack, settings);
			player.internal.inventory.setInventorySlotContents(slot, stack.internal);
		}
	}
}
