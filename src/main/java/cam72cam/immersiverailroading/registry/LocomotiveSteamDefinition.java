package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.immersiverailroading.gui.overlay.GuiBuilder;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.model.SteamLocomotiveModel;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.mod.resource.Identifier;

import java.io.IOException;
import java.util.List;

public class LocomotiveSteamDefinition extends LocomotiveDefinition {
    public Quilling quill;
    public Identifier whistle;
    public SoundDefinition idle;
    public Identifier chuff;
    public Identifier pressure;
    public Identifier cylinder_drain;
    private FluidQuantity tankCapacity;
    private int maxPSI;
    private int numSlots;
    private int width;
    public boolean tender_auto_feed;
    public boolean cab_forward;

    public LocomotiveSteamDefinition(String defID, DataBlock data) throws Exception {
        super(LocomotiveSteam.class, defID, data);
    }

    @Override
    public void loadData(DataBlock data) throws Exception {
        super.loadData(data);
        DataBlock properties = data.getBlock("properties");
        if (isCabCar()) {
            tankCapacity = FluidQuantity.ZERO;
            maxPSI = 0;
            numSlots = 0;
            width = 0;
            tender_auto_feed = false;
        } else {
            DataBlock firebox = data.getBlock("firebox");

            tankCapacity = FluidQuantity.FromLiters((int) Math.ceil(properties.getValue("water_capacity_l").asInteger() * internal_inv_scale));
            maxPSI = (int) Math.ceil(properties.getValue("max_psi").asInteger() * internal_inv_scale);
            numSlots = (int) Math.ceil(firebox.getValue("slots").asInteger() * internal_inv_scale);
            width = (int) Math.ceil(firebox.getValue("width").asInteger() * internal_inv_scale);
            tender_auto_feed = properties.getValue("tender_auto_feed").asBoolean(true);
        }
        cab_forward = properties.getValue("cab_forward").asBoolean(false);

        //sets default sounds
        whistle = null; //new Identifier(ImmersiveRailroading.MODID, "sounds/steam/default/whistle.ogg");
        idle = new SoundDefinition(new Identifier(ImmersiveRailroading.MODID, "sounds/steam/default/idle.ogg"));
        chuff = new Identifier(ImmersiveRailroading.MODID, "sounds/steam/default/chuff.ogg");
        pressure = new Identifier(ImmersiveRailroading.MODID, "sounds/steam/default/pressure.ogg");
        bell = new SoundDefinition(new Identifier(ImmersiveRailroading.MODID, "sounds/steam/default/bell.ogg"));
        cylinder_drain = new Identifier(ImmersiveRailroading.MODID, "sounds/steam/default/cylinder_drain.ogg");

        DataBlock sounds = data.getBlock("sounds");
        //overrides original sounds with added sounds
        if (sounds != null) {
            whistle = sounds.getValue("whistle").asIdentifier(whistle);
            idle = SoundDefinition.getOrDefault(sounds, "idle", idle);
            chuff = sounds.getValue("chuff").asIdentifier(chuff);
            pressure = sounds.getValue("pressure").asIdentifier(pressure);
            bell = SoundDefinition.getOrDefault(sounds, "bell", bell);
            cylinder_drain = sounds.getValue("cylinder_drain").asIdentifier(cylinder_drain);

            List<DataBlock> quilling = sounds.getBlocks("quilling");
            if (quilling != null) {
                quill = new Quilling(quilling);
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
    protected GuiBuilder getDefaultOverlay(DataBlock data) throws IOException {
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
