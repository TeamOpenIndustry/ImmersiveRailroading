package cam72cam.immersiverailroading.track;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.tile.TileRail;

public class TrackRail extends TrackBase {

	private TrackItems type;
	private int length;

	public TrackRail(BuilderBase builder, int rel_x, int rel_y, int rel_z, TrackItems type, int length) {
		super(builder, rel_x, rel_y, rel_z, IRBlocks.BLOCK_RAIL);
		this.type = type;
		this.length = length;
	}
	
	@Override
	public TileEntity placeTrack() {
		TileRail tileRail = (TileRail) super.placeTrack();
		
		tileRail.setType(type);
		tileRail.setLength(this.length);
		tileRail.setTurnQuarters(builder.info.quarters);
		tileRail.setRailBed(builder.info.railBed);
		tileRail.setDrops(builder.drops);
		tileRail.setGauge(builder.gauge);
		tileRail.setPlacementInfo(builder.info.placementInfo);
		tileRail.setCustomInfo(builder.info.customInfo);
		tileRail.markDirty();
		
		return tileRail;
	}
}
