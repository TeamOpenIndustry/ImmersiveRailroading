package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityBuildableRollingStock;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.gui.overlay.GuiBuilder;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.model.animation.Animatrix;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.util.RealBB;
import cam72cam.mod.entity.EntityRegistry;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.ResourceCache;
import cam72cam.mod.serialization.ResourceCache.GenericByteBuffer;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapped;
import cam72cam.mod.text.TextUtil;
import cam72cam.mod.world.World;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import util.Matrix4;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@TagMapped(EntityRollingStockDefinition.TagMapper.class)
public abstract class EntityRollingStockDefinition {
    private static final Identifier DEFAULT_PARTICLE_TEXTURE = new Identifier(ImmersiveRailroading.MODID, "textures/light.png");

    public final String defID;
    private final Class<? extends EntityRollingStock> type;
    public final List<String> itemGroups;
    public Map<String, String> textureNames;
    public float dampeningAmount;
    public Gauge recommended_gauge;
    public Boolean shouldSit;
    public Identifier wheel_sound;
    public Identifier clackFront;
    public Identifier clackRear;
    public double internal_model_scale;
    public Identifier couple_sound;
    double internal_inv_scale;
    private String name;
    private String modelerName;
    private String packName;
    private ValveGearConfig valveGear;
    public float darken;
    public Identifier modelLoc;
    protected StockModel<?, ?> model;
    private Vec3d passengerCenter;
    private float bogeyFront;
    private float bogeyRear;
    private float couplerOffsetFront;
    private float couplerOffsetRear;
    private float couplerSlackFront;
    private float couplerSlackRear;
    private boolean scalePitch;
    private double frontBounds;
    private double rearBounds;
    private double heightBounds;
    private double widthBounds;
    private double passengerCompartmentLength;
    private double passengerCompartmentWidth;
    private int weight;
    private int maxPassengers;
    private float interiorLightLevel;
    private boolean hasIndependentBrake;
    private final Map<ModelComponentType, List<ModelComponent>> renderComponents;
    private final List<ItemComponentType> itemComponents;
    private final Function<EntityBuildableRollingStock, float[][]> heightmap;
    private final Map<String, LightDefinition> lights = new HashMap<>();
    protected final Map<String, ControlSoundsDefinition> controlSounds = new HashMap<>();
    public Identifier smokeParticleTexture;
    public Identifier steamParticleTexture;
    private boolean isLinearBrakeControl;
    private GuiBuilder overlay;
    private List<String> extraTooltipInfo;
    private boolean hasInternalLighting;
    private double swayMultiplier;
    private double tiltMultiplier;
    private float brakeCoefficient;

    public List<AnimationDefinition> animations;

    public static class AnimationDefinition {

        public enum AnimationMode {
            VALUE,
            PLAY_FORWARD,
            PLAY_REVERSE,
            PLAY_BOTH,
            LOOP,
            LOOP_SPEED
        }
        public final String control_group;
        public final AnimationMode mode;
        public final Readouts readout;
        public final Animatrix animatrix;
        public final float offset;
        public final boolean invert;
        public final float frames_per_tick;

        private final Map<UUID, Integer> tickStart;
        private final Map<UUID, Integer> tickStop;
        private final boolean looping;

        public AnimationDefinition(JsonObject obj, double internal_model_scale) throws IOException {
            control_group = getOrDefault(obj, "control_group", (String)null);
            readout = obj.has("readout") ? Readouts.valueOf(obj.get("readout").getAsString().toUpperCase(Locale.ROOT)) : null;
            if (control_group == null && readout == null) {
                throw new IllegalArgumentException("Must specify either a control group or a readout for an animation");
            }
            Identifier animatrixID = getOrDefault(obj, "animatrix", (Identifier) null);
            animatrix = animatrixID != null ? new Animatrix(animatrixID.getResourceStream(), internal_model_scale) : null;
            mode = AnimationMode.valueOf(obj.get("mode").getAsString().toUpperCase(Locale.ROOT));
            offset = getOrDefault(obj, "offset", 0f);
            invert = getOrDefault(obj, "invert", false);
            frames_per_tick = getOrDefault(obj, "frames_per_tick", 1f);

            tickStart = new HashMap<>();
            tickStop = new HashMap<>();
            switch (mode) {
                case VALUE:
                case PLAY_FORWARD:
                case PLAY_REVERSE:
                case PLAY_BOTH:
                    looping = false;
                    break;
                case LOOP:
                case LOOP_SPEED:
                default:
                    looping = true;
            }
        }

