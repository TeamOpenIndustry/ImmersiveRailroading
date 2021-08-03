package cam72cam.immersiverailroading.library;

import java.util.Locale;

public enum CouplerAugmentMode {
	ENGAGED,
	DISENGAGED
	;
	
	@Override
	public String toString() {
	    return "immersiverailroading:coupler_augment_mode." + super.toString().toLowerCase(Locale.ROOT);
	}
}
