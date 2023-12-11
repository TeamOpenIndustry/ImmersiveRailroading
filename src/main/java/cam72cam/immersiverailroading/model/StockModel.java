package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.CarPassenger;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.model.animation.StockAnimation;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.*;
import cam72cam.immersiverailroading.model.part.TrackFollower.TrackFollowers;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition.SoundDefinition;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.OptiFine;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.RenderState;
import util.Matrix4;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StockModel<ENTITY extends EntityMoveableRollingStock, DEFINITION extends EntityRollingStockDefinition> extends OBJModel {
    private final DEFINITION def;
    public final List<ModelComponent> allComponents;
    protected ModelState base;
    protected ModelState rocking;
    protected ModelState front;
    protected ModelState frontRocking;
    protected ModelState rear;
    protected ModelState rearRocking;
    protected Frame frame;
    protected Bogey bogeyFront;
    protected Bogey bogeyRear;
    protected DrivingAssembly drivingWheels;
    private ModelComponent shell;
    private ModelComponent remaining;
    protected final List<Door<ENTITY>> doors;
    protected final List<Control<ENTITY>> controls;
    protected final List<Readout<ENTITY>> gauges;
    protected final List<Seat<ENTITY>> seats;

    protected List<LightFlare<ENTITY>> headlights;

    private final TrackFollowers frontTrackers;
    private final TrackFollowers rearTrackers;

    public static final int LOD_LARGE = 1024;
    public static final int LOD_SMALL = 512;

    private final List<StockAnimation> animations;

    private final float sndRand;
    private final PartSound wheel_sound;
    private final PartSound slidingSound;
    private final FlangeSound flangeSound;
    private final SwaySimulator sway;

    public StockModel(DEFINITION def) throws Exception {
        super(def.modelLoc, def.darken, def.internal_model_scale, def.textureNames.keySet(), ConfigGraphics.textureCacheSeconds, i -> {
            List<Integer> lodSizes = new ArrayList<>();
            lodSizes.add(LOD_LARGE);
            lodSizes.add(LOD_SMALL);
            return lodSizes;
        });

        this.def = def;
        boolean hasInterior = this.groups().stream().anyMatch(x -> x.contains("INTERIOR"));

        this.doors = new ArrayList<>();
        this.seats = new ArrayList<>();
        this.controls = new ArrayList<>();
        this.gauges = new ArrayList<>();
        this.headlights = new ArrayList<>();

        ModelState.LightState base = new ModelState.LightState(null, null, null, hasInterior);

        float interiorLight = def.interiorLightLevel();
        ModelState.Lighter interiorLit = stock -> {
            if (!stock.hasElectricalPower()) {
                return base;
            }
            boolean hasInteriorOverride = hasInterior;
            if (!hasInteriorOverride && ConfigGraphics.FakeInteriorLighting) {
                // No interior found in this stock, should we use a fallback?

                if (stock instanceof Locomotive  || stock instanceof CarPassenger) {
                    // Locomotives and cars should pretend to have an interior when not ridden
                    hasInteriorOverride = MinecraftClient.getPlayer().getRiding() != stock;
                } else {
                    // All other stock should pretend to have an interior to prevent the fallback
                    hasInteriorOverride = true;
                }
            }
            float blockLight = stock.getWorld().getBlockLightLevel(stock.getBlockPosition());
            float skyLight = stock.getWorld().getSkyLightLevel(stock.getBlockPosition());
            boolean brighter = blockLight < interiorLight;
            return base.merge(new ModelState.LightState(brighter ? interiorLight : null, brighter ? skyLight : null, true, hasInteriorOverride));
        };

        animations = new ArrayList<>();
        for (EntityRollingStockDefinition.AnimationDefinition animDef : def.animations) {
            if (animDef.valid()) {
                animations.add(new StockAnimation(animDef, def.internal_model_scale));
            }
        }
        ModelState.GroupAnimator animators = (stock, group, partialTicks) -> {
            Matrix4 m = null;
            for (StockAnimation animation : animations) {
                Matrix4 found = animation.getMatrix(stock , group, partialTicks);
                if (found != null) {
                    if (m == null) {
                        m = found;
                    } else {
                        m.multiply(found);
                    }
                }
            }
            return m;
        };
        this.base = ModelState.construct(settings -> settings.add(animators).add(interiorLit));

        ComponentProvider provider = new ComponentProvider(this, def.internal_model_scale, def.widgetConfig);
        initStates();
        parseControllable(provider, def);

        // Shay Hack...
        // A proper dependency tree would be ideal...
        this.bogeyFront = Bogey.get(provider, front, unifiedBogies(), ModelPosition.FRONT);
        this.bogeyRear = Bogey.get(provider, rear, unifiedBogies(), ModelPosition.REAR);

        parseComponents(provider, def);
        provider.parse(ModelComponentType.IMMERSIVERAILROADING_BASE_COMPONENT);
        this.remaining = provider.parse(ModelComponentType.REMAINING);
        rocking.include(remaining);
        this.allComponents = provider.components();

        frontTrackers = new TrackFollowers(s -> new TrackFollower(s, bogeyFront != null ? bogeyFront.bogey : null, bogeyFront != null ? bogeyFront.wheels : null, true));
        rearTrackers = new TrackFollowers(s -> new TrackFollower(s, bogeyRear != null ? bogeyRear.bogey : null,  bogeyRear != null ? bogeyRear.wheels : null, false));

        sndRand = (float) Math.random() / 10;
        wheel_sound = new PartSound(new SoundDefinition(def.wheel_sound), true, 40, ConfigSound.SoundCategories.RollingStock::wheel);
        slidingSound = new PartSound(new SoundDefinition(def.sliding_sound), true, 40, ConfigSound.SoundCategories.RollingStock::sliding);
        flangeSound = new FlangeSound(def.flange_sound, true, 40);
        sway = new SwaySimulator();
    }

    public ModelState addRoll(ModelState state) {
        return state.push(builder -> builder.add((ModelState.Animator) (stock, partialTicks) ->
                new Matrix4().rotate(Math.toRadians(sway.getRollDegrees(stock, partialTicks)), 1, 0, 0)));
    }

    protected void initStates() {
        this.rocking = addRoll(this.base);
        this.front = this.base.push(settings -> settings.add((EntityMoveableRollingStock stock, float partialTicks) -> getFrontBogeyMatrix(stock)));
        this.frontRocking = addRoll(this.front);
        this.rear = this.base.push(settings -> settings.add((EntityMoveableRollingStock stock, float partialTicks) -> getRearBogeyMatrix(stock)));
        this.rearRocking = addRoll(this.rear);
    }

    protected void addGauge(ComponentProvider provider, ModelComponentType type, Readouts value) {
        gauges.addAll(Readout.getReadouts(provider, frontRocking, type, ModelPosition.BOGEY_FRONT, value));
        gauges.addAll(Readout.getReadouts(provider, rearRocking, type, ModelPosition.BOGEY_REAR, value));
        gauges.addAll(Readout.getReadouts(provider, rocking, type, ModelPosition.FRONT, value));
        gauges.addAll(Readout.getReadouts(provider, rocking, type, ModelPosition.REAR, value));
        gauges.addAll(Readout.getReadouts(provider, rocking, type, value));
    }

    protected void addControl(ComponentProvider provider, ModelComponentType type) {
        controls.addAll(Control.get(provider, frontRocking, type, ModelPosition.BOGEY_FRONT));
        controls.addAll(Control.get(provider, rearRocking, type, ModelPosition.BOGEY_REAR));
        controls.addAll(Control.get(provider, rocking, type, ModelPosition.FRONT));
        controls.addAll(Control.get(provider, rocking, type, ModelPosition.REAR));
        controls.addAll(Control.get(provider, rocking, type));
    }

    protected void addDoor(ComponentProvider provider) {
        this.doors.addAll(Door.get(provider, frontRocking, ModelPosition.BOGEY_FRONT));
        this.doors.addAll(Door.get(provider, rearRocking, ModelPosition.BOGEY_REAR));
        this.doors.addAll(Door.get(provider, rocking));
    }

    protected void addHeadlight(DEFINITION def, ComponentProvider provider, ModelComponentType type) {
        this.headlights.addAll(LightFlare.get(def, provider, frontRocking, type, ModelPosition.BOGEY_FRONT));
        this.headlights.addAll(LightFlare.get(def, provider, rearRocking, type, ModelPosition.BOGEY_REAR));
        this.headlights.addAll(LightFlare.get(def, provider, rocking, type));
    }

    protected void parseControllable(ComponentProvider provider, DEFINITION def) {
        gauges.addAll(Readout.getReadouts(provider, frontRocking, ModelComponentType.COUPLED_X, ModelPosition.BOGEY_FRONT, Readouts.COUPLED_FRONT));
        gauges.addAll(Readout.getReadouts(provider, rearRocking, ModelComponentType.COUPLED_X, ModelPosition.BOGEY_REAR, Readouts.COUPLED_REAR));
        gauges.addAll(Readout.getReadouts(provider, rocking, ModelComponentType.COUPLED_X, ModelPosition.FRONT, Readouts.COUPLED_FRONT));
        gauges.addAll(Readout.getReadouts(provider, rocking, ModelComponentType.COUPLED_X, ModelPosition.REAR, Readouts.COUPLED_REAR));

        addControl(provider, ModelComponentType.COUPLER_ENGAGED_X);

        if (def.hasIndependentBrake()) {
            addGauge(provider, ModelComponentType.GAUGE_INDEPENDENT_BRAKE_X, Readouts.INDEPENDENT_BRAKE);
        }
        addGauge(provider, ModelComponentType.BRAKE_PRESSURE_X, Readouts.BRAKE_PRESSURE);
        addControl(provider, ModelComponentType.WINDOW_X);
        addControl(provider, ModelComponentType.WIDGET_X);

        if (def.hasIndependentBrake()) {
            addControl(provider, ModelComponentType.INDEPENDENT_BRAKE_X);
        }

        addDoor(provider);
        seats.addAll(Seat.get(provider, rocking));

        addHeadlight(def, provider, ModelComponentType.HEADLIGHT_X);
    }

    protected void parseComponents(ComponentProvider provider, DEFINITION def) {
        this.frame = new Frame(provider, base, rocking, def.defID);

        drivingWheels = DrivingAssembly.get(def.getValveGear(), provider, base, 0,
                frame != null ? frame.wheels : null,
                bogeyFront != null ? bogeyFront.wheels : null,
                bogeyRear != null ? bogeyRear.wheels : null
        );

        this.shell = provider.parse(ModelComponentType.SHELL);
        rocking.include(shell);
    }

    protected boolean unifiedBogies() {
        return true;
    }


    public final void onClientTick(EntityMoveableRollingStock stock) {
        effects((ENTITY) stock);
    }

    protected void effects(ENTITY stock) {
        headlights.forEach(x -> x.effects(stock));
        controls.forEach(c -> c.effects(stock));
        doors.forEach(c -> c.effects(stock));
        gauges.forEach(c -> c.effects(stock));
        animations.forEach(c -> c.effects(stock));


        float adjust = (float) Math.abs(stock.getCurrentSpeed().metric()) / 300;
        float pitch = adjust + 0.7f;
        if (stock.getDefinition().shouldScalePitch()) {
            // TODO this is probably wrong...
            pitch = (float) (pitch/stock.gauge.scale());
        }
        float volume = 0.01f + adjust;

        wheel_sound.effects(stock, Math.abs(stock.getCurrentSpeed().metric()) > 1 ? volume : 0, pitch + sndRand);
        slidingSound.effects(stock, stock.sliding ? Math.min(1, adjust*4) : 0);
        flangeSound.effects(stock);
        sway.effects(stock);
    }

    public final void onClientRemoved(EntityMoveableRollingStock stock) {
        removed((ENTITY) stock);
    }

    protected void removed(ENTITY stock) {
        headlights.forEach(x -> x.removed(stock));
        controls.forEach(c -> c.removed(stock));
        doors.forEach(c -> c.removed(stock));
        gauges.forEach(c -> c.removed(stock));
        animations.forEach(c -> c.removed(stock));

        wheel_sound.removed(stock);
        slidingSound.removed(stock);
        flangeSound.removed(stock);
        sway.removed(stock);
    }

    private int lod_level = LOD_LARGE;
    private int lod_tick = 0;
    public final void renderEntity(EntityMoveableRollingStock stock, RenderState state, float partialTicks) {
        List<ModelComponentType> available = stock.isBuilt() ? null : stock.getItemComponents()
                .stream().flatMap(x -> x.render.stream())
                .collect(Collectors.toList());

        state.lighting(true)
                .cull_face(false)
                .rescale_normal(true)
                .scale(stock.gauge.scale(), stock.gauge.scale(), stock.gauge.scale());

        if ((ConfigGraphics.OptifineEntityShaderOverrideAll || !normals.isEmpty() || !speculars.isEmpty()) &&
                ConfigGraphics.OptiFineEntityShader != OptiFine.Shaders.Entities) {
            state = state.shader(ConfigGraphics.OptiFineEntityShader);
        }

        // Refresh LOD every 0.5s
        if (lod_tick + 10 < stock.getTickCount() || lod_tick > stock.getTickCount())  {
            lod_tick = stock.getTickCount();

            double playerDistanceSq = stock.getWorld().getEntities(stock.getClass()).stream()
                    .filter(x -> Objects.equals(x.getDefinitionID(), stock.getDefinitionID()) && Objects.equals(x.getTexture(), stock.getTexture()))
                    .mapToDouble(x -> x.getPosition().distanceToSquared(MinecraftClient.getPlayer().getPosition())).min().orElse(0);

            if (playerDistanceSq > ConfigGraphics.StockLODDistance * 2 * ConfigGraphics.StockLODDistance * 2) {
                lod_level = LOD_SMALL;
            } else if (playerDistanceSq > ConfigGraphics.StockLODDistance * ConfigGraphics.StockLODDistance) {
                lod_level = LOD_LARGE;
            } else {
                lod_level = cam72cam.mod.Config.MaxTextureSize;
            }
        }

        Binder binder = binder().texture(stock.getTexture()).lod(lod_level);
        try (
                OBJRender.Binding bound = binder.bind(state);
        ) {
            double backup = stock.distanceTraveled;

            if (!stock.isSliding()) {
                stock.distanceTraveled = stock.distanceTraveled + stock.getCurrentSpeed().minecraft() * stock.getTickSkew() * partialTicks * 1.1;
            }
            stock.distanceTraveled /= stock.gauge.scale();


            base.render(bound, stock, available, partialTicks);

            stock.distanceTraveled = backup;
        }
    }

    public void postRenderEntity(EntityMoveableRollingStock stock, RenderState state, float partialTicks) {
        postRender((ENTITY) stock, state, partialTicks);
    }

    // TODO invert -> reinvert sway
    private Matrix4 getFrontBogeyMatrix(EntityMoveableRollingStock stock) {
        return frontTrackers.get(stock).getMatrix();
    }

    public float getFrontYaw(EntityMoveableRollingStock stock) {
        return frontTrackers.get(stock).getYawReadout();
    }

    private Matrix4 getRearBogeyMatrix(EntityMoveableRollingStock stock) {
        return rearTrackers.get(stock).getMatrix();
    }

    public float getRearYaw(EntityMoveableRollingStock stock) {
        return rearTrackers.get(stock).getYawReadout();
    }

    protected void postRender(ENTITY stock, RenderState state, float partialTicks) {
        state.scale(stock.gauge.scale(), stock.gauge.scale(), stock.gauge.scale());
        state.rotate(sway.getRollDegrees(stock, partialTicks), 1, 0, 0);
        controls.forEach(c -> c.postRender(stock, state, partialTicks));
        doors.forEach(c -> c.postRender(stock, state, partialTicks));
        gauges.forEach(c -> c.postRender(stock, state, partialTicks));
        headlights.forEach(x -> x.postRender(stock, state));
    }

    public List<Control<ENTITY>> getControls() {
        return controls;
    }

    public List<Door<ENTITY>> getDoors() {
        return doors;
    }

    public List<Control<ENTITY>> getDraggable() {
        List<Control<ENTITY>> draggable = new ArrayList<>();
        draggable.addAll(controls);
        draggable.addAll(doors);
        return draggable;
    }

    public List<Interactable<ENTITY>> getInteractable() {
        List<Interactable<ENTITY>> interactable = new ArrayList<>(getDraggable());
        interactable.addAll(seats);
        return interactable;
    }

    public List<Seat<ENTITY>> getSeats() {
        return seats;
    }
}
