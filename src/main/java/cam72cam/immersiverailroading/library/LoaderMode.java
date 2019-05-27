package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.util.TextUtil;

public enum LoaderMode {
	DEFAULT_ON,
	DEFAULT_OFF,
	ALWAYS_ON,
	ALWAYS_OFF,
	;
	
	@Override
	public String toString() {
	    return TextUtil.translate("immersiverailroading:loader_mode." + super.toString().toLowerCase()); 
	}
}
