package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.track.BuilderCrossing;
import cam72cam.immersiverailroading.track.BuilderSlope;
import cam72cam.immersiverailroading.track.BuilderStraight;
import cam72cam.immersiverailroading.track.BuilderTurn;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/*
 * TrackType maps to block state model+render combinations
 * TODO generate dynamic mappings
 */
public enum TrackType implements IStringSerializable {
	STRAIGHT_SMALL(TrackItems.STRAIGHT_SMALL),
	STRAIGHT_MEDIUM(TrackItems.STRAIGHT_MEDIUM),
	STRAIGHT_LONG(TrackItems.STRAIGHT_LARGE),
	
	
	CROSSING(TrackItems.CROSSING),
	
	SLOPE_MEDIUM(TrackItems.SLOPE_MEDIUM),
	SLOPE_LARGE(TrackItems.SLOPE_LARGE),
	
	TURN_MEDIUM_RIGHT(TrackItems.TURN_MEDIUM, TrackDirection.RIGHT),
	TURN_MEDIUM_LEFT(TrackItems.TURN_MEDIUM, TrackDirection.LEFT),
	
	TURN_LARGE_RIGHT(TrackItems.TURN_LARGE, TrackDirection.RIGHT),
	TURN_LARGE_LEFT(TrackItems.TURN_LARGE, TrackDirection.LEFT),
	/*
	MEDIUM_RIGHT_SWITCH(TrackItems.SWITCH_MEDIUM),
	MEDIUM_LEFT_SWITCH(TrackItems.SWITCH_MEDIUM),
	LARGE_RIGHT_SWITCH(TrackItems.SWITCH_LARGE),
	LARGE_LEFT_SWITCH(TrackItems.SWITCH_LARGE),
	
	MEDIUM_RIGHT_PARALLEL_SWITCH(TrackItems.PARALEL_SWITCH_MEDIUM),
	MEDIUM_LEFT_PARALLEL_SWITCH(TrackItems.PARALEL_SWITCH_MEDIUM),
	*/
	;

	private TrackItems type;
	private TrackDirection direction;
	
	TrackType(TrackItems type) {
		this.type = type;
	}
	
	TrackType(TrackItems type, TrackDirection dir) {
		this.type = type;
		this.direction = dir;
	}
	
	public TrackItems getType() {
		return this.type;
	}

	@Override
	public String getName() {
		return this.name().toLowerCase(); 
	}
	
	@Override
	public String toString() {
	    return getName();
	}

	public static TrackType fromMeta(int meta, TrackDirection dir) {
		TrackItems item = TrackItems.fromMeta(meta);
		for (TrackType tt : TrackType.values()) {
			if (tt.type == item) {
				if (tt.direction == null || tt.direction == dir) {
					return tt;
				}
			}
		}
		return null;
	}

	public int getMeta() {
		return this.type.getMeta();
	}

	public TrackDirection getDirection() {
		return this.direction;
	}

	public boolean isTurn() {
		return type.isTurn();
	}
	
	public BuilderBase getBuilder(World world, BlockPos pos, EnumFacing facing) {
		switch (getType()) {
		case STRAIGHT_SMALL:
			return new BuilderStraight(world, pos.getX(), pos.getY(), pos.getZ(), facing, 2);
		case STRAIGHT_MEDIUM:
			return new BuilderStraight(world, pos.getX(), pos.getY(), pos.getZ(), facing, 8);
		case STRAIGHT_LARGE:
			return new BuilderStraight(world, pos.getX(), pos.getY(), pos.getZ(), facing, 16);
		case CROSSING:
			return new BuilderCrossing(world, pos.getX(), pos.getY(), pos.getZ(), facing);
		case SLOPE_MEDIUM:
		case SLOPE_LARGE:
			return new BuilderSlope(world, pos.getX(), pos.getY(), pos.getZ(), facing, this);
		case TURN_MEDIUM:
		case TURN_LARGE:
			return new BuilderTurn(world, pos.getX(), pos.getY(), pos.getZ(), facing, this);
		default:
			return null;
		}
	}
}