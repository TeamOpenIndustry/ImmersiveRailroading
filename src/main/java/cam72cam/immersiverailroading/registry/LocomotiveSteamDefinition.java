package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.model.SteamLocomotiveModel;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.mod.resource.Identifier;
import com.google.gson.JsonObject;

public class LocomotiveSteamDefinition extends LocomotiveDefinition {
    private static Identifier default_whistle = new Identifier(ImmersiveRailroading.MODID, "sounds/steam/default/whistle.ogg");
    private static Identifier default_idle = new Identifier(ImmersiveRailroading.MODID, "sounds/steam/default/idle.ogg");
    private static Identifier default_chuff = new Identifier(ImmersiveRailroading.MODID, "sounds/steam/default/chuff.ogg");
    private static Identifier default_pressure = new Identifier(ImmersiveRailroading.MODID, "sounds/steam/default/pressure.ogg");
    private static Identifier default_bell = new Identifier(ImmersiveRailroading.MODID, "sounds/steam/default/bell.ogg");

    public Quilling quill;
    public Identifier whistle;
    public Identifier idle;
    public Identifier chuff;
    public Identifier pressure;
    private FluidQuantity tankCapacity;
    private int maxPSI;
    private int numSlots;
    private int width;
    public boolean tender_auto_feed;
    public boolean cab_forward;

    public LocomotiveSteamDefinition(String defID, JsonObject data) throws Exception {
        super(LocomotiveSteam.class, defID, data);

        // Handle null data
        if (tankCapacity == null) {
            tankCapacity = FluidQuantity.ZERO;
        }
    }

    @Override
    public void parseJson(JsonObject data) throws Exception {
        super.parseJson(data);
        JsonObject properties = data.get("properties").getAsJsonObject();
        tankCapacity = FluidQuantity.FromLiters((int) Math.ceil(properties.get("water_capacity_l").getAsInt() * internal_inv_scale));
        maxPSI = (int) Math.ceil(properties.get("max_psi").getAsInt() * internal_inv_scale);
        JsonObject firebox = data.get("firebox").getAsJsonObject();
        this.numSlots = (int) Math.ceil(firebox.get("slots").getAsInt() * internal_inv_scale);
        this.width = (int) Math.ceil(firebox.get("width").getAsInt() * internal_inv_scale);
        this.tender_auto_feed = properties.has("tender_auto_feed") ? properties.get("tender_auto_feed").getAsBoolean() : true;
        this.cab_forward = properties.has("cab_forward") && properties.get("cab_forward").getAsBoolean();

        JsonObject sounds = data.has("sounds") ? data.get("sounds").getAsJsonObject() : null;

        //sets default sounds
        whistle = default_whistle;
        idle = default_idle;
        chuff = default_chuff;
        pressure = default_pressure;
        bell = default_bell;

        boolean whistleSet = false;

        //overrides original sounds with added sounds
        if (sounds != null) {
            if (sounds.has("whistle")) {
                whistle = new Identifier(ImmersiveRailroading.MODID, sounds.get("whistle").getAsString()).getOrDefault(default_whistle);
                whistleSet = true;
            }

            if (sounds.has("idle")) {
                idle = new Identifier(ImmersiveRailroading.MODID, sounds.get("idle").getAsString()).getOrDefault(default_idle);
            }

            if (sounds.has("chuff")) {
                chuff = new Identifier(ImmersiveRailroading.MODID, sounds.get("chuff").getAsString()).getOrDefault(default_chuff);
            }

            if (sounds.has("pressure")) {
                pressure = new Identifier(ImmersiveRailroading.MODID, sounds.get("pressure").getAsString()).getOrDefault(default_pressure);
            }

            if (sounds.has("bell")) {
                bell = new Identifier(ImmersiveRailroading.MODID, sounds.get("bell").getAsString()).getOrDefault(default_bell);
            }

            if (sounds.has("quilling")) {
                quill = new Quilling(sounds.get("quilling").getAsJsonArray());
                whistleSet = true;
            }
        }
        if (!whistleSet && (quill == null || !quill.canLoad())) {
            quill = new Quilling(new Identifier(ImmersiveRailroading.MODID, "sounds/steam/default/quill.ogg"));
        }

        if (controlSounds.isEmpty()) {
            controlSounds.put("REVERSER_1", new ControlSoundsDefinition(
                    new Identifier(ImmersiveRailroading.MODID, "sounds/default/lever_engage.ogg"),
                    new Identifier(ImmersiveRailroading.MODID, "sounds/default/lever_move.ogg"),
                    0.1f,
                    new Identifier(ImmersiveRailroading.MODID, "sounds/default/lever_disengage.ogg")
            ));
            controlSounds.put("THROTTLE_1", new ControlSoundsDefinition(
                    new Identifier(ImmersiveRailroading.MODID, "sounds/default/lever_engage.ogg"),
                    null,
                    null,
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
    protected StockModel<?> createModel() throws Exception {
        return new SteamLocomotiveModel(this);
    }

    @Override
    public SteamLocomotiveModel getModel() {
        return (SteamLocomotiveModel) super.getModel();
    }

    public FluidQuantity getTankCapacity(Gauge gauge) {
        return this.tankCapacity.scale(gauge.scale()).min(FluidQuantity.FromBuckets(1)).roundBuckets();
    }

    public int getMaxPSI(Gauge gauge) {
        return (int) Math.ceil(this.maxPSI * gauge.scale());
    }

    public int getInventorySize(Gauge gauge) {
        return (int) Math.ceil(numSlots * gauge.scale());
    }

    public int getInventoryWidth(Gauge gauge) {
        return (int) Math.max(3, Math.ceil(width * gauge.scale()));
    }
}
