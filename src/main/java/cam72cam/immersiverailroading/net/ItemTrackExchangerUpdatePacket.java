package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.items.ItemTrackExchanger;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.util.Hand;

public class ItemTrackExchangerUpdatePacket extends Packet {
	@TagField
	private String track;

	public ItemTrackExchangerUpdatePacket() {}

	public ItemTrackExchangerUpdatePacket(String track) {
		this.track = track;
	}

	@Override
	public void handle() {
		ItemStack stack = getPlayer().getHeldItem(Hand.PRIMARY);
		ItemTrackExchanger.Data data = new ItemTrackExchanger.Data(stack);
		data.track = track;
		data.write();
		getPlayer().setHeldItem(Hand.PRIMARY, stack);
	}
}
