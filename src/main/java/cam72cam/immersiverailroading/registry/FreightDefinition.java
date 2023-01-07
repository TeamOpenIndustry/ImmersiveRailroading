package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.model.FreightModel;
import cam72cam.immersiverailroading.model.StockModel;
import com.google.gson.JsonObject;

public abstract class FreightDefinition extends EntityRollingStockDefinition {

    private boolean showCurrentLoadOnly;

    FreightDefinition(Class<? extends EntityRollingStock> type, String defID, JsonObject data) throws Exception {
        super(type, defID, data);
    }

    public void parseJson(JsonObject data) throws Exception {
        super.parseJson(data);
        this.showCurrentLoadOnly = getOrDefault(data, "show_current_load_only", false);
    }

    @Override
    protected StockModel<?, ?> createModel() throws Exception {
        return new FreightModel<>(this);
    }

    public boolean shouldShowCurrentLoadOnly() {
        return this.showCurrentLoadOnly;
    }

}
