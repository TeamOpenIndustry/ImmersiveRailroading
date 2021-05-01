package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.items.ItemTrackExchanger;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;

public class ItemTrackExchangerUpdatePacket extends Packet {
	@TagField
	private String track;

	@TagField
	private ItemStack railBed;

	public ItemTrackExchangerUpdatePacket() {}

	public ItemTrackExchangerUpdatePacket(String track, ItemStack railBed) {
		this.track = track;
		this.railBed = railBed;
	}

	@Override
	public void handle() {
		ItemStack stack = getPlayer().getHeldItem(Player.Hand.PRIMARY);
		ItemTrackExchanger.Data data = new ItemTrackExchanger.Data(stack);
		data.track = track;
		data.railBed = railBed;
		data.write();
		getPlayer().setHeldItem(Player.Hand.PRIMARY, stack);
	}
}