        public boolean valid() {
            return animatrix != null && (control_group != null || readout != null);
        }

        public float getPercent(EntityRollingStock stock) {
            float value = control_group != null ? stock.getControlPosition(control_group) : readout.getValue(stock);
            value += offset;
            if (invert) {
                value = 1-value;
            }

            float total_ticks_per_loop = animatrix.frameCount() / frames_per_tick;
            if (mode == AnimationMode.LOOP_SPEED) {
                total_ticks_per_loop /= value;
            }

            switch (mode) {
                case VALUE:
                    return value;
                case PLAY_FORWARD:
                case PLAY_REVERSE:
                case PLAY_BOTH:
                    UUID key = stock.getUUID();
                    float tickDelta;
                    if (value >= 0.95) {
                        // FORWARD
                        if (!tickStart.containsKey(key)) {
                            tickStart.put(key, stock.getTickCount());
                            tickStop.remove(key);
                        }
                        if (mode == AnimationMode.PLAY_REVERSE) {
                            return 1;
                        }
                        // 0 -> 1+
                        tickDelta = stock.getTickCount() - tickStart.get(key);
                    } else {
                        // REVERSE
                        if (!tickStop.containsKey(key)) {
                            tickStop.put(key, stock.getTickCount());
                            tickStart.remove(key);
                        }
                        if (mode == AnimationMode.PLAY_FORWARD) {
                            return 0;
                        }
                        // 1 -> 0-
                        tickDelta = total_ticks_per_loop - (stock.getTickCount() - tickStop.get(key));
                    }
                    // Clipped in getMatrix
                    return tickDelta / total_ticks_per_loop;
                case LOOP:
                    if (value < 0.95) {
                        return 0;
                    }
                    break;
                case LOOP_SPEED:
                    if (value == 0) {
                        return 0;
                    }
                    break;
            }

            return (stock.getTickCount() % total_ticks_per_loop) / total_ticks_per_loop;
        }

        public Matrix4 getMatrix(EntityRollingStock stock, String group) {
            return animatrix.getMatrix(group, getPercent(stock), looping);
        }
    }

    public static class LightDefinition {
        public static final Identifier default_light_tex = new Identifier(ImmersiveRailroading.MODID, "textures/light.png");

        public final float blinkIntervalSeconds;
        public final float blinkOffsetSeconds;
        public final boolean blinkFullBright;
        public final String reverseColor;
        public final Identifier lightTex;
        public final boolean castsLight;

        private LightDefinition(JsonObject data) {
            blinkIntervalSeconds = getOrDefault(data, "blinkIntervalSeconds", 0f);
            blinkOffsetSeconds = getOrDefault(data, "blinkOffsetSeconds", 0f);
            blinkFullBright = getOrDefault(data, "blinkFullBright", true);
            reverseColor = getOrDefault(data, "reverseColor", (String)null);
            lightTex = getOrDefault(data, "texture", default_light_tex);
            castsLight = getOrDefault(data, "castsLight", true);
        }
    }

    public static class ControlSoundsDefinition {
        public final Identifier engage;
        public final Identifier move;
        public final Float movePercent;
        public final Identifier disengage;

        protected ControlSoundsDefinition(Identifier engage, Identifier move, Float movePercent, Identifier disengage) {
            this.engage = engage;
            this.move = move;
            this.movePercent = movePercent;
            this.disengage = disengage;
        }

