package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;

import java.util.Locale;

public enum CraftingType {
	CASTING,
	CASTING_HAMMER,
	PLATE_SMALL,
	PLATE_MEDIUM,
	PLATE_LARGE,
	PLATE_BOILER,
	;
	
	public boolean isCasting() {
		return this == CASTING || this == CASTING_HAMMER;
	}

	public boolean isPlate() {
		return !isCasting();
	}

	@Override
	public String toString() {
		return TextUtil.translate("immersiverailroading:crafting_type." + super.toString().toLowerCase(Locale.ROOT));
	}
}

