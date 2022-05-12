package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;

import java.util.Locale;

public enum PlateType {
	SMALL,
	MEDIUM,
	LARGE,
	BOILER,
	;
	
	@Override
	public String toString() {
	    return TextUtil.translate("immersiverailroading:plate." + super.toString().toLowerCase(Locale.ROOT));
	}

	public int platesPerBlock() {
		switch (this) {
			case SMALL:
				return 8;
			case MEDIUM:
				return 4;
			case LARGE:
				return 2;
			case BOILER:
				return 1;
		}
		return 0;
	}
}
