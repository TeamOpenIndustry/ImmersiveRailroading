package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.immersiverailroading.gui.overlay.GuiBuilder;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.ValveGearConfig;
import cam72cam.immersiverailroading.model.DieselLocomotiveModel;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.mod.resource.Identifier;

import java.io.IOException;

public class LocomotiveDieselDefinition extends LocomotiveDefinition {
    public SoundDefinition idle;
    public SoundDefinition horn;
    private FluidQuantity fuelCapacity;
    private int fuelEfficiency;
    private boolean hornSus;
    private int notches;

    public LocomotiveDieselDefinition(String defID, DataBlock data) throws Exception {
        super(LocomotiveDiesel.class, defID, data);
    }

    @Override
    protected Identifier defaultDataLocation() {
        return new Identifier(ImmersiveRailroading.MODID, "rolling_stock/default/diesel.caml");
    }

    @Override
    public void loadData(DataBlock data) throws Exception {
        super.loadData(data);

        DataBlock properties = data.getBlock("properties");
        if (!isCabCar()) {
            fuelCapacity = FluidQuantity.FromLiters((int) Math.ceil(properties.getValue("fuel_capacity_l").asInteger() * internal_inv_scale * 10));
            fuelEfficiency = properties.getValue("fuel_efficiency_%").asInteger();
        } else {
            fuelCapacity = FluidQuantity.ZERO;
        }
        notches = properties.getValue("throttle_notches").asInteger(8);

        hornSus = properties.getValue("horn_sustained").asBoolean(false);

        idle = new SoundDefinition(new Identifier(ImmersiveRailroading.MODID, "sounds/diesel/default/idle.ogg"));
        horn = new SoundDefinition(new Identifier(ImmersiveRailroading.MODID, "sounds/diesel/default/horn.ogg"));
        bell = new SoundDefinition(new Identifier(ImmersiveRailroading.MODID, "sounds/diesel/default/bell.ogg"));

        DataBlock sounds = data.getBlock("sounds");
        if(sounds != null) {
            idle = SoundDefinition.getOrDefault(sounds, "idle", idle);
            horn = SoundDefinition.getOrDefault(sounds, "horn", horn);
            bell = SoundDefinition.getOrDefault(sounds, "bell", bell);
        }

        if (controlSounds.isEmpty()) {
            controlSounds.put("REVERSER_1", new ControlSoundsDefinition(
                    new Identifier(ImmersiveRailroading.MODID, "sounds/default/lever_engage.ogg"),
                    new Identifier(ImmersiveRailroading.MODID, "sounds/default/lever_move.ogg"),
                    0.45f,
                    new Identifier(ImmersiveRailroading.MODID, "sounds/default/lever_disengage.ogg")
            ));
            controlSounds.put("THROTTLE_1", new ControlSoundsDefinition(
                    new Identifier(ImmersiveRailroading.MODID, "sounds/default/lever_engage.ogg"),
                    new Identifier(ImmersiveRailroading.MODID, "sounds/default/lever_move.ogg"),
                    1/8f,
                    new Identifier(ImmersiveRailroading.MODID, "sounds/default/lever_disengage.ogg")
            ));
            controlSounds.put("TRAIN_BRAKE_1", new ControlSoundsDefinition(
                    new Identifier(ImmersiveRailroading.MODID, "sounds/default/lever_engage.ogg"),
                    new Identifier(ImmersiveRailroading.MODID, "sounds/default/pressure.ogg"),
                    null,
                    new Identifier(ImmersiveRailroading.MODID, "sounds/default/lever_disengage.ogg")
            ));
            controlSounds.put("INDEPENDENT_BRAKE_1", new ControlSoundsDefinition(
                    new Identifier(ImmersiveRailroading.MODID, "sounds/default/lever_engage.ogg"),
                    new Identifier(ImmersiveRailroading.MODID, "sounds/default/pressure.ogg"),
                    null,
                    new Identifier(ImmersiveRailroading.MODID, "sounds/default/lever_disengage.ogg")
            ));
        }
    }

    @Override
    protected boolean multiUnitDefault() {
        return true;
    }

    @Override
    protected StockModel<?, ?> createModel() throws Exception {
        return new DieselLocomotiveModel(this);
    }

    @Override
    protected GuiBuilder getDefaultOverlay(DataBlock data) throws IOException {
        return readCabCarFlag(data) ?
                GuiBuilder.parse(new Identifier(ImmersiveRailroading.MODID, "gui/default/cab_car.caml")) :
                GuiBuilder.parse(new Identifier(ImmersiveRailroading.MODID, "gui/default/diesel.caml"));
    }

    @Override
    public StockModel<?, ?> getModel() {
        return (DieselLocomotiveModel) super.getModel();
    }

    //checks to see if horn is sustained, on by default
    public boolean getHornSus() {
        return hornSus;
    }

    public FluidQuantity getFuelCapacity(Gauge gauge) {
        return this.fuelCapacity.scale(gauge.scale()).min(FluidQuantity.FromBuckets(1)).roundBuckets();
    }

    public int getFuelEfficiency() {
        return this.fuelEfficiency;
    }


    public ValveGearConfig getValveGear() {
        return super.getValveGear() == null ? new ValveGearConfig(ValveGearConfig.ValveGearType.CONNECTING, null) : super.getValveGear();
    }

    public int getThrottleNotches() {
        return notches;
    }
}