        private ControlSoundsDefinition(JsonObject data) {
            engage = getOrDefault(data, "engage", (Identifier) null);
            move = getOrDefault(data, "move", (Identifier) null);
            movePercent = getOrDefault(data, "movePercent", (Float) null);
            disengage = getOrDefault(data, "disengage", (Identifier) null);
        }
    }

    public EntityRollingStockDefinition(Class<? extends EntityRollingStock> type, String defID, JsonObject data) throws Exception {
        this.type = type;
        this.defID = defID;


        parseJson(data);

        this.model = createModel();
        this.itemGroups = model.groups.keySet().stream().filter(x -> !ModelComponentType.shouldRender(x)).collect(Collectors.toList());

        this.renderComponents = new HashMap<>();
        for (ModelComponent component : model.allComponents) {
            renderComponents.computeIfAbsent(component.type, v -> new ArrayList<>())
                    .add(0, component);
        }

        itemComponents = model.allComponents.stream()
                .map(component -> component.type)
                .map(ItemComponentType::from)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        frontBounds = -model.minOfGroup(model.groups()).x;
        rearBounds = model.maxOfGroup(model.groups()).x;
        widthBounds = model.widthOfGroups(model.groups());

        // Bad hack for height bounds
        ArrayList<String> heightGroups = new ArrayList<>();
        for (String group : model.groups()) {
            boolean ignore = false;
            for (ModelComponentType rct : ModelComponentType.values()) {
                if (rct.collisionsEnabled) {
                    continue;
                }
                for (int i = 0; i < 10; i++) {
                    if (Pattern.matches(rct.regex.replace("#ID#", "" + i), group)) {
                        ignore = true;
                        break;
                    }
                }
                if (ignore) {
                    break;
                }
            }
            if (!ignore) {
                heightGroups.add(group);
            }
        }
        heightBounds = model.heightOfGroups(heightGroups);

        this.heightmap = initHeightmap();
    }

    public final EntityRollingStock spawn(World world, Vec3d pos, float yaw, Gauge gauge, String texture) {
        EntityRollingStock stock = (EntityRollingStock) EntityRegistry.create(world, type);
        stock.setPosition(pos);
        stock.setRotationYaw(yaw);
        // Override prev
        stock.setRotationYaw(yaw);
        stock.setup(defID, gauge, texture);

        return stock;
    }

    public boolean shouldScalePitch() {
        return ConfigSound.scaleSoundToGauge && scalePitch;
    }

    protected static String getOrDefault(JsonObject data, String field, String fallback) {
        return data.has(field) ? data.get(field).getAsString() : fallback;
    }
    protected static boolean getOrDefault(JsonObject data, String field, boolean fallback) {
        return data.has(field) ? data.get(field).getAsBoolean() : fallback;
    }
    protected static Boolean getOrDefault(JsonObject data, String field, Boolean fallback) {
        return data.has(field) ? (Boolean) data.get(field).getAsBoolean() : fallback;
    }
    protected static int getOrDefault(JsonObject data, String field, int fallback) {
        return data.has(field) ? data.get(field).getAsInt() : fallback;
    }
    protected static Integer getOrDefault(JsonObject data, String field, Integer fallback) {
        return data.has(field) ? (Integer) data.get(field).getAsInt() : fallback;
    }
    protected static float getOrDefault(JsonObject data, String field, float fallback) {
        return data.has(field) ? data.get(field).getAsFloat() : fallback;
    }
    protected static Float getOrDefault(JsonObject data, String field, Float fallback) {
        return data.has(field) ? (Float) data.get(field).getAsFloat() : fallback;
    }
    protected static double getOrDefault(JsonObject data, String field, double fallback) {
        return data.has(field) ? data.get(field).getAsDouble() : fallback;
    }
    protected static Identifier getOrDefault(JsonObject data, String field, Identifier fallback) {
        if (data.has(field)) {
            Identifier found = new Identifier(data.get(field).getAsString());
            // Force IR modid
            return new Identifier(ImmersiveRailroading.MODID, found.getPath());
        }
        return fallback;
    }


