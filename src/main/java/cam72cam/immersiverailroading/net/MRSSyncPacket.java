package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.physics.TickPos;
import cam72cam.immersiverailroading.proxy.CommonProxy;
import cam72cam.mod.net.Packet;

import java.util.List;

/*
 * Movable rolling stock sync packet
 */
public class MRSSyncPacket extends Packet {
	public MRSSyncPacket() {
		// Forge Reflection
	}
	public MRSSyncPacket(EntityMoveableRollingStock mrs, List<TickPos> positions) {
		data.setEntity("stock", mrs);
		data.setDouble("tps", ConfigDebug.serverTickCompensation ? 20 : mrs.getWorld().getTPS(positions.size()));
		data.setList("positions", positions, TickPos::toTag);
	}
	@Override
	public void handle() {
		EntityMoveableRollingStock stock = data.getEntity("stock", EntityMoveableRollingStock.class);
		if (stock != null) {
            stock.handleTickPosPacket(data.getList("positions", TickPos::new), data.getDouble("tps"));
		}
	}
}
