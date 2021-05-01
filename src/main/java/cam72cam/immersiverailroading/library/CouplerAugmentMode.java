package cam72cam.immersiverailroading.library;

public enum CouplerAugmentMode {
	ENGAGED,
	DISENGAGED
	;
	
	@Override
	public String toString() {
	    return "immersiverailroading:coupler_augment_mode." + super.toString().toLowerCase();
	}
}