    public void parseJson(JsonObject data) throws Exception {
        name = data.get("name").getAsString();
        modelerName = getOrDefault(data, "modeler", "N/A");
        packName = getOrDefault(data, "pack", "N/A");
        darken = getOrDefault(data, "darken_model", 0f);
        internal_model_scale = 1;
        internal_inv_scale = 1;
        // TODO Gauge.from(Gauge.STANDARD).value() what happens when != Gauge.STANDARD
        this.recommended_gauge = Gauge.from(Gauge.STANDARD);
        if (data.has("model_gauge_m")) {
            this.recommended_gauge = Gauge.from(data.get("model_gauge_m").getAsDouble());
            internal_model_scale = Gauge.STANDARD / data.get("model_gauge_m").getAsDouble();
        }
        if (data.has("recommended_gauge_m")) {
            this.recommended_gauge = Gauge.from(data.get("recommended_gauge_m").getAsDouble());
        }
        if (this.recommended_gauge != Gauge.from(Gauge.STANDARD)) {
            internal_inv_scale = Gauge.STANDARD / recommended_gauge.value();
        }

        textureNames = new LinkedHashMap<>();
        textureNames.put("", "Default");
        if (data.has("tex_variants")) {
            JsonElement variants = data.get("tex_variants");
            for (Entry<String, JsonElement> variant : variants.getAsJsonObject().entrySet()) {
                textureNames.put(variant.getValue().getAsString(), variant.getKey());
            }
        }

        Identifier alt_textures = new Identifier(ImmersiveRailroading.MODID, defID.replace(".json", "_variants.json"));
        try {
            List<InputStream> alts = alt_textures.getResourceStreamAll();
            for (InputStream input : alts) {
                JsonParser parser = new JsonParser();
                JsonElement variants = parser.parse(new InputStreamReader(input));
                for (Entry<String, JsonElement> variant : variants.getAsJsonObject().entrySet()) {
                    textureNames.put(variant.getValue().getAsString(), variant.getKey());
                }
            }
        } catch (java.io.FileNotFoundException ex) {
            //ignore
        }

        modelLoc = new Identifier(data.get("model").getAsString());

        JsonObject passenger = data.get("passenger").getAsJsonObject();
        passengerCenter = new Vec3d(0, passenger.get("center_y").getAsDouble() - 0.35, passenger.get("center_x").getAsDouble()).scale(internal_model_scale);
        passengerCompartmentLength = passenger.get("length").getAsDouble() * internal_model_scale;
        passengerCompartmentWidth = passenger.get("width").getAsDouble() * internal_model_scale;
        maxPassengers = passenger.get("slots").getAsInt();
        shouldSit = getOrDefault(passenger, "should_sit", (Boolean) null);

        bogeyFront = data.get("trucks").getAsJsonObject().get("front").getAsFloat() * (float) internal_model_scale;
        bogeyRear = data.get("trucks").getAsJsonObject().get("rear").getAsFloat() * (float) internal_model_scale;

        dampeningAmount = getOrDefault(data, "sound_dampening_percentage", 0.75f);
        if (dampeningAmount < 0 || dampeningAmount > 1) {
            dampeningAmount = 0.75f;
        }
        scalePitch = getOrDefault(data, "scale_pitch", true);

        couplerSlackFront = couplerSlackRear = 0.025f;

        if (data.has("couplers")) {
            JsonObject couplers = data.get("couplers").getAsJsonObject();
            couplerOffsetFront = getOrDefault(couplers, "front_offset", 0f) * (float) internal_model_scale;
            couplerOffsetRear = getOrDefault(couplers, "rear_offset", 0f) * (float) internal_model_scale;
            couplerSlackFront = getOrDefault(couplers, "front_slack", couplerSlackFront) * (float) internal_model_scale;
            couplerSlackRear = getOrDefault(couplers, "rear_slack", couplerSlackRear) * (float) internal_model_scale;
        }

        JsonObject properties = data.get("properties").getAsJsonObject();
        weight = (int) Math.ceil(properties.get("weight_kg").getAsInt() * internal_inv_scale);
        valveGear = properties.has("valve_gear") ? new ValveGearConfig(properties.get("valve_gear").getAsString()) : null;
        hasIndependentBrake = getOrDefault(properties, "independent_brake", independentBrakeDefault());
        // Locomotives default to linear brake control
        isLinearBrakeControl = getOrDefault(properties, "linear_brake_control", !(this instanceof LocomotiveDefinition));

        if (data.has("lights")) {
            for (Entry<String, JsonElement> entry : data.get("lights").getAsJsonObject().entrySet()) {
                lights.put(entry.getKey(), new LightDefinition(entry.getValue().getAsJsonObject()));
            }
        }
        interiorLightLevel = getOrDefault(properties, "interior_light_level", 6 / 15f);
        hasInternalLighting = getOrDefault(properties, "internalLighting", this instanceof CarPassengerDefinition);
        swayMultiplier = getOrDefault(properties, "swayMultiplier", 1d);
        tiltMultiplier = getOrDefault(properties, "tiltMultiplier", 0d);

        brakeCoefficient = PhysicalMaterials.STEEL.kineticFriction(PhysicalMaterials.CAST_IRON);
        try {
            brakeCoefficient = PhysicalMaterials.STEEL.kineticFriction(PhysicalMaterials.valueOf(getOrDefault(properties, "brake_shoe_material", "CAST_IRON")));
        } catch (Exception ex) {
            ImmersiveRailroading.warn("Invalid brake_shoe_material, possible values are: %s", Arrays.toString(PhysicalMaterials.values()));
        }
        brakeCoefficient = getOrDefault(properties, "brake_friction_coefficient", brakeCoefficient);

        wheel_sound = new Identifier(ImmersiveRailroading.MODID, "sounds/default/track_wheels.ogg");
        clackFront = clackRear = new Identifier(ImmersiveRailroading.MODID, "sounds/default/clack.ogg");
        couple_sound = new Identifier(ImmersiveRailroading.MODID, "sounds/default/coupling.ogg");

        JsonObject sounds = data.has("sounds") ? data.get("sounds").getAsJsonObject() : null;
        if (sounds != null) {
            wheel_sound = getOrDefault(sounds, "wheels", wheel_sound);
            clackFront = clackRear = getOrDefault(sounds, "clack", clackFront);
            clackFront = getOrDefault(sounds, "clack_front", clackFront);
            clackRear = getOrDefault(sounds, "clack_rear", clackRear);
            couple_sound = getOrDefault(sounds, "couple", couple_sound);
            if (sounds.has("controls")) {
                for (Entry<String, JsonElement> entry : sounds.get("controls").getAsJsonObject().entrySet()) {
                    controlSounds.put(entry.getKey(), new ControlSoundsDefinition(entry.getValue().getAsJsonObject()));
                }
            }
        }

        overlay = data.has("overlay") ? GuiBuilder.parse(new Identifier(data.get("overlay").getAsString())) : getDefaultOverlay(data);
        if (data.has("extra_tooltip_info")) {
            extraTooltipInfo = new ArrayList<>();
            data.getAsJsonArray("extra_tooltip_info").forEach(jsonElement -> extraTooltipInfo.add(jsonElement.getAsString()));
        } else {
            extraTooltipInfo = Collections.emptyList();
        }

        smokeParticleTexture = steamParticleTexture = DEFAULT_PARTICLE_TEXTURE;
        if (data.has("particles")) {
            JsonObject particles = data.get("particles").getAsJsonObject();
            if (particles.has("smoke")) {
                smokeParticleTexture = new Identifier(particles.get("smoke").getAsJsonObject().get("texture").getAsString());
            }
            if (particles.has("steam")) {
                steamParticleTexture = new Identifier(particles.get("steam").getAsJsonObject().get("texture").getAsString());
            }
        }

        animations = new ArrayList<>();
        if (data.has("animations")) {
            JsonArray aobj = data.getAsJsonArray("animations");
            for (JsonElement entry : aobj) {
                animations.add(new AnimationDefinition(entry.getAsJsonObject(), internal_model_scale));
            }
        }
    }

