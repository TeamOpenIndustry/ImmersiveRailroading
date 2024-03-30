package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.entity.LocomotiveElectric;
import cam72cam.immersiverailroading.gui.overlay.GuiBuilder;
import cam72cam.immersiverailroading.library.ValveGearConfig;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.resource.Identifier;
import cam72cam.immersiverailroading.model.ElectricLocomotiveModel;

import java.io.IOException;
import java.util.Map;

public class LocomotiveElectricDefinition extends LocomotiveDefinition {
    public EntityRollingStockDefinition.SoundDefinition idle;
    public EntityRollingStockDefinition.SoundDefinition running;
    public EntityRollingStockDefinition.SoundDefinition horn;
    private int energyCapacity;
    private int energyConsumeRatio;
    private boolean hornSus;
    private int notches;
    private float enginePitchRange;
    public boolean hasDynamicTractionControl;

    public LocomotiveElectricDefinition(String defID, DataBlock data) throws Exception {
        super(LocomotiveElectric.class, defID, data);
    }

    protected Identifier defaultDataLocation() {
        return new Identifier("immersiverailroading", "rolling_stock/default/diesel.caml");
    }

    public void loadData(DataBlock data) throws Exception {
        super.loadData(data);
        DataBlock properties = data.getBlock("properties");
        if (!this.isCabCar()) {
            this.energyCapacity = properties.getValue("energy_capacity_rf").asInteger();
            this.energyConsumeRatio = properties.getValue("energy_consume_ratio_rf/t").asInteger();
            this.hasDynamicTractionControl = properties.getValue("dynamic_traction_control").asBoolean();
        } else {
            this.energyCapacity = 0;
            this.energyConsumeRatio = 0;
        }

        this.notches = properties.getValue("throttle_notches").asInteger();
        this.hornSus = properties.getValue("horn_sustained").asBoolean();
        DataBlock sounds = data.getBlock("sounds");
        this.idle = SoundDefinition.getOrDefault(sounds, "idle");
        this.running = SoundDefinition.getOrDefault(sounds, "running");
        this.enginePitchRange = sounds.getValue("engine_pitch_range").asFloat();
        this.horn = SoundDefinition.getOrDefault(sounds, "horn");
        this.bell = SoundDefinition.getOrDefault(sounds, "bell");
    }

    protected StockModel<?, ?> createModel() throws Exception {
        return new ElectricLocomotiveModel(this);
    }

    protected GuiBuilder getDefaultOverlay(DataBlock data) throws IOException {
        return GuiBuilder.parse(new Identifier("immersiverailroading", "gui/default/electric.caml"));
    }

    public StockModel<?, ?> getModel() {
        return (ElectricLocomotiveModel)super.getModel();
    }

    public boolean getHornSus() {
        return this.hornSus;
    }

    public ValveGearConfig getValveGear() {
        return super.getValveGear() == null ? new ValveGearConfig(ValveGearConfig.ValveGearType.CONNECTING, (Map<Float, Identifier>)null) : super.getValveGear();
    }

    public int getThrottleNotches() {
        return this.notches;
    }

    public float getEnginePitchRange() {
        return this.enginePitchRange;
    }

    public int getEnergyCapacity() {
        return energyCapacity;
    }

    public int getEnergyConsumeRatio() {
        return energyConsumeRatio;
    }
}
