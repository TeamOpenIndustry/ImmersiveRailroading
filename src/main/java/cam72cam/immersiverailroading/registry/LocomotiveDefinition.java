package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.unit.ForceDisplayType;
import cam72cam.immersiverailroading.library.unit.PowerDisplayType;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.model.LocomotiveModel;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.resource.Identifier;

import java.util.List;

public abstract class LocomotiveDefinition extends FreightDefinition {
    public boolean toggleBell;
    public SoundDefinition bell;
    private String works;
    private double power_kW;
    private double traction_N;
    private Speed maxSpeed;
    private boolean hasRadioEquipment;
    public boolean muliUnitCapable;
    private boolean isCabCar;
    private boolean isLinkedBrakeThrottle;
    private boolean isCog;
    private double factorOfAdhesion;

    LocomotiveDefinition(Class<? extends EntityRollingStock> type, String defID, DataBlock data) throws Exception {
        super(type, defID, data);
    }

    @Override
    protected Identifier defaultDataLocation() {
        return new Identifier(ImmersiveRailroading.MODID, "rolling_stock/default/locomotive.caml");
    }

    @Override
    public void loadData(DataBlock data) throws Exception {
        super.loadData(data);

        works = data.getValue("works").asString();

        DataBlock properties = data.getBlock("properties");

        hasRadioEquipment = properties.getValue("radio_equipped").asBoolean(false);

        isCabCar = readCabCarFlag(data);
        if (isCabCar) {
            power_kW = 0;
            traction_N = 0;
            maxSpeed = Speed.ZERO;
            muliUnitCapable = true;
            factorOfAdhesion = 0;
        } else {
            if (properties.getValue("horsepower").asFloat() != null) {
                power_kW = properties.getValue("horsepower").asFloat() * PowerDisplayType.hpToKW * internal_inv_scale;
            } else if (properties.getValue("power_hp").asFloat() != null) {
                power_kW = properties.getValue("power_hp").asFloat() * PowerDisplayType.hpToKW * internal_inv_scale;
            } else if (properties.getValue("power_kw").asFloat() != null) {
                power_kW = properties.getValue("power_kw").asFloat() * internal_inv_scale;
            } else {
                power_kW = properties.getValue("power_w").asFloat() / 1000 * internal_inv_scale;
            }

            if (properties.getValue("tractive_effort_lbf").asFloat() != null) {
                traction_N = properties.getValue("tractive_effort_lbf").asFloat() * ForceDisplayType.lbfToNewton * internal_inv_scale;
            } else if (properties.getValue("tractive_effort_kn").asFloat() != null) {
                traction_N = properties.getValue("tractive_effort_kn").asFloat() * 1000 * internal_inv_scale;
            } else {
                traction_N = properties.getValue("tractive_effort_n").asFloat() * internal_inv_scale;
            }

            factorOfAdhesion = properties.getValue("factor_of_adhesion").asDouble(4);
            maxSpeed = Speed.fromMetric(properties.getValue("max_speed_kmh").asDouble() * internal_inv_scale);
            muliUnitCapable = properties.getValue("multi_unit_capable").asBoolean();
        }
        isLinkedBrakeThrottle = properties.getValue("isLinkedBrakeThrottle").asBoolean();
        toggleBell = properties.getValue("toggle_bell").asBoolean();
        isCog = properties.getValue("cog").asBoolean();
    }

    protected boolean readCabCarFlag(DataBlock data) {
        return data.getBlock("properties").getValue("cab_car").asBoolean(false);
    }

    @Override
    protected StockModel<?, ?> createModel() throws Exception {
        return new LocomotiveModel<>(this);
    }

    @Override
    public List<String> getTooltip(Gauge gauge) {
        List<String> tips = super.getTooltip(gauge);
        tips.add(GuiText.LOCO_WORKS.toString(this.works));
        if (!isCabCar) {
            float power = ConfigGraphics.powerUnit.convertFromWatt(this.getWatt(gauge));
            String p = String.format("%.0f", power);
            tips.add(GuiText.LOCO_POWER.toString(p) + ConfigGraphics.powerUnit.toUnitString());
            float force = ConfigGraphics.forceUnit.convertFromNewton(this.getStartingTractionNewtons(gauge));
            String f = String.format("%.0f", force);
            tips.add(GuiText.LOCO_TRACTION.toString(f) + ConfigGraphics.forceUnit.toUnitString());
            float speed = (float) ConfigGraphics.speedUnit.convertFromKmh(this.getMaxSpeed(gauge).metric());
            String v = String.format("%.0f", speed);
            tips.add(GuiText.LOCO_MAX_SPEED.toString(v) + ConfigGraphics.speedUnit.toUnitString());
        }
        return tips;
    }

    public float getHorsePower(Gauge gauge) {
        return (float) (gauge.scale() * this.power_kW * PowerDisplayType.kwToHp);
    }

    public float getWatt(Gauge gauge) {
        return (float) (gauge.scale() * this.power_kW * 1000);
    }

    /**
     * @return tractive effort in newtons
     */
    public float getStartingTractionNewtons(Gauge gauge) {
        return (float) (gauge.scale() * this.traction_N);
    }

    public Speed getMaxSpeed(Gauge gauge) {
        return Speed.fromMinecraft(gauge.scale() * this.maxSpeed.minecraft());
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

    public boolean isCabCar() {
        return isCabCar;
    }

    public boolean isCog() {
        return isCog;
    }

    public double factorOfAdhesion() {
        return this.factorOfAdhesion;
    }
}