    public List<ModelComponent> getComponents(ModelComponentType name) {
        if (!renderComponents.containsKey(name)) {
            return null;
        }
        return renderComponents.get(name);
    }

    public Vec3d correctPassengerBounds(Gauge gauge, Vec3d pos, boolean shouldSit) {
        double gs = gauge.scale();
        Vec3d passengerCenter = this.passengerCenter.scale(gs);
        pos = pos.subtract(passengerCenter);
        if (pos.z > this.passengerCompartmentLength * gs) {
            pos = new Vec3d(pos.x, pos.y, this.passengerCompartmentLength * gs);
        }

        if (pos.z < -this.passengerCompartmentLength * gs) {
            pos = new Vec3d(pos.x, pos.y, -this.passengerCompartmentLength * gs);
        }

        if (Math.abs(pos.x) > this.passengerCompartmentWidth / 2 * gs) {
            pos = new Vec3d(Math.copySign(this.passengerCompartmentWidth / 2 * gs, pos.x), pos.y, pos.z);
        }

        pos = new Vec3d(pos.x, passengerCenter.y - (shouldSit ? 0.75 : 0), pos.z + passengerCenter.z);

        return pos;
    }

    public boolean isAtFront(Gauge gauge, Vec3d pos) {
        pos = pos.subtract(passengerCenter.scale(gauge.scale()));
        return pos.z >= this.passengerCompartmentLength * gauge.scale();
    }

