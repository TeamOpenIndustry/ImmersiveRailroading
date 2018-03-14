package cam72cam.immersiverailroading.track;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.tile.TileRailGag;

public class TrackGag extends TrackBase {
	public TrackGag(BuilderBase builder, int rel_x, int rel_y, int rel_z) {
		super(builder, rel_x, rel_y, rel_z, IRBlocks.BLOCK_RAIL_GAG, EnumFacing.NORTH);
	}

	@Override
	public TileEntity placeTrack() {
		TileRailGag tileGag = (TileRailGag) super.placeTrack();
		
		tileGag.setFlexible(isFlexible());
		
		return tileGag;
	}
}
