package cam72cam.immersiverailroading.net;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.net.Packet;
import cam72cam.mod.util.Hand;

public class MousePressPacket extends Packet {
	public MousePressPacket() {
		// Forge Reflection
	}
	public MousePressPacket(Hand hand, Entity target) {
		super();
		data.setEnum("hand", hand);
		data.setEntity("target", target);
	}

	@Override
	public void handle() {
		Hand hand = data.getEnum("hand", Hand.class);
		Entity target = data.getEntity("target");
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
