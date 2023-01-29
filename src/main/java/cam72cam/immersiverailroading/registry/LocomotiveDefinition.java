package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.gui.overlay.DataBlock;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.model.LocomotiveModel;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.util.Speed;

import java.util.List;

public abstract class LocomotiveDefinition extends FreightDefinition {
    public boolean toggleBell;
    public SoundDefinition bell;
    private String works;
    private int power;
    private int traction;
    private Speed maxSpeed;
    private boolean hasRadioEquipment;
    public boolean muliUnitCapable;
    private boolean isCabCar;
    private boolean isLinkedBrakeThrottle;
    private boolean isCog;

    LocomotiveDefinition(Class<? extends EntityRollingStock> type, String defID, DataBlock data) throws Exception {
        super(type, defID, data);
    }

    @Override
    public void parseJson(DataBlock data) throws Exception {
        super.parseJson(data);

        works = data.getString("works");

        DataBlock properties = data.getBlock("properties");

        hasRadioEquipment = properties.getBoolean("radio_equipped", false);

        isCabCar = readCabCarFlag(data);
        if (isCabCar) {
            power = 0;
            traction = 0;
            maxSpeed = Speed.ZERO;
            muliUnitCapable = true;
        } else {
            power = (int) Math.ceil(properties.getInteger("horsepower") * internal_inv_scale);
            traction = (int) Math.ceil(properties.getInteger("tractive_effort_lbf") * internal_inv_scale);
            maxSpeed = Speed.fromMetric(properties.getFloat("max_speed_kmh") * internal_inv_scale);
            muliUnitCapable = properties.getBoolean("multi_unit_capable", this.multiUnitDefault());
        }
        isLinkedBrakeThrottle = properties.getBoolean("isLinkedBrakeThrottle", false);
        toggleBell = properties.getBoolean("toggle_bell", true);
        isCog = properties.getBoolean("cog", false);
    }

    protected boolean readCabCarFlag(DataBlock data) {
        return data.getBlock("properties").getBoolean("cab_car", false);
    }

    protected abstract boolean multiUnitDefault();

    @Override
    protected StockModel<?, ?> createModel() throws Exception {
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

    public boolean isCog() {
        return isCog;
    }
}
