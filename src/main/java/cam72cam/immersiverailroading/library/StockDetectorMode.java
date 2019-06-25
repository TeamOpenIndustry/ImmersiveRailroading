package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;

public enum StockDetectorMode {
	SIMPLE,
	SPEED,
	PASSENGERS,
	CARGO,
	LIQUID,
	COMPUTER,
	;
	
	@Override
	public String toString() {
	    return TextUtil.translate("immersiverailroading:detector_mode." + super.toString().toLowerCase()); 
	}
}
