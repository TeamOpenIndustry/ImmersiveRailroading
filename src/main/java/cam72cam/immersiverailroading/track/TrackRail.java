package cam72cam.immersiverailroading.track;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.tile.TileRail;

public class TrackRail extends TrackBase {

	private TrackItems type;
	private Vec3d center;
	private int length;
	private int quarter;
	private int turnQuarters;
	private TrackDirection direction = TrackDirection.NONE;
	private Vec3d placementPosition;

	public TrackRail(BuilderBase builder, int rel_x, int rel_y, int rel_z, EnumFacing rel_rotation, TrackItems type, int length, int quarter, Vec3d placementPosition) {
		super(builder, rel_x, rel_y, rel_z, IRBlocks.BLOCK_RAIL, rel_rotation);
		this.type = type;
		this.quarter = quarter;
		this.length = length;
		this.placementPosition = placementPosition;
	}
	
	public void setRotationCenter(Vec3d center) {
		//center = builder.convertRelativeCenterPositions(rel_cx, rel_cy, rel_cz, EnumFacing.NORTH);
		this.center = center;
	}
	
	@Override
	public TileEntity placeTrack() {
		TileRail tileRail = (TileRail) super.placeTrack();
		
		tileRail.setFacing(super.getFacing());
		tileRail.setCenter(center);
		tileRail.setType(type);
		tileRail.setLength(this.length);
		tileRail.setDirection(direction);
		tileRail.setRotationQuarter(quarter);
		tileRail.setTurnQuarters(turnQuarters);
		tileRail.setPlacementPosition(placementPosition);
		tileRail.setRailBed(builder.info.railBed);
		tileRail.setDrops(builder.drops);
		tileRail.setGauge(builder.gauge);
		
		return tileRail;
	}

	public void setDirection(TrackDirection direction) {
		this.direction = direction;
	}

	public void setTurnQuarters(int quarters) {
		this.turnQuarters = quarters;
	}
}
