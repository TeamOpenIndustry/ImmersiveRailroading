package cam72cam.immersiverailroading.library;

import net.minecraft.item.EnumDyeColor;

public enum Augment {
	SPEED_RETARDER,
	WATER_TROUGH,
	LOCO_CONTROL,
	ITEM_LOADER,
	ITEM_UNLOADER,
	FLUID_LOADER,
	FLUID_UNLOADER,
	DETECTOR,
	COUPLER,
	;
	
	public EnumDyeColor tempColor() {
		switch (this) {
		case DETECTOR:
			return EnumDyeColor.RED;
		case FLUID_LOADER:
			return EnumDyeColor.BLUE;
		case FLUID_UNLOADER:
			return EnumDyeColor.LIGHT_BLUE;
		case ITEM_LOADER:
			return EnumDyeColor.GREEN;
		case ITEM_UNLOADER:
			return EnumDyeColor.LIME;
		case LOCO_CONTROL:
			return EnumDyeColor.BLACK;
		case SPEED_RETARDER:
			return EnumDyeColor.GRAY;
		case WATER_TROUGH:
			return EnumDyeColor.CYAN;
		case COUPLER:
			return EnumDyeColor.ORANGE;
		}
		return null;
	}
}
