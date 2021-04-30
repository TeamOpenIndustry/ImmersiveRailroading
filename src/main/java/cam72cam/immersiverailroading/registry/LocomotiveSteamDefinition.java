package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.library.ValveGearType;
import cam72cam.immersiverailroading.model.RenderComponent;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.mod.resource.Identifier;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    public Identifier bell;
    private FluidQuantity tankCapacity;
    private int maxPSI;
    private ValveGearType valveGear;
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
        valveGear = ValveGearType.valueOf(properties.get("valve_gear").getAsString().toUpperCase());
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
    }

    @Override
    protected boolean unifiedBogies() {
        return false;
    }

    @Override
    protected Set<String> parseComponents() {
        Set<String> groups = super.parseComponents();

        switch (this.valveGear) {
            case STEPHENSON:
            case WALSCHAERTS:
            case TRI_WALSCHAERTS:
            case HIDDEN:
                for (int i = 100; i >= 0; i--) {
                    addComponentIfExists(RenderComponent.parseID(RenderComponentType.WHEEL_DRIVER_X, this, groups, i), true);
                }
                break;
            case T1:
            case GARRAT:
            case MALLET_WALSCHAERTS:
                for (int i = 100; i >= 0; i--) {
                    addComponentIfExists(RenderComponent.parseID(RenderComponentType.WHEEL_DRIVER_FRONT_X, this, groups, i), true);
                    addComponentIfExists(RenderComponent.parseID(RenderComponentType.WHEEL_DRIVER_REAR_X, this, groups, i), true);
                }
                addComponentIfExists(RenderComponent.parse(RenderComponentType.FRONT_LOCOMOTIVE, this, groups), true);
                break;
            case CLIMAX:
                break;
            case SHAY:
                break;
        }


        for (int i = 100; i >= 0; i--) {
            addComponentIfExists(RenderComponent.parseID(RenderComponentType.BOILER_SEGMENT_X, this, groups, i), true);
        }

        for (int i = 100; i >= 0; i--) {
            addComponentIfExists(RenderComponent.parseID(RenderComponentType.PARTICLE_CHIMNEY_X, this, groups, i), false);
            addComponentIfExists(RenderComponent.parseID(RenderComponentType.PRESSURE_VALVE_X, this, groups, i), false);
        }

        addComponentIfExists(RenderComponent.parse(RenderComponentType.FIREBOX, this, groups), true);
        addComponentIfExists(RenderComponent.parse(RenderComponentType.SMOKEBOX, this, groups), true);
        addComponentIfExists(RenderComponent.parse(RenderComponentType.STEAM_CHEST_FRONT, this, groups), true);
        addComponentIfExists(RenderComponent.parse(RenderComponentType.STEAM_CHEST_REAR, this, groups), true);
        addComponentIfExists(RenderComponent.parse(RenderComponentType.STEAM_CHEST, this, groups), true);
        addComponentIfExists(RenderComponent.parse(RenderComponentType.PIPING, this, groups), true);


        List<String> sides = new ArrayList<>();

        switch (this.valveGear) {
            case TRI_WALSCHAERTS:
                sides.add("CENTER");
            case STEPHENSON:
            case WALSCHAERTS:
                sides.add("RIGHT");
                sides.add("LEFT");
            case T1:
            case GARRAT:
            case MALLET_WALSCHAERTS:
                if (sides.size() == 0) {
                    sides.add("LEFT_FRONT");
                    sides.add("RIGHT_FRONT");
                    sides.add("LEFT_REAR");
                    sides.add("RIGHT_REAR");
                }

                RenderComponentType[] components = new RenderComponentType[]{
                        RenderComponentType.SIDE_ROD_SIDE,
                        RenderComponentType.MAIN_ROD_SIDE,
                        RenderComponentType.PISTON_ROD_SIDE,
                        RenderComponentType.CYLINDER_SIDE,

                        RenderComponentType.UNION_LINK_SIDE,
                        RenderComponentType.COMBINATION_LEVER_SIDE,
                        RenderComponentType.VALVE_STEM_SIDE,
                        RenderComponentType.RADIUS_BAR_SIDE,
                        RenderComponentType.EXPANSION_LINK_SIDE,
                        RenderComponentType.ECCENTRIC_ROD_SIDE,
                        RenderComponentType.ECCENTRIC_CRANK_SIDE,
                        RenderComponentType.REVERSING_ARM_SIDE,
                        RenderComponentType.LIFTING_LINK_SIDE,
                        RenderComponentType.REACH_ROD_SIDE,
                };

                for (String side : sides) {
                    for (RenderComponentType name : components) {
                        addComponentIfExists(RenderComponent.parseSide(name, this, groups, side), true);
                    }
                }
            case CLIMAX:
                break;
            case SHAY:
                break;
            case HIDDEN:
                break;
        }

        return groups;
    }

    public FluidQuantity getTankCapacity(Gauge gauge) {
        return this.tankCapacity.scale(gauge.scale()).min(FluidQuantity.FromBuckets(1)).roundBuckets();
    }

    public int getMaxPSI(Gauge gauge) {
        return (int) Math.ceil(this.maxPSI * gauge.scale());
    }

    public ValveGearType getValveGear() {
        return valveGear;
    }

    public int getInventorySize(Gauge gauge) {
        return (int) Math.ceil(numSlots * gauge.scale());
    }

    public int getInventoryWidth(Gauge gauge) {
        return (int) Math.max(3, Math.ceil(width * gauge.scale()));
    }
}
