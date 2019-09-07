package cam72cam.immersiverailroading.net;

import cam72cam.mod.entity.Player;
import cam72cam.mod.net.Packet;

import cam72cam.immersiverailroading.entity.EntityRidableRollingStock;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.mod.net.PacketDirection;

public class KeyPressPacket extends Packet {
	static {
		Packet.register(KeyPressPacket::new, PacketDirection.ClientToServer);
	}

	public KeyPressPacket() {
		// Forge Reflection
	}
	public KeyPressPacket(KeyTypes key, Player player, EntityRidableRollingStock riding) {
		super();
		data.setEnum("key", key);
		data.setEntity("riding", riding);
		data.setBoolean("sprinting", player.internal.isSprinting());
	}

	@Override
	public void handle() {
		KeyTypes key = data.getEnum("key", KeyTypes.class);
		EntityRidableRollingStock riding = data.getEntity("riding", getWorld(), EntityRidableRollingStock.class);
		boolean sprinting = data.getBoolean("sprinting");

		if (riding != null) {
			riding.handleKeyPress(getPlayer(), key, sprinting);
		}
	}
}
