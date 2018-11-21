package cam72cam.immersiverailroading.track;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.tile.TileRail;
import net.minecraft.util.math.BlockPos;

public class TrackRail extends TrackBase {

	public TrackRail(BuilderBase builder, BlockPos rel) {
		super(builder, rel, IRBlocks.BLOCK_RAIL);
	}

	@Override
	public TileEntity placeTrack() {
		TileRail tileRail = (TileRail) super.placeTrack();

		tileRail.info = builder.info;
		tileRail.setDrops(builder.drops);
		tileRail.markDirty();
		
		return tileRail;
	}
}
