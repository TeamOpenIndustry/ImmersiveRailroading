package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.ValveGearType;
import cam72cam.immersiverailroading.model.DieselLocomotiveModel;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.mod.resource.Identifier;
import com.google.gson.JsonObject;

public class LocomotiveDieselDefinition extends LocomotiveDefinition {
    private static Identifier default_idle = new Identifier(ImmersiveRailroading.MODID, "sounds/diesel/default/idle.ogg");
    private static Identifier default_horn = new Identifier(ImmersiveRailroading.MODID, "sounds/diesel/default/horn.ogg");
    private static Identifier default_bell = new Identifier(ImmersiveRailroading.MODID, "sounds/diesel/default/bell.ogg");

    public Identifier idle;
    public Identifier horn;
    public boolean muliUnitCapable;
    private FluidQuantity fuelCapacity;
    private int fuelEfficiency;
    private boolean hornSus;

    public LocomotiveDieselDefinition(String defID, JsonObject data) throws Exception {
        super(LocomotiveDiesel.class, defID, data);

        // Handle null data
        if (fuelCapacity == null) {
            fuelCapacity = FluidQuantity.ZERO;
        }
    }

    @Override
    public void parseJson(JsonObject data) throws Exception {
        super.parseJson(data);

        JsonObject properties = data.get("properties").getAsJsonObject();
        fuelCapacity = FluidQuantity.FromLiters((int) Math.ceil(properties.get("fuel_capacity_l").getAsInt() * internal_inv_scale * 10));
        fuelEfficiency = properties.get("fuel_efficiency_%").getAsInt();
        muliUnitCapable = !properties.has("multi_unit_capable") || properties.get("multi_unit_capable").getAsBoolean();

        hornSus = false;
        if (properties.has("horn_sustained")) {
            hornSus = properties.get("horn_sustained").getAsBoolean();
        }
        JsonObject sounds = data.has("sounds") ? data.get("sounds").getAsJsonObject() : null;

        idle = default_idle;
        horn = default_horn;
        bell = default_bell;

        if(sounds != null){
            if (sounds.has("idle")) {
                idle = new Identifier(ImmersiveRailroading.MODID, sounds.get("idle").getAsString()).getOrDefault(default_idle);
            }

            if (sounds.has("horn")) {
                horn = new Identifier(ImmersiveRailroading.MODID, sounds.get("horn").getAsString()).getOrDefault(default_horn);
            }

            if (sounds.has("bell")) {
                bell = new Identifier(ImmersiveRailroading.MODID, sounds.get("bell").getAsString()).getOrDefault(default_bell);
            }
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
            controlSounds.put("BRAKE_1", new ControlSoundsDefinition(
                    new Identifier(ImmersiveRailroading.MODID, "sounds/default/lever_engage.ogg"),
                    null,
                    null,
                    new Identifier(ImmersiveRailroading.MODID, "sounds/default/lever_disengage.ogg")
            ));
        }
    }

    @Override
    protected StockModel<?> createModel() throws Exception {
        return new DieselLocomotiveModel(this);
    }

    @Override
    public DieselLocomotiveModel getModel() {
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


    public ValveGearType getValveGear() {
        return super.getValveGear() == null ? ValveGearType.CONNECTING : super.getValveGear();
    }
}
