package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.items.nbt.ItemTrackExchangerType;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.net.Packet;
import cam72cam.mod.util.Hand;

public class ItemTrackExchangerUpdatePacket extends Packet {
	public ItemTrackExchangerUpdatePacket() {}

	public ItemTrackExchangerUpdatePacket(String track) {
		data.setString("track", track);
	}

	@Override
	public void handle() {
		ItemStack stack = getPlayer().getHeldItem(Hand.PRIMARY);
		ItemTrackExchangerType.set(stack, data.getString("track"));
		getPlayer().setHeldItem(Hand.PRIMARY, stack);
	}
}
