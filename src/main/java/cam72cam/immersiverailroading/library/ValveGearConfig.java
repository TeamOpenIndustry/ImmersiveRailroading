package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.resource.Identifier;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ValveGearConfig {
	public final ValveGearType type;
	public final Map<Float, Identifier> custom;
    public boolean invert = false;

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
			animatrix.getValueMap().forEach((percent, anim) -> custom.put(Float.parseFloat(percent), anim.asIdentifier()));
			return new ValveGearConfig(ValveGearType.CUSTOM, custom);
		}
		String name = def.getValue(key).asString();
		if (name != null) {
            name = name.toUpperCase(Locale.ROOT);
			ValveGearConfig valveGearConfig = name.endsWith("_INVERT") ?
                    new ValveGearConfig(ValveGearType.from(name.substring(0, name.length() - 7)), null) :
                    new ValveGearConfig(ValveGearType.from(name), null);
            valveGearConfig.invert = name.endsWith("_INVERT");
            return valveGearConfig;
		}

		return null;
	}
}