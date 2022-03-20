package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.*;
import cam72cam.immersiverailroading.model.part.TrackFollower.TrackFollowers;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.RenderState;
import util.Matrix4;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StockModel<T extends EntityMoveableRollingStock> extends OBJModel {
    private final EntityRollingStockDefinition def;
    public final List<ModelComponent> allComponents;
    private Frame frame;
    private Bogey bogeyFront;
    private Bogey bogeyRear;
    private ModelComponent shell;
    private ModelComponent remaining;
    protected final List<Door<T>> doors;
    protected final List<Control<T>> controls;
    protected final List<Readout<T>> gauges;

    protected List<LightFlare<T>> headlights;

    private final TrackFollowers frontTrackers;
    private final TrackFollowers rearTrackers;
    private final boolean hasInterior;

    public StockModel(EntityRollingStockDefinition def) throws Exception {
        //TODO BORK BORK BORK ConfigGraphics.textureCacheSeconds
        super(def.modelLoc, def.darken, def.internal_model_scale, def.textureNames.keySet());
        this.def = def;
        this.hasInterior = this.groups().stream().anyMatch(x -> x.contains("INTERIOR"));

        this.doors = new ArrayList<>();
        this.controls = new ArrayList<>();
        this.gauges = new ArrayList<>();
        this.headlights = new ArrayList<>();

        ComponentProvider provider = new ComponentProvider(this);
        parseControllable(provider, def);
        parseComponents(provider, def);
        provider.parse(ModelComponentType.IMMERSIVERAILROADING_BASE_COMPONENT);
        this.remaining = provider.parse(ModelComponentType.REMAINING);
        this.allComponents = provider.components();

        if (bogeyFront != null && Math.abs(def.getBogeyFront(Gauge.from(Gauge.STANDARD)) + bogeyFront.center().x) > 0.5) {
            frontTrackers = new TrackFollowers(s -> bogeyFront.center());
        } else {
            frontTrackers = null;
        }
        if (bogeyRear != null && Math.abs(def.getBogeyRear(Gauge.from(Gauge.STANDARD)) + bogeyRear.center().x) > 0.5) {
            rearTrackers = new TrackFollowers(s -> bogeyRear.center());
        } else {
            rearTrackers = null;
        }
    }

    protected void addGauge(ComponentProvider provider, ModelComponentType type, Readouts value) {
        gauges.addAll(Readout.getReadouts(provider, type, ModelPosition.BOGEY_FRONT, value, this::getFrontBogeyMatrix));
        gauges.addAll(Readout.getReadouts(provider, type, ModelPosition.BOGEY_REAR, value, this::getRearBogeyMatrix));
        gauges.addAll(Readout.getReadouts(provider, type, ModelPosition.FRONT, value, null));
        gauges.addAll(Readout.getReadouts(provider, type, ModelPosition.REAR, value, null));
        gauges.addAll(Readout.getReadouts(provider, type, value));
    }

    protected void addControl(ComponentProvider provider, ModelComponentType type) {
        controls.addAll(Control.get(provider, type, ModelPosition.BOGEY_FRONT, this::getFrontBogeyMatrix));
        controls.addAll(Control.get(provider, type, ModelPosition.BOGEY_REAR, this::getRearBogeyMatrix));
        controls.addAll(Control.get(provider, type, ModelPosition.FRONT, null));
        controls.addAll(Control.get(provider, type, ModelPosition.REAR, null));
        controls.addAll(Control.get(provider, type));
    }

    protected void addDoor(ComponentProvider provider) {
        this.doors.addAll(Door.get(provider, ModelPosition.BOGEY_FRONT, this::getFrontBogeyMatrix));
        this.doors.addAll(Door.get(provider, ModelPosition.BOGEY_REAR, this::getRearBogeyMatrix));
        this.doors.addAll(Door.get(provider));
    }

    protected void addHeadlight(EntityRollingStockDefinition def, ComponentProvider provider, ModelComponentType type) {
        this.headlights.addAll(LightFlare.get(def, provider, type, ModelPosition.BOGEY_FRONT, this::getFrontBogeyMatrix));
        this.headlights.addAll(LightFlare.get(def, provider, type, ModelPosition.BOGEY_REAR, this::getRearBogeyMatrix));
        this.headlights.addAll(LightFlare.get(def, provider, type));
    }

    protected void parseControllable(ComponentProvider provider, EntityRollingStockDefinition def) {
        gauges.addAll(Readout.getReadouts(provider, ModelComponentType.COUPLED_X, ModelPosition.BOGEY_FRONT, Readouts.COUPLED_FRONT, this::getFrontBogeyMatrix));
        gauges.addAll(Readout.getReadouts(provider, ModelComponentType.COUPLED_X, ModelPosition.BOGEY_REAR, Readouts.COUPLED_REAR, this::getRearBogeyMatrix));
        gauges.addAll(Readout.getReadouts(provider, ModelComponentType.COUPLED_X, ModelPosition.FRONT, Readouts.COUPLED_FRONT, null));
        gauges.addAll(Readout.getReadouts(provider, ModelComponentType.COUPLED_X, ModelPosition.REAR, Readouts.COUPLED_REAR, null));

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

        addHeadlight(def, provider, ModelComponentType.HEADLIGHT_X);
    }

    protected void parseComponents(ComponentProvider provider, EntityRollingStockDefinition def) {
        this.frame = new Frame(provider, def.defID, def.getValveGear());
        this.shell = provider.parse(ModelComponentType.SHELL);
        this.bogeyFront = Bogey.get(provider, unifiedBogies(), ModelPosition.FRONT);
        this.bogeyRear = Bogey.get(provider, unifiedBogies(), ModelPosition.REAR);
    }

    protected boolean unifiedBogies() {
        return true;
    }


    public final void onClientTick(EntityMoveableRollingStock stock) {
        effects((T) stock);
    }

    protected void effects(T stock) {
        headlights.forEach(x -> x.effects(stock));
        controls.forEach(c -> c.effects(stock));
        doors.forEach(c -> c.effects(stock));
        gauges.forEach(c -> c.effects(stock));
    }

    public final void onClientRemoved(EntityMoveableRollingStock stock) {
        removed((T) stock);
    }

    protected void removed(T stock) {
        headlights.forEach(x -> x.removed(stock));
    }

    public final void render(EntityMoveableRollingStock stock, RenderState state, float partialTicks) {
        List<ModelComponentType> available = stock.isBuilt() ? null : stock.getItemComponents()
                .stream().flatMap(x -> x.render.stream())
                .collect(Collectors.toList());

        try {
            //Class<?> ShaderRenderer = Class.forName("net.optifine.shaders.ShadersRender");
            //Method beginTerrainSolid = ShaderRenderer.getDeclaredMethod("beginTerrainSolid");
            //beginTerrainSolid.invoke(ShaderRenderer);
            //ShaderRenderer.getDeclaredMethod("beginHand", Boolean.class).invoke(false);
            //Class.forName("net.optifine.shaders.Shaders").getDeclaredMethod("beginHand", boolean.class).invoke(null, false);
            //Class.forName("net.optifine.shaders.Shaders").getDeclaredMethod("beginLitParticles").invoke(null);
            //Class.forName("net.optifine.shaders.ShadersRender").getDeclaredMethod("beginTerrainSolid").invoke(null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        state.lighting(true)
                .cull_face(false)
                .rescale_normal(true)
                .scale(stock.gauge.scale(), stock.gauge.scale(), stock.gauge.scale());

        try (
                OBJRender.Binding bound = binder().texture(stock.getTexture()).bind(state);
        ) {
            double distanceTraveled = stock.distanceTraveled + stock.getCurrentSpeed().minecraft() * stock.getTickSkew() * partialTicks * 1.1;
            distanceTraveled /= stock.gauge.scale();

            try (
                    ComponentRenderer draw = new ComponentRenderer(stock, bound, available, hasInterior)
            ) {
                //noinspection unchecked
                render((T) stock, draw, distanceTraveled);
            }
        }

        try {
            //Class<?> ShaderRenderer = Class.forName("net.optifine.shaders.ShadersRender");
            //Method endTerrain = ShaderRenderer.getDeclaredMethod("endTerrain");
            //endTerrain.invoke(ShaderRenderer);
            //Class.forName("net.optifine.shaders.Shaders").getDeclaredMethod("endHand").invoke(null);
            //Class.forName("net.optifine.shaders.Shaders").getDeclaredMethod("endParticles").invoke(null);
            //Class.forName("net.optifine.shaders.ShadersRender").getDeclaredMethod("endTerrain").invoke(null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void postRender(EntityMoveableRollingStock stock, RenderState state, float partialTicks) {
        postRender((T) stock, state);
    }

    private Matrix4 getFrontBogeyMatrix(T stock) {
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

    private Matrix4 getRearBogeyMatrix(T stock) {
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

    protected void render(T stock, ComponentRenderer draw, double distanceTraveled) {
        frame.render(distanceTraveled, draw);

        try (ComponentRenderer light = draw.withBrightGroups(true)) {
            headlights.forEach(x -> x.render(light, stock));
        }

        try (ComponentRenderer light = stock.internalLightsEnabled() ? draw.withBrightGroups(true).withInteriorLight(stock) : draw) {
            renderWithInteriorLighting(stock, light);
        }

        if (bogeyFront != null) {
            try (ComponentRenderer matrix = draw.push()) {
                matrix.mult(getFrontBogeyMatrix(stock));
                bogeyFront.render(distanceTraveled, matrix);
            }
        }

        if (bogeyRear != null) {
            try (ComponentRenderer matrix = draw.push()) {
                matrix.mult(getRearBogeyMatrix(stock));
                bogeyRear.render(distanceTraveled, matrix);
            }
        }
    }

    protected void renderWithInteriorLighting(T stock, ComponentRenderer draw) {
        draw.render(shell);
        draw.render(remaining);
        controls.forEach(c -> c.render(stock, draw));
        doors.forEach(c -> c.render(stock, draw));
        gauges.forEach(r -> r.render(stock, draw));
    }

    protected void postRender(T stock, RenderState state) {
        controls.forEach(c -> c.postRender(stock, state));
        doors.forEach(c -> c.postRender(stock, state));
        gauges.forEach(c -> c.postRender(stock, state));
        headlights.forEach(x -> x.postRender(stock, state));
    }

    public List<Control<T>> getControls() {
        return controls;
    }

    public List<Door<T>> getDoors() {
        return doors;
    }

    public List<Control<T>> getDraggable() {
        List<Control<T>> draggable = new ArrayList<>();
        draggable.addAll(controls);
        draggable.addAll(doors);
        return draggable;
    }
}
