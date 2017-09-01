package cam72cam.immersiverailroading.library;

import net.minecraft.util.IStringSerializable;

public enum TrackItems implements IStringSerializable {
	STRAIGHT,
	CROSSING,
	SLOPE,
	TURN,
	SWITCH,
	PARALEL_SWITCH,
	;
	
	public int getMeta() {
		return this.ordinal();
	}
	
	public static TrackItems fromMeta(int meta) {
		return TrackItems.values()[meta];
	}
	@Override
	public String getName() {
		return this.name().toLowerCase(); 
	}
	
	@Override
	public String toString() {
	    return getName();
	}

	public boolean isTurn() {
		return this == TURN;
	}
}