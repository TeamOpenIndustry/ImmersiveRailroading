package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;

/*
 * Movable rolling stock sync packet
 */
public class PaintSyncPacket extends Packet {
	static {
		Packet.register(PaintSyncPacket::new, PacketDirection.ServerToClient);
	}
	public PaintSyncPacket() {
		// Forge Reflection
	}
	public PaintSyncPacket(EntityRollingStock mrs) {
		data.setString("texture", mrs.texture);
		data.setEntity("target", mrs);
	}
	@Override
	public void handle() {
		String texture = data.getString("texture");
		EntityRollingStock stock = data.getEntity("target", getWorld(), EntityRollingStock.class);
		if (stock != null) {
			stock.texture = texture;
		}
	}
}