    public boolean isAtRear(Gauge gauge, Vec3d pos) {
        pos = pos.subtract(passengerCenter.scale(gauge.scale()));
        return pos.z <= -this.passengerCompartmentLength * gauge.scale();
    }

    public List<ItemComponentType> getItemComponents() {
        return itemComponents;
    }

    public float getBogeyFront(Gauge gauge) {
        return (float) gauge.scale() * this.bogeyFront;
    }

    public float getBogeyRear(Gauge gauge) {
        return (float) gauge.scale() * this.bogeyRear;
    }

    public double getCouplerPosition(CouplerType coupler, Gauge gauge) {
        switch (coupler) {
            default:
            case FRONT:
                return gauge.scale() * (this.frontBounds + couplerOffsetFront);
            case BACK:
                return gauge.scale() * (this.rearBounds + couplerOffsetRear);
        }
    }

    public double getCouplerSlack(CouplerType coupler, Gauge gauge) {
        switch (coupler) {
            default:
            case FRONT:
                return gauge.scale() * (this.couplerSlackFront);
            case BACK:
                return gauge.scale() * (this.couplerSlackRear);
        }
    }


    public boolean hasIndependentBrake() {
        return hasIndependentBrake;
    }

    private static class HeightMapData {
        final int xRes;
        final int zRes;
        final List<ModelComponent> components;
        final float[] data;

