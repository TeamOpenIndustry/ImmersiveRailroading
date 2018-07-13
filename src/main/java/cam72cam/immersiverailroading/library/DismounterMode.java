package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.util.TextUtil;

public enum DismounterMode {
	ALL,
	PLAYER,
	VILLAGER,
	PASSIVE,
	HOSTILE,
	;
	
	@Override
	public String toString() {
	    return TextUtil.translate("immersiverailroading:dismounter_mode." + super.toString().toLowerCase()); 
	}
}
