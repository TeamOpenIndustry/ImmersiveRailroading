package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import com.google.gson.JsonObject;

public abstract class FreightDefinition extends EntityRollingStockDefinition {

    private boolean showCurrentLoadOnly;

    FreightDefinition(Class<? extends EntityRollingStock> type, String defID, JsonObject data) throws Exception {
        super(type, defID, data);
    }

    public void parseJson(JsonObject data) throws Exception {
        super.parseJson(data);
        if (data.has("show_current_load_only")) {
            this.showCurrentLoadOnly = data.get("show_current_load_only").getAsBoolean();
        } else {
            this.showCurrentLoadOnly = false;
        }
    }

    public boolean shouldShowCurrentLoadOnly() {
        return this.showCurrentLoadOnly;
    }

}
