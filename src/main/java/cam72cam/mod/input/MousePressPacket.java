package cam72cam.mod.input;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import cam72cam.mod.util.Hand;

class MousePressPacket extends Packet {
	static {
		Packet.register(MousePressPacket::new, PacketDirection.ClientToServer);
	}
	private MousePressPacket() {
		// Forge Reflection
	}
	MousePressPacket(Hand hand, Entity target) {
		super();
		data.setEnum("hand", hand);
		data.setEntity("target", target);
	}

	@Override
	public void handle() {
		Hand hand = data.getEnum("hand", Hand.class);
		Entity target = data.getEntity("target", getWorld());
		if (target != null) {
			switch (hand) {
				case PRIMARY:
					getPlayer().internal.interactOn(target.internal, hand.internal);
					break;
				case SECONDARY:
					getPlayer().internal.attackTargetEntityWithCurrentItem(target.internal);
					break;
			}
		}
	}
}
