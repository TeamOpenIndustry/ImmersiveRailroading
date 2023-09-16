package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.CarFreight;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.mod.resource.Identifier;

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
    protected Identifier defaultDataLocation() {
        return new Identifier(ImmersiveRailroading.MODID, "rolling_stock/default/freight.caml");
    }

    @Override
    public void loadData(DataBlock data) throws Exception {
        super.loadData(data);
        DataBlock freight = data.getBlock("freight");
        this.numSlots = (int) Math.ceil(freight.getValue("slots").asInteger() * internal_inv_scale);
        this.width = (int) Math.ceil(freight.getValue("width").asInteger() * internal_inv_scale);
        List<DataBlock.Value> cargo = freight.getValues("cargo");
        this.validCargo = cargo == null ? null : cargo.stream().map(DataBlock.Value::asString).collect(Collectors.toList());
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
