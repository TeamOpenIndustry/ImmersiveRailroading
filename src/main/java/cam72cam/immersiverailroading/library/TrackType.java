package cam72cam.immersiverailroading.library;

import net.minecraft.util.IStringSerializable;

/*
 * TrackType maps to block state model+render combinations
 * TODO generate dynamic mappings
 */
public enum TrackType implements IStringSerializable {
	STRAIGHT_SMALL(TrackItems.STRAIGHT_SMALL),
	STRAIGHT_MEDIUM(TrackItems.STRAIGHT_MEDIUM),
	STRAIGHT_LONG(TrackItems.STRAIGHT_LARGE),
	
	
	CROSSING(TrackItems.CROSSING),
	/*
	LARGE_SLOPE_WOOD(TrackItems.SLOPE_MEDIUM),
	LARGE_SLOPE_GRAVEL(TrackItems.SLOPE_MEDIUM),
	LARGE_SLOPE_BALLAST(TrackItems.SLOPE_MEDIUM),
	VERY_LARGE_SLOPE_WOOD(TrackItems.SLOPE_LARGE),
	VERY_LARGE_SLOPE_GRAVEL(TrackItems.SLOPE_LARGE),
	VERY_LARGE_SLOPE_BALLAST(TrackItems.SLOPE_LARGE),
	
	MEDIUM_RIGHT_TURN(TrackItems.TURN_MEDIUM, TrackDirection.RIGHT),
	MEDIUM_LEFT_TURN(TrackItems.TURN_MEDIUM, TrackDirection.LEFT),
	*/
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
}