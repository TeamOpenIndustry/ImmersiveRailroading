package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.util.TextUtil;

public enum CouplerAugmentMode {
	ENGAGED,
	DISENGAGED
	;
	
	@Override
	public String toString() {
	    return TextUtil.translate("immersiverailroading:coupler_augment_mode." + super.toString().toLowerCase()); 
	}
}
