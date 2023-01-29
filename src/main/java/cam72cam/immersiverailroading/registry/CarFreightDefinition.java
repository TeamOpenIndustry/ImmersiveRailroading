package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.entity.CarFreight;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CarFreightDefinition extends FreightDefinition {

    private int numSlots;
    private int width;
    private List<String> validCargo;

    public CarFreightDefinition(Class<? extends CarFreight> cls, String defID, DataBlock data) throws Exception {
        super(cls, defID, data);
    }

    public CarFreightDefinition(String defID, DataBlock data) throws Exception {
        this(CarFreight.class, defID, data);
    }

    @Override
    public void parseJson(DataBlock data) throws Exception {
        super.parseJson(data);
        DataBlock freight = data.getBlock("freight");
        if (freight != null) {
            this.numSlots = (int) Math.ceil(freight.getValue("slots").getInteger() * internal_inv_scale);
            this.width = (int) Math.ceil(freight.getValue("width").getInteger() * internal_inv_scale);
            this.validCargo = freight.getValues("cargo").stream().map(DataBlock.Value::getString).collect(Collectors.toList());
        } else {
            this.numSlots = 0;
            this.width = 0;
            this.validCargo = null;
        }
    }

    @Override
    public List<String> getTooltip(Gauge gauge) {
        List<String> tips = super.getTooltip(gauge);
        if (numSlots > 0) {
            tips.add(GuiText.FREIGHT_CAPACITY_TOOLTIP.toString(this.getInventorySize(gauge)));
        }
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
