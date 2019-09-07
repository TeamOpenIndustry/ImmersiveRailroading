package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.items.ItemTrackBlueprint;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import cam72cam.mod.util.Hand;

public class ItemRailUpdatePacket extends Packet {
	static {
		Packet.register(ItemRailUpdatePacket::new, PacketDirection.ClientToServer);
	}
	public ItemRailUpdatePacket() {
		// Forge Reflection
	}

	public ItemRailUpdatePacket(RailSettings settings) {
		super();
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
			Player player = this.getPlayer();
			ItemStack stack = player.getHeldItem(Hand.PRIMARY);
			ItemTrackBlueprint.settings(stack, settings);
			player.setHeldItem(Hand.PRIMARY, stack);
		}
	}
}
