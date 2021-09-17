package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.model.LocomotiveModel;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.resource.Identifier;
import com.google.gson.JsonObject;

import java.util.List;

public abstract class LocomotiveDefinition extends FreightDefinition {
    public boolean toggleBell;
    public Identifier bell;
    private String works;
    private int power;
    private int traction;
    private Speed maxSpeed;
    private boolean hasRadioEquipment;
    public boolean muliUnitCapable;
    private boolean isCabCar;
    private boolean isLinkedBrakeThrottle;

    LocomotiveDefinition(Class<? extends EntityRollingStock> type, String defID, JsonObject data) throws Exception {
        super(type, defID, data);

        // Handle null data
        if (works == null) {
            works = "Unknown";
        }
        if (maxSpeed == null) {
            maxSpeed = Speed.ZERO;
        }
    }

    @Override
    public void parseJson(JsonObject data) throws Exception {
        super.parseJson(data);

        works = data.get("works").getAsString();

        JsonObject properties = data.get("properties").getAsJsonObject();

        hasRadioEquipment = properties.has("radio_equipped") && properties.get("radio_equipped").getAsBoolean();

        isCabCar = properties.has("cab_car") && properties.get("cab_car").getAsBoolean();
        if (isCabCar) {
            muliUnitCapable = true;
        } else {
            power = (int) Math.ceil(properties.get("horsepower").getAsInt() * internal_inv_scale);
            traction = (int) Math.ceil(properties.get("tractive_effort_lbf").getAsInt() * internal_inv_scale);
            maxSpeed = Speed.fromMetric(properties.get("max_speed_kmh").getAsDouble() * internal_inv_scale);
            muliUnitCapable = !properties.has("multi_unit_capable") ? this.multiUnitDefault() : properties.get("multi_unit_capable").getAsBoolean();
        }
        isLinkedBrakeThrottle = properties.has("isLinkedBrakeThrottle") && properties.get("linked_brake_throttle").getAsBoolean();
        toggleBell = !properties.has("toggle_bell") || properties.get("toggle_bell").getAsBoolean();
    }

    protected abstract boolean multiUnitDefault();

    @Override
    protected StockModel<?> createModel() throws Exception {
        return new LocomotiveModel<>(this);
    }

    @Override
    public List<String> getTooltip(Gauge gauge) {
        List<String> tips = super.getTooltip(gauge);
        tips.add(GuiText.LOCO_WORKS.toString(this.works));
        if (!isCabCar) {
            tips.add(GuiText.LOCO_HORSE_POWER.toString(this.getHorsePower(gauge)));
            tips.add(GuiText.LOCO_TRACTION.toString(this.getStartingTractionNewtons(gauge)));
            tips.add(GuiText.LOCO_MAX_SPEED.toString(this.getMaxSpeed(gauge).metricString()));
        }
        return tips;
    }

    public int getHorsePower(Gauge gauge) {
        return (int) Math.ceil(gauge.scale() * this.power);
    }

    /**
     * @return tractive effort in newtons
     */
    public int getStartingTractionNewtons(Gauge gauge) {
        return (int) Math.ceil(gauge.scale() * this.traction * 4.44822);
    }

    public Speed getMaxSpeed(Gauge gauge) {
        return Speed.fromMinecraft(gauge.scale() * this.maxSpeed.minecraft());
    }

    public double getBrakePower() {
        // TODO
        return 1;
    }

    public boolean getRadioCapability() {
        return this.hasRadioEquipment;
    }

    public boolean isLinearBrakeControl() {
        return isLinkedBrakeThrottle() || super.isLinearBrakeControl();
    }

    public boolean isLinkedBrakeThrottle() {
        return isLinkedBrakeThrottle;
    }

    @Override
    protected boolean independentBrakeDefault() {
        return true;
    }

    public boolean isCabCar() {
        return isCabCar;
    }
}
