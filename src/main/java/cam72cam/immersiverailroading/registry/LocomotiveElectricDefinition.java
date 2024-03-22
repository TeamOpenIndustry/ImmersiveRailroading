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
    //    private double fuelCapacity_l;
//    private int fuelEfficiency;
    private int powerRequired;
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

    public int getPowerRequired() {
        return powerRequired;
    }

    public void setPowerRequired(int powerRequired) {
        this.powerRequired = powerRequired;
    }

    public void loadData(DataBlock data) throws Exception {
        super.loadData(data);
        DataBlock properties = data.getBlock("properties");
        if (!this.isCabCar()) {
            this.powerRequired = properties.getValue("power").asInteger(2000);
            this.hasDynamicTractionControl = properties.getValue("dynamic_traction_control").asBoolean();
        } else {
            this.powerRequired = 0;
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
        return GuiBuilder.parse(new Identifier("immersiverailroading", "gui/default/cab_car.caml"));
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
}
