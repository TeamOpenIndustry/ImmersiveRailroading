package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.physics.TickPos;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;

import java.util.List;

/*
 * Movable rolling stock sync packet
 */
public class MRSSyncPacket extends Packet {
	@TagField
	private EntityMoveableRollingStock stock;
	@TagField
	private double tps;
	@TagField(mapper = TickPosMapper.class)
	private List<TickPos> positions;

	public static class TickPosMapper implements TagMapper<List<TickPos>> {
		@Override
		public TagAccessor<List<TickPos>> apply(Class<List<TickPos>> type, String fieldName, TagField tag) {
			return new TagAccessor<>(
					(data, positions) -> data.setList(fieldName, positions, TickPos::toTag),
					data -> data.getList(fieldName, TickPos::new)
			);
		}
	}

	public MRSSyncPacket() { }

	public MRSSyncPacket(EntityMoveableRollingStock mrs, List<TickPos> positions) {
		this.stock = mrs;
		this.tps = ConfigDebug.serverTickCompensation ? 20 : mrs.getWorld().getTPS(positions.size());
		this.positions = positions;
	}
	@Override
	public void handle() {
		if (stock != null) {
            stock.handleTickPosPacket(positions, tps);
		}
	}
}
