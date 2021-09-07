package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.entity.Tender;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.model.FreightTankModel;
import cam72cam.immersiverailroading.model.StockModel;
import com.google.gson.JsonObject;

import java.util.List;

public class TenderDefinition extends CarTankDefinition {
    private int numSlots;
    private int width;
    private boolean showCurrentLoadOnly;

    public TenderDefinition(String defID, JsonObject data) throws Exception {
        super(Tender.class, defID, data);
    }

    @Override
    public void parseJson(JsonObject data) throws Exception {
        super.parseJson(data);

        JsonObject tender = data.get("tender").getAsJsonObject();
        this.numSlots = (int) Math.ceil(tender.get("slots").getAsInt() * internal_inv_scale);
        this.width = (int) Math.ceil(tender.get("width").getAsInt() * internal_inv_scale);
        this.showCurrentLoadOnly = false;
        if (tender.has("show_current_load_only")) {
            this.showCurrentLoadOnly = tender.get("show_current_load_only").getAsBoolean();
        }
    }

    @Override
    public List<String> getTooltip(Gauge gauge) {
        List<String> tips = super.getTooltip(gauge);
        tips.add(GuiText.FREIGHT_CAPACITY_TOOLTIP.toString(this.getInventorySize(gauge)));
        return tips;
    }

    public int getInventorySize(Gauge gauge) {
        return (int) Math.ceil(numSlots * gauge.scale());
    }

    public int getInventoryWidth(Gauge gauge) {
        return (int) Math.ceil(width * gauge.scale());
    }

    public boolean shouldShowCurrentLoadOnly() {
        return this.showCurrentLoadOnly;
    }

    @Override
    protected StockModel<?> createModel() throws Exception {
        return new FreightTankModel<>(this);
    }
}
