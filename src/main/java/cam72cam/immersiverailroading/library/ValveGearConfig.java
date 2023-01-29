package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.resource.Identifier;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ValveGearConfig {
	public final ValveGearType type;
	public final Map<Float, Identifier> custom;

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

	public ValveGearConfig(ValveGearType type, Map<Float, Identifier> custom) {
		this.type = type;
		this.custom = custom;
	}

	public static ValveGearConfig get(DataBlock def, String key) {
		DataBlock block = def.getBlock(key);
		if (block != null) {
			DataBlock animatrix = block.getBlock("animatrix");
			Map<Float, Identifier> custom = new HashMap<>();
			animatrix.getValueMap().forEach((percent, anim) -> custom.put(Float.parseFloat(percent), anim.getIdentifier()));
			return new ValveGearConfig(ValveGearType.CUSTOM, custom);
		}
		String name = def.getValue(key).getString();
		if (name != null) {
			return new ValveGearConfig(ValveGearType.from(name.toUpperCase(Locale.ROOT)), null);
		}

		return null;
	}
}