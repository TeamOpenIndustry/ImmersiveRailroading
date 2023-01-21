package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.gui.overlay.GuiBuilder;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.model.SteamLocomotiveModel;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.mod.resource.Identifier;
import com.google.gson.JsonObject;

import java.io.IOException;

public class LocomotiveSteamDefinition extends LocomotiveDefinition {
    public Quilling quill;
    public Identifier whistle;
    public Identifier idle;
    public Identifier chuff;
    public Identifier pressure;
    public Identifier cylinder_drain;
    private FluidQuantity tankCapacity;
    private int maxPSI;
    private int numSlots;
    private int width;
    public boolean tender_auto_feed;
    public boolean cab_forward;

    public LocomotiveSteamDefinition(String defID, JsonObject data) throws Exception {
        super(LocomotiveSteam.class, defID, data);
    }

    @Override
    public void parseJson(JsonObject data) throws Exception {
        super.parseJson(data);
        JsonObject properties = data.get("properties").getAsJsonObject();
        if (isCabCar()) {
            tankCapacity = FluidQuantity.ZERO;
            maxPSI = 0;
            numSlots = 0;
            width = 0;
            tender_auto_feed = false;
        } else {
            JsonObject firebox = data.get("firebox").getAsJsonObject();

            tankCapacity = FluidQuantity.FromLiters((int) Math.ceil(properties.get("water_capacity_l").getAsInt() * internal_inv_scale));
            maxPSI = (int) Math.ceil(properties.get("max_psi").getAsInt() * internal_inv_scale);
            numSlots = (int) Math.ceil(firebox.get("slots").getAsInt() * internal_inv_scale);
            width = (int) Math.ceil(firebox.get("width").getAsInt() * internal_inv_scale);
            tender_auto_feed = getOrDefault(properties, "tender_auto_feed", true);
        }
        cab_forward = getOrDefault(properties, "cab_forward", false);

        //sets default sounds
        whistle = null; //new Identifier(ImmersiveRailroading.MODID, "sounds/steam/default/whistle.ogg");
        idle = new Identifier(ImmersiveRailroading.MODID, "sounds/steam/default/idle.ogg");
        chuff = new Identifier(ImmersiveRailroading.MODID, "sounds/steam/default/chuff.ogg");
        pressure = new Identifier(ImmersiveRailroading.MODID, "sounds/steam/default/pressure.ogg");
        bell = new Identifier(ImmersiveRailroading.MODID, "sounds/steam/default/bell.ogg");
        cylinder_drain = new Identifier(ImmersiveRailroading.MODID, "sounds/steam/default/cylinder_drain.ogg");

        JsonObject sounds = data.has("sounds") ? data.get("sounds").getAsJsonObject() : null;
        //overrides original sounds with added sounds
        if (sounds != null) {
            whistle = getOrDefault(sounds, "whistle", whistle);
            idle = getOrDefault(sounds, "idle", idle);
            chuff = getOrDefault(sounds, "chuff", chuff);
            pressure = getOrDefault(sounds, "pressure", pressure);
            bell = getOrDefault(sounds, "bell", bell);
            cylinder_drain = getOrDefault(sounds, "cylinder_drain", cylinder_drain);

            if (sounds.has("quilling")) {
                quill = new Quilling(sounds.get("quilling").getAsJsonArray());
            }
        }
        if (whistle == null && (quill == null || !quill.canLoad())) {
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
    protected boolean multiUnitDefault() {
        return false;
    }

    @Override
    protected StockModel<?, ?> createModel() throws Exception {
        return new SteamLocomotiveModel(this);
    }

    @Override
    public StockModel<?, ?> getModel() {
        return (SteamLocomotiveModel) super.getModel();
    }

    @Override
    protected GuiBuilder getDefaultOverlay(JsonObject data) throws IOException {
        return readCabCarFlag(data) ?
                GuiBuilder.parse(new Identifier(ImmersiveRailroading.MODID, "gui/default/cab_car.json")) :
                GuiBuilder.parse(new Identifier(ImmersiveRailroading.MODID, "gui/default/steam.json"));
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
