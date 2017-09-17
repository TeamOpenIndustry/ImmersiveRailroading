package cam72cam.immersiverailroading.library;

import net.minecraft.util.IStringSerializable;

public enum TrackItems implements IStringSerializable {
	STRAIGHT,
	CROSSING,
	SLOPE,
	TURN,
	SWITCH,
	;
	
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