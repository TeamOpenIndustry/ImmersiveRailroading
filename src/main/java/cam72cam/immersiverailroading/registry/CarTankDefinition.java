package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.CarTank;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.model.FreightTankModel;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.mod.fluid.Fluid;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class CarTankDefinition extends FreightDefinition {

    private List<Fluid> fluidFilter; // null == no filter
    private FluidQuantity capacity;

    public CarTankDefinition(String defID, JsonObject data) throws Exception {
        this(CarTank.class, defID, data);
    }

    CarTankDefinition(Class<? extends CarTank> type, String defID, JsonObject data) throws Exception {
        super(type, defID, data);
    }

    @Override
    public void parseJson(JsonObject data) throws Exception {
        super.parseJson(data);
        JsonObject tank = data.get("tank").getAsJsonObject();
        capacity = FluidQuantity.FromLiters((int) Math.ceil(tank.get("capacity_l").getAsInt() * internal_inv_scale));
        if (tank.has("whitelist")) {
            fluidFilter = new ArrayList<>();
            for (JsonElement allowed : tank.get("whitelist").getAsJsonArray()) {
                Fluid allowedFluid = Fluid.getFluid(allowed.getAsString());
                if (allowedFluid == null) {
                    ImmersiveRailroading.warn("Skipping unknown whitelisted fluid: " + allowed.getAsString());
                    continue;
                }
                fluidFilter.add(allowedFluid);
            }
        }
    }

    @Override
    public List<String> getTooltip(Gauge gauge) {
        List<String> tips = super.getTooltip(gauge);
        tips.add(GuiText.TANK_CAPACITY_TOOLTIP.toString(this.getTankCapaity(gauge).Buckets()));
        return tips;
    }

    public FluidQuantity getTankCapaity(Gauge gauge) {
        return this.capacity.scale(gauge.scale()).min(FluidQuantity.FromBuckets(1)).roundBuckets();
    }

    public List<Fluid> getFluidFilter() {
        return this.fluidFilter;
    }

    @Override
    protected StockModel<?, ?> createModel() throws Exception {
        return new FreightTankModel<>(this);
    }
}
