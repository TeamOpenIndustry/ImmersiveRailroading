package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.tile.TileRailGag;
import net.minecraft.util.math.BlockPos;

public class TrackGag extends TrackBase {
	public TrackGag(BuilderBase builder, BlockPos rel) {
		super(builder, rel, IRBlocks.BLOCK_RAIL_GAG);
	}

	@Override
	public TileRailBase placeTrack(boolean actuallyPlace) {
		TileRailGag tileGag = (TileRailGag) super.placeTrack(actuallyPlace);
		
		tileGag.setFlexible(isFlexible());
		
		return tileGag;
	}
}
