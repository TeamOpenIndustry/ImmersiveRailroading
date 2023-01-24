package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.resource.Identifier;

import java.util.Locale;

public class ValveGearConfig {
	public final ValveGearType type;
	public final Identifier custom;

	public enum ValveGearType {
		CONNECTING,
		STEPHENSON,
		WALSCHAERTS,
		HIDDEN,
		// TODO
		SHAY,
		CLIMAX,
		CUSTOM,
		;

		public static ValveGearType from(String valveGear) {
			if (valveGear == null) {
				return null;
			}
			switch (valveGear) {
				case "TRI_WALSCHAERTS":
				case "GARRAT":
				case "MALLET_WALSCHAERTS":
					return WALSCHAERTS;
				case "T1":
					return STEPHENSON;
				default:
					return ValveGearType.valueOf(valveGear);
			}
		}
	}

	public ValveGearConfig(String def) {
		ValveGearType type;
		Identifier custom = null;
		try {
			type = ValveGearType.from(def.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			type = ValveGearType.CUSTOM;
			custom = new Identifier(def);
			custom = new Identifier(ImmersiveRailroading.MODID, custom.getPath());
		}
		this.type = type;
		this.custom = custom;
	}
}