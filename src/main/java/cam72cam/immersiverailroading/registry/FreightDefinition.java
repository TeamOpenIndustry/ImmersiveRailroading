package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.immersiverailroading.model.FreightModel;
import cam72cam.immersiverailroading.model.StockModel;

public abstract class FreightDefinition extends EntityRollingStockDefinition {

    private boolean showCurrentLoadOnly;

    FreightDefinition(Class<? extends EntityRollingStock> type, String defID, DataBlock data) throws Exception {
        super(type, defID, data);
    }

    public void parseJson(DataBlock data) throws Exception {
        super.parseJson(data);
        this.showCurrentLoadOnly = data.getValue("show_current_load_only").getBoolean(false);
    }

    @Override
    protected StockModel<?, ?> createModel() throws Exception {
        return new FreightModel<>(this);
    }

    public boolean shouldShowCurrentLoadOnly() {
        return this.showCurrentLoadOnly;
    }

}
