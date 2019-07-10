package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.tile.RailInstance;
import cam72cam.immersiverailroading.tile.RailBaseInstance;
import cam72cam.mod.math.Vec3i;

public class TrackRail extends TrackBase {

	public TrackRail(BuilderBase builder, Vec3i rel) {
		super(builder, rel, IRBlocks.BLOCK_RAIL);
	}

	@Override
	public RailBaseInstance placeTrack(boolean actuallyPlace) {
		RailInstance tileRail = (RailInstance) super.placeTrack(actuallyPlace);

		tileRail.info = builder.info;
		tileRail.setDrops(builder.drops);

		return tileRail;
	}
}
