package cam72cam.immersiverailroading.registry;

import com.google.gson.JsonObject;

public abstract class FreightDefinition extends EntityRollingStockDefinition {

	private boolean showCurrentLoadOnly;

	public FreightDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
	}
	
	public void parseJson(JsonObject data) throws Exception {
		super.parseJson(data);
		if (data.has("show_current_load_only")) {
			this.showCurrentLoadOnly = data.get("show_current_load_only").getAsBoolean();
		}
	}

	public boolean shouldShowCurrentLoadOnly() {
		return this.showCurrentLoadOnly;
	}
	
}
