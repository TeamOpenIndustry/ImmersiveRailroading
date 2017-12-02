package cam72cam.immersiverailroading.library;

public enum Augment {
	SPEED_RETARDER,
	WATER_TROUGH,
	LOCO_CONTROL,
	ITEM_LOADER,
	ITEM_UNLOADER,
	FLUID_LOADER,
	FLUID_UNLOADER,
	DETECTOR
	;
	
	public boolean isFluidHandler() {
		switch (this) {
		case FLUID_LOADER:
		case FLUID_UNLOADER:
		case WATER_TROUGH:
			return true;
		default:
			return false;
		}
	}
}
