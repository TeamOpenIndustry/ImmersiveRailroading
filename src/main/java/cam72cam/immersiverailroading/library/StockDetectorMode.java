package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.util.TextUtil;

public enum StockDetectorMode {
	SIMPLE,
	SPEED,
	PASSENGERS,
	CARGO,
	LIQUID,
	;
	
	@Override
	public String toString() {
	    return TextUtil.translate("immersiverailroading:detector_mode." + super.toString().toLowerCase()); 
	}
}
