package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.items.ItemTrackBlueprint;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;

public class ItemRailUpdatePacket extends Packet {
	@TagField
	private RailSettings settings;
	@TagField
	private Vec3i pos;
	@TagField
	private int guiOpenType;

	public ItemRailUpdatePacket() { }

	public ItemRailUpdatePacket(RailSettings settings, int guiOpenType) {
		this.settings = settings;
		this.guiOpenType = guiOpenType;
	}

	public ItemRailUpdatePacket(Vec3i tilePreviewPos, RailSettings settings, int guiOpenType) {
		this.pos = tilePreviewPos;
		this.settings = settings;
		this.guiOpenType = guiOpenType;
	}

	@Override
	public void handle() {
		if (pos != null) {
			TileRailPreview tile = this.getWorld().getBlockEntity(pos, TileRailPreview.class);
			if (tile != null) {
				ItemStack stack = tile.getItem();
				settings.write(stack);
				ItemTrackBlueprint.Data.writeTo(stack, guiOpenType);
				tile.setItem(stack, getPlayer());
			}
		} else {
			Player player = this.getPlayer();
			ItemStack stack = player.getHeldItem(Player.Hand.PRIMARY);
			settings.write(stack);
			ItemTrackBlueprint.Data.writeTo(stack, guiOpenType);
			player.setHeldItem(Player.Hand.PRIMARY, stack);
		}
	}
}
