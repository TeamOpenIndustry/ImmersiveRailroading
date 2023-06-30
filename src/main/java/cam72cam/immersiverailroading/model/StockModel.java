package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.model.animation.StockAnimation;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.*;
import cam72cam.immersiverailroading.model.part.TrackFollower.TrackFollowers;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
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
            if (!stock.internalLightsEnabled()) {
                return base;
            }
            float blockLight = stock.getWorld().getBlockLightLevel(stock.getBlockPosition());
            float skyLight = stock.getWorld().getSkyLightLevel(stock.getBlockPosition());
            boolean brighter = blockLight < interiorLight;
            return base.merge(new ModelState.LightState(brighter ? interiorLight : null, brighter ? skyLight : null, true, null));
        };

        animations = new ArrayList<>();
        for (EntityRollingStockDefinition.AnimationDefinition animDef : def.animations) {
            if (animDef.valid()) {
                animations.add(new StockAnimation(animDef, def.internal_model_scale));
            }
        }
        ModelState.GroupAnimator animators = (stock, group) -> {
            Matrix4 m = null;
            for (StockAnimation animation : animations) {
                Matrix4 found = animation.getMatrix(stock , group);
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

        ComponentProvider provider = new ComponentProvider(this, def.internal_model_scale);
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

        if (bogeyFront != null && Math.abs(def.getBogeyFront(Gauge.from(Gauge.STANDARD)) + bogeyFront.center().x) > 0.5) {
            frontTrackers = new TrackFollowers(s -> new TrackFollower(s, bogeyFront.center(), def.getBogeyFront(s.gauge)));
        } else {
            frontTrackers = null;
        }
        if (bogeyRear != null && Math.abs(def.getBogeyRear(Gauge.from(Gauge.STANDARD)) + bogeyRear.center().x) > 0.5) {
            rearTrackers = new TrackFollowers(s -> new TrackFollower(s, bogeyRear.center(), def.getBogeyRear(s.gauge)));
        } else {
            rearTrackers = null;
        }

    }

    public ModelState addRoll(ModelState state) {
        return state.push(builder -> builder.add((ModelState.Animator) stock ->
                new Matrix4().rotate(Math.toRadians(stock.getRollDegrees()), 1, 0, 0)));
    }

    protected void initStates() {
        this.rocking = addRoll(this.base);
        this.front = this.base.push(settings -> settings.add(this::getFrontBogeyMatrix));
        this.frontRocking = addRoll(this.front);
        this.rear = this.base.push(settings -> settings.add(this::getRearBogeyMatrix));
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
        this.frame = new Frame(provider, base, rocking, def.defID, def.getValveGear());
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
    }

    public final void onClientRemoved(EntityMoveableRollingStock stock) {
        removed((ENTITY) stock);
    }

    protected void removed(ENTITY stock) {
        headlights.forEach(x -> x.removed(stock));
    }

    private int lod_level = LOD_LARGE;
    private int lod_tick = 0;
    public final void render(EntityMoveableRollingStock stock, RenderState state, float partialTicks) {
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

            stock.distanceTraveled = stock.distanceTraveled + stock.getCurrentSpeed().minecraft() * stock.getTickSkew() * partialTicks * 1.1;
            stock.distanceTraveled /= stock.gauge.scale();


            base.render(bound, stock, available);

            stock.distanceTraveled = backup;
        }
    }

    public void postRender(EntityMoveableRollingStock stock, RenderState state, float partialTicks) {
        postRender((ENTITY) stock, state);
    }

    // TODO invert -> reinvert sway
    private Matrix4 getFrontBogeyMatrix(EntityMoveableRollingStock stock) {
        if (frontTrackers != null) {
            return frontTrackers.get(stock).getMatrix();
        } else {
            Matrix4 matrix = new Matrix4(); // TODO cache this?
            matrix.translate(-def.getBogeyFront(Gauge.standard()), 0, 0);
            matrix.rotate(Math.toRadians(stock.getRotationYaw() - stock.getFrontYaw()), 0, 1, 0);
            matrix.translate(def.getBogeyFront(Gauge.standard()), 0, 0);
            return matrix;
        }
    }

    public float getFrontYaw(EntityMoveableRollingStock stock) {
        return frontTrackers != null ? frontTrackers.get(stock).toPointYaw + frontTrackers.get(stock).atPointYaw : stock.getRotationYaw() - stock.getFrontYaw();
    }

    private Matrix4 getRearBogeyMatrix(EntityMoveableRollingStock stock) {
        if (rearTrackers != null) {
            return rearTrackers.get(stock).getMatrix();
        } else {
            Matrix4 matrix = new Matrix4(); // TODO cache this?
            matrix.translate(-def.getBogeyRear(Gauge.standard()), 0, 0);
            matrix.rotate(Math.toRadians(stock.getRotationYaw() - stock.getRearYaw()), 0, 1, 0);
            matrix.translate(def.getBogeyRear(Gauge.standard()), 0, 0);
            return matrix;
        }
    }

    public float getRearYaw(EntityMoveableRollingStock stock) {
        return rearTrackers != null ? rearTrackers.get(stock).toPointYaw + rearTrackers.get(stock).atPointYaw : stock.getRotationYaw() - stock.getRearYaw();
    }

    protected void postRender(ENTITY stock, RenderState state) {
        state.rotate(stock.getRollDegrees(), 1, 0, 0);
        controls.forEach(c -> c.postRender(stock, state));
        doors.forEach(c -> c.postRender(stock, state));
        gauges.forEach(c -> c.postRender(stock, state));
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
