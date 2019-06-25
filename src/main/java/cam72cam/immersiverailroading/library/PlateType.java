package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;

public enum PlateType {
	SMALL,
	MEDIUM,
	LARGE,
	BOILER,
	;
	
	@Override
	public String toString() {
	    return TextUtil.translate("immersiverailroading:plate." + super.toString().toLowerCase()); 
	}
}
