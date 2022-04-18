package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.physics.TickPos;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;

import java.util.List;

/*
 * Movable rolling stock sync packet
 */
public class MRSSyncPacket extends Packet {
	@TagField
	private EntityMoveableRollingStock stock;
	@TagField
	private double tps;
	@TagField(mapper = TickPos.ListTagMapper.class)
	private List<TickPos> positions;

	public MRSSyncPacket() { }

	public MRSSyncPacket(EntityMoveableRollingStock mrs, List<TickPos> positions) {
		this.stock = mrs;
		this.tps = mrs.getWorld().getTPS(positions.size());
		this.positions = positions;
	}
	@Override
	public void handle() {
		if (stock != null) {
            stock.handleTickPosPacket(positions, tps);
		}
	}
}
