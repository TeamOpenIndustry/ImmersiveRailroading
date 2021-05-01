package cam72cam.immersiverailroading.library;

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
	    return "immersiverailroading:detector_mode." + super.toString().toLowerCase();
	}
}
