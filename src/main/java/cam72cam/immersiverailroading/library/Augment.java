package cam72cam.immersiverailroading.library;

import cam72cam.mod.render.Color;

public enum Augment {
	SPEED_RETARDER,
	WATER_TROUGH,
	LOCO_CONTROL,
	ITEM_LOADER,
	ITEM_UNLOADER,
	FLUID_LOADER,
	FLUID_UNLOADER,
    SUPPLIER,
	DETECTOR,
	COUPLER,
	ACTUATOR,
	;
	
	public Color color() {
		switch (this) {
		case DETECTOR:
			return Color.RED;
		case FLUID_LOADER:
			return Color.BLUE;
		case FLUID_UNLOADER:
			return Color.LIGHT_BLUE;
		case ITEM_LOADER:
			return Color.GREEN;
		case ITEM_UNLOADER:
			return Color.LIME;
		case LOCO_CONTROL:
			return Color.BLACK;
		case SPEED_RETARDER:
			return Color.GRAY;
		case WATER_TROUGH:
			return Color.CYAN;
		case COUPLER:
			return Color.ORANGE;
		case ACTUATOR:
			return Color.SILVER;
        case SUPPLIER:
            return Color.PURPLE;
		}
		return Color.WHITE;
	}
}
