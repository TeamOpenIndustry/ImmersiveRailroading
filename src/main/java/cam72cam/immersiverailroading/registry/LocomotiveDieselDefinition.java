package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.gui.overlay.GuiBuilder;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.ValveGearConfig;
import cam72cam.immersiverailroading.model.DieselLocomotiveModel;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.mod.resource.Identifier;
import com.google.gson.JsonObject;

import java.io.IOException;

public class LocomotiveDieselDefinition extends LocomotiveDefinition {
    public SoundDefinition idle;
    public SoundDefinition horn;
    private FluidQuantity fuelCapacity;
    private int fuelEfficiency;
    private boolean hornSus;
    private int notches;

    public LocomotiveDieselDefinition(String defID, JsonObject data) throws Exception {
        super(LocomotiveDiesel.class, defID, data);
    }

    @Override
    public void parseJson(JsonObject data) throws Exception {
        super.parseJson(data);

        JsonObject properties = data.get("properties").getAsJsonObject();
        if (!isCabCar()) {
            fuelCapacity = FluidQuantity.FromLiters((int) Math.ceil(properties.get("fuel_capacity_l").getAsInt() * internal_inv_scale * 10));
            fuelEfficiency = properties.get("fuel_efficiency_%").getAsInt();
        } else {
            fuelCapacity = FluidQuantity.ZERO;
        }
        notches = getOrDefault(properties, "throttle_notches", 8);

        hornSus = getOrDefault(properties, "horn_sustained", false);
        JsonObject sounds = data.has("sounds") ? data.get("sounds").getAsJsonObject() : null;

        idle = new SoundDefinition(new Identifier(ImmersiveRailroading.MODID, "sounds/diesel/default/idle.ogg"));
        horn = new SoundDefinition(new Identifier(ImmersiveRailroading.MODID, "sounds/diesel/default/horn.ogg"));
        bell = new SoundDefinition(new Identifier(ImmersiveRailroading.MODID, "sounds/diesel/default/bell.ogg"));

        if(sounds != null) {
            idle = getOrDefault(sounds, "idle", idle);
            horn = getOrDefault(sounds, "horn", horn);
            bell = getOrDefault(sounds, "bell", bell);
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
    protected GuiBuilder getDefaultOverlay(JsonObject data) throws IOException {
        return readCabCarFlag(data) ?
                GuiBuilder.parse(new Identifier(ImmersiveRailroading.MODID, "gui/default/cab_car.json")) :
                GuiBuilder.parse(new Identifier(ImmersiveRailroading.MODID, "gui/default/diesel.json"));
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
        return super.getValveGear() == null ? new ValveGearConfig(ValveGearConfig.ValveGearType.CONNECTING) : super.getValveGear();
    }

    public int getThrottleNotches() {
        return notches;
    }
}