        HeightMapData(EntityRollingStockDefinition def) {
            ImmersiveRailroading.info("Generating heightmap %s", def.defID);

            double ratio = 8;
            int precision = (int) Math.ceil(def.heightBounds * 4);

            xRes = (int) Math.ceil((def.frontBounds + def.rearBounds) * ratio);
            zRes = (int) Math.ceil(def.widthBounds * ratio);
            components = def.renderComponents.values().stream()
                    .flatMap(Collection::stream)
                    .filter(rc -> rc.type.collisionsEnabled)
                    .collect(Collectors.toList());
            data = new float[components.size() * xRes * zRes];

            VertexBuffer vb = def.model.vbo.buffer.get();

            for (int i = 0; i < components.size(); i++) {
                ModelComponent rc = components.get(i);
                int idx = i * xRes * zRes;
                for (String group : rc.modelIDs) {
                    OBJGroup faces = def.model.groups.get(group);

                    for (int face = faces.faceStart; face <= faces.faceStop; face++) {
                        Path2D path = new Path2D.Float();
                        float fheight = 0;
                        boolean first = true;
                        for (int point = 0; point < vb.vertsPerFace; point++) {
                            int vertex = face * vb.vertsPerFace * vb.stride + point * vb.stride;
                            float vertX = vb.data[vertex + 0];
                            float vertY = vb.data[vertex + 1];
                            float vertZ = vb.data[vertex + 2];
                            vertX += def.frontBounds;
                            vertZ += def.widthBounds / 2;
                            if (first) {
                                path.moveTo(vertX * ratio, vertZ * ratio);
                                first = false;
                            } else {
                                path.lineTo(vertX * ratio, vertZ * ratio);
                            }
                            fheight += vertY / vb.vertsPerFace;
                        }
                        Rectangle2D bounds = path.getBounds2D();
                        if (bounds.getWidth() * bounds.getHeight() < 1) {
                            continue;
                        }
                        for (int x = 0; x < xRes; x++) {
                            for (int z = 0; z < zRes; z++) {
                                float relX = ((xRes - 1) - x);
                                float relZ = z;
                                if (bounds.contains(relX, relZ) && path.contains(relX, relZ)) {
                                    float relHeight = fheight / (float) def.heightBounds;
                                    relHeight = ((int) Math.ceil(relHeight * precision)) / (float) precision;
                                    data[idx + x * zRes + z] = Math.max(data[idx + x * zRes + z], relHeight);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Function<EntityBuildableRollingStock, float[][]> initHeightmap() {
        String key = String.format(
                "%s-%s-%s-%s-%s-%s",
                model.hash, frontBounds, rearBounds, widthBounds, heightBounds, renderComponents.size());
        try {
            ResourceCache<HeightMapData> cache = new ResourceCache<>(
                    new Identifier(modelLoc.getDomain(), modelLoc.getPath() + "_heightmap_" + key.hashCode()),
                    provider -> new HeightMapData(this)
            );
            Supplier<GenericByteBuffer> data = cache.getResource("data.bin", builder -> new GenericByteBuffer(builder.data));
            Supplier<GenericByteBuffer> meta = cache.getResource("meta.nbt", builder -> {
                try {
                    return new GenericByteBuffer(new TagCompound()
                            .setInteger("xRes", builder.xRes)
                            .setInteger("zRes", builder.zRes)
                            .setList("components", builder.components, v -> new TagCompound().setString("key", v.key))
                            .toBytes()
                    );
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            cache.close();

            return (stock) -> {
                try {
                    float[] raw = data.get().floats();
                    TagCompound tc = new TagCompound(meta.get().bytes());

                    int xRes = tc.getInteger("xRes");
                    int zRes = tc.getInteger("zRes");
                    List<String> componentKeys = tc.getList("components", v -> v.getString("key"));

                    float[][] heightMap = new float[xRes][zRes];

                    List<ModelComponentType> availComponents = new ArrayList<>();
                    for (ItemComponentType item : stock.getItemComponents()) {
                        availComponents.addAll(item.render);
                    }

                    for (List<ModelComponent> rcl : this.renderComponents.values()) {
                        for (ModelComponent rc : rcl) {
                            if (!rc.type.collisionsEnabled) {
                                continue;
                            }

                            if (availComponents.contains(rc.type)) {
                                availComponents.remove(rc.type);
                            } else if (rc.type == ModelComponentType.REMAINING && stock.isBuilt()) {
                                //pass
                            } else {
                                continue;
                            }

                            int idx = componentKeys.indexOf(rc.key) * xRes * zRes;
                            if (idx < 0) {
                                // Code changed, we should probably invalidate this cache key...
                                continue;
                            }
                            for (int x = 0; x < xRes; x++) {
                                for (int z = 0; z < zRes; z++) {
                                    heightMap[x][z] = Math.max(heightMap[x][z], raw[idx + x * zRes + z]);
                                }
                            }
                        }
                    }

                    return heightMap;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public float[][] createHeightMap(EntityBuildableRollingStock stock) {
        return heightmap.apply(stock);
    }

    public RealBB getBounds(float yaw, Gauge gauge) {
        return new RealBB(gauge.scale() * frontBounds, gauge.scale() * -rearBounds, gauge.scale() * widthBounds,
                gauge.scale() * heightBounds, yaw);
    }

    public String name() {
        String[] sp = this.defID.replaceAll(".json", "").split("/");
        String localStr = String.format("%s:entity.%s.%s", ImmersiveRailroading.MODID, sp[sp.length - 2], sp[sp.length - 1]);
        String transStr = TextUtil.translate(localStr);
        return !localStr.equals(transStr) ? transStr : name;
    }

    public List<String> getTooltip(Gauge gauge) {
        List<String> tips = new ArrayList<>();
        tips.add(GuiText.WEIGHT_TOOLTIP.toString(this.getWeight(gauge)));
        tips.add(GuiText.MODELER_TOOLTIP.toString(modelerName));
        tips.add(GuiText.PACK_TOOLTIP.toString(packName));
        return tips;
    }

    protected StockModel<?, ?> createModel() throws Exception {
        return new StockModel<>(this);
    }
    public StockModel<?, ?> getModel() {
        return this.model;
    }

    /**
     * @return Stock Weight in Kg
     */
    public int getWeight(Gauge gauge) {
        return (int) Math.ceil(gauge.scale() * this.weight);
    }

    public double getHeight(Gauge gauge) {
        return gauge.scale() * this.heightBounds;
    }

    public double getWidth(Gauge gauge) {
        return gauge.scale() * this.widthBounds;
    }

    public double getLength(Gauge gauge) {
        return gauge.scale() * this.frontBounds + this.rearBounds;
    }

    public int getMaxPassengers() {
        return this.maxPassengers;
    }

    public boolean acceptsPassengers() {
        return false;
    }

    public boolean acceptsLivestock() {
        return false;
    }

    static class TagMapper implements cam72cam.mod.serialization.TagMapper<EntityRollingStockDefinition> {
        @Override
        public TagAccessor<EntityRollingStockDefinition> apply(Class<EntityRollingStockDefinition> type, String fieldName, TagField tag) {
            return new TagAccessor<>(
                    (d, o) -> d.setString(fieldName, o == null ? null : o.defID),
                    d -> DefinitionManager.getDefinition(d.getString(fieldName))
            );
        }
    }

    public ValveGearConfig getValveGear() {
        return valveGear;
    }

    public LightDefinition getLight(String name) {
        return lights.get(name);
    }
    public ControlSoundsDefinition getControlSound(String name) {
        return controlSounds.get(name);
    }

    public float interiorLightLevel() {
        return interiorLightLevel;
    }


    protected boolean independentBrakeDefault() {
        return false;
    }

    public boolean isLinearBrakeControl() {
        return isLinearBrakeControl;
    }

    protected GuiBuilder getDefaultOverlay(JsonObject data) throws IOException {
        return hasIndependentBrake() ? GuiBuilder.parse(new Identifier(ImmersiveRailroading.MODID, "gui/default/independent.json")) : null;
    }

    public GuiBuilder getOverlay() {
        return overlay;
    }

    public List<String> getExtraTooltipInfo() {
        return extraTooltipInfo;
    }

    public boolean hasInternalLighting() {
        return hasInternalLighting;
    }

    public double getSwayMultiplier() {
        return swayMultiplier;
    }

    public double getTiltMultiplier() {
        return tiltMultiplier;
    }

    public double getBrakeShoeFriction() {
        return brakeCoefficient;
    }

}
