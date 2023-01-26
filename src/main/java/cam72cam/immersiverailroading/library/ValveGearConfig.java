package cam72cam.immersiverailroading.library;

import cam72cam.mod.resource.Identifier;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
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

	public ValveGearConfig(ValveGearType type) {
		this.type = type;
		this.custom = null;
	}

	public ValveGearConfig(JsonElement def) {
		if (def.isJsonObject()) {
			JsonObject anim = def.getAsJsonObject().get("animatrix").getAsJsonObject();
			type = ValveGearType.CUSTOM;
			custom = new HashMap<>();
			for (Map.Entry<String, JsonElement> elem : anim.entrySet()) {
				custom.put(Float.parseFloat(elem.getKey()), new Identifier(elem.getValue().getAsString()));
			}
		} else {
			type = ValveGearType.from(def.getAsString().toUpperCase(Locale.ROOT));
			custom = null;
		}
	}
}