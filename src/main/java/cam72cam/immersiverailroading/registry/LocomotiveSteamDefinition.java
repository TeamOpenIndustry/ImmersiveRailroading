package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.library.unit.PressureDisplayType;
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
    public SoundDefinition whistle;
    public SoundDefinition idle;
    public Identifier chuff;
    public Identifier pressure;
    public Identifier cylinder_drain;
    private double tankCapacity_l;
    private double maxPSI;
    private double numSlots;
    private double width;
    public boolean tender_auto_feed;
    public boolean cab_forward;

    public LocomotiveSteamDefinition(String defID, DataBlock data) throws Exception {
        super(LocomotiveSteam.class, defID, data);
    }

    @Override
    protected Identifier defaultDataLocation() {
        return new Identifier(ImmersiveRailroading.MODID, "rolling_stock/default/steam.caml");
    }

    @Override
    public void loadData(DataBlock data) throws Exception {
        super.loadData(data);
        DataBlock properties = data.getBlock("properties");
        if (isCabCar()) {
            tankCapacity_l = 0;
            maxPSI = 0;
            numSlots = 0;
            width = 0;
            tender_auto_feed = false;
        } else {
            DataBlock firebox = data.getBlock("firebox");

            tankCapacity_l = properties.getValue("water_capacity_l").asInteger() * internal_inv_scale;
            if (properties.getValue("max_psi").asFloat() != null) {
                maxPSI = properties.getValue("max_psi").asFloat() * internal_inv_scale;
            } else if (properties.getValue("max_pressure_psi").asFloat() != null) {
                maxPSI = properties.getValue("max_pressure_psi").asFloat() * internal_inv_scale;
            } else if (properties.getValue("max_pressure_bar").asFloat() != null) {
                maxPSI = properties.getValue("max_pressure_bar").asFloat() * PressureDisplayType.BarToPsi * internal_inv_scale;
            } else {
                maxPSI = properties.getValue("max_pressure_kpa").asFloat() * PressureDisplayType.kPaToPsi * internal_inv_scale;
            }
            numSlots = Math.ceil(firebox.getValue("slots").asInteger() * internal_inv_scale);
            width = Math.ceil(firebox.getValue("width").asInteger() * internal_inv_scale);
            tender_auto_feed = properties.getValue("tender_auto_feed").asBoolean(true);
        }
        cab_forward = properties.getValue("cab_forward").asBoolean(false);

        DataBlock sounds = data.getBlock("sounds");
        whistle = SoundDefinition.getOrDefault(sounds, "whistle");
        idle = SoundDefinition.getOrDefault(sounds, "idle");
        chuff = sounds.getValue("chuff").asIdentifier();
        pressure = sounds.getValue("pressure").asIdentifier();
        bell = SoundDefinition.getOrDefault(sounds, "bell");
        cylinder_drain = sounds.getValue("cylinder_drain").asIdentifier();

        List<DataBlock> quilling = sounds.getBlocks("quilling");
        if (quilling != null) {
            quill = new Quilling(quilling);
        }
        if (whistle == null && (quill == null || !quill.canLoad())) {
            quill = new Quilling(new Identifier(ImmersiveRailroading.MODID, "sounds/steam/default/quill.ogg"));
        }
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
                GuiBuilder.parse(new Identifier(ImmersiveRailroading.MODID, "gui/default/cab_car.caml")) :
                GuiBuilder.parse(new Identifier(ImmersiveRailroading.MODID, "gui/default/steam.caml"));
    }

    public FluidQuantity getTankCapacity(Gauge gauge) {
        FluidQuantity cap = FluidQuantity.FromLiters((int) Math.ceil(this.tankCapacity_l * gauge.scale())).min(FluidQuantity.FromBuckets(1));
        return Config.ConfigBalance.RoundStockTankToNearestBucket ? cap.roundBuckets() : cap;
    }

    public float getMaxPSI(Gauge gauge) {
        return (float) (this.maxPSI * gauge.scale());
    }

    public int getInventorySize(Gauge gauge) {
        return (int) Math.ceil(numSlots * gauge.scale());
    }

    public int getInventoryWidth(Gauge gauge) {
        return (int) Math.max(3, Math.ceil(width * gauge.scale()));
    }
}
