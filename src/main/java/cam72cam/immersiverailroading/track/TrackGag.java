package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.tile.RailBaseInstance;
import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.tile.RailGagInstance;
import cam72cam.mod.math.Vec3i;

public class TrackGag extends TrackBase {
	public TrackGag(BuilderBase builder, Vec3i rel) {
		super(builder, rel, IRBlocks.BLOCK_RAIL_GAG);
	}

	@Override
	public RailBaseInstance placeTrack(boolean actuallyPlace) {
		RailGagInstance tileGag = (RailGagInstance) super.placeTrack(actuallyPlace);
		
		tileGag.setFlexible(isFlexible());
		
		return tileGag;
	}
}
