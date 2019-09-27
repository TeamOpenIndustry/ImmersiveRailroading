package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.entity.CarFreight;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class CarFreightDefinition extends FreightDefinition {

    private int numSlots;
    private int width;
    private List<String> validCargo;

    public CarFreightDefinition(String defID, JsonObject data) throws Exception {
        super(CarFreight.class, defID, data);

        // Handle null data
        if (validCargo == null) {
            validCargo = new ArrayList<>();
        }
    }

    @Override
    public void parseJson(JsonObject data) throws Exception {
        super.parseJson(data);
        JsonObject freight = data.get("freight").getAsJsonObject();
        this.numSlots = (int) Math.ceil(freight.get("slots").getAsInt() * internal_inv_scale);
        this.width = (int) Math.ceil(freight.get("width").getAsInt() * internal_inv_scale);
        this.validCargo = new ArrayList<>();
        for (JsonElement el : freight.get("cargo").getAsJsonArray()) {
            validCargo.add(el.getAsString());
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

    @Override
    public boolean acceptsLivestock() {
        return true;
    }
}
