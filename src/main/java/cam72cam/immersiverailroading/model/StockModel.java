package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.*;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.render.ExpireableList;
import cam72cam.immersiverailroading.render.StockRenderCache;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.obj.OBJVBO;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class StockModel<T extends EntityMoveableRollingStock> extends OBJModel {
    private final EntityRollingStockDefinition def;
    public final List<ModelComponent> allComponents;
    private Frame frame;
    private Bogey bogeyFront;
    private Bogey bogeyRear;
    private ModelComponent shell;
    private ModelComponent remaining;
    private List<Door> doors;
    private List<Control> windows;
    private List<Control> widgets;
    private List<Control> independent_brakes;
    private List<Readout<T>> gauges;

    private List<LightFlare> headlights;

    private ExpireableList<UUID, TrackFollower> frontTrackers = null;
    private ExpireableList<UUID, TrackFollower> rearTrackers = null;
    private final boolean hasInterior;
    private List<Readout<T>> front_gauges;
    private List<Readout<T>> rear_gauges;

    public StockModel(EntityRollingStockDefinition def) throws Exception {
        super(def.modelLoc, def.darken, def.internal_model_scale, def.textureNames.keySet());
        this.def = def;
        this.hasInterior = this.groups().stream().anyMatch(x -> x.contains("INTERIOR"));

        ComponentProvider provider = new ComponentProvider(this);
        parseComponents(provider, def);
        provider.parse(ModelComponentType.IMMERSIVERAILROADING_BASE_COMPONENT);
        this.remaining = provider.parse(ModelComponentType.REMAINING);
        this.allComponents = provider.components();
    }

    protected void parseComponents(ComponentProvider provider, EntityRollingStockDefinition def) {
        gauges = new ArrayList<>();
        front_gauges = new ArrayList<>();
        rear_gauges = new ArrayList<>();
        if (def.hasIndependentBrake()) {
            gauges.addAll(
                    Readout.getReadouts(provider, ModelComponentType.GAUGE_INDEPENDENT_BRAKE_X, Readouts.INDEPENDENT_BRAKE)
            );
        }
        gauges.addAll(Readout.getReadouts(provider, ModelComponentType.BRAKE_PRESSURE_X, Readouts.BRAKE_PRESSURE));
        front_gauges.addAll(Readout.getReadouts(provider, ModelComponentType.BRAKE_PRESSURE_POS_X, "BOGEY_FRONT", Readouts.BRAKE_PRESSURE));
        rear_gauges.addAll(Readout.getReadouts(provider, ModelComponentType.BRAKE_PRESSURE_POS_X, "BOGEY_REAR", Readouts.BRAKE_PRESSURE));

        this.frame = new Frame(provider, def.defID, def.getValveGear());
        this.shell = provider.parse(ModelComponentType.SHELL);
        this.bogeyFront = Bogey.get(provider, unifiedBogies(), "FRONT");
        this.bogeyRear = Bogey.get(provider, unifiedBogies(), "REAR");
        this.headlights = LightFlare.get(def, provider, ModelComponentType.HEADLIGHT_X);
        this.doors = Door.get(provider);
        this.windows = Control.get(provider, ModelComponentType.WINDOW_X);
        this.widgets = Control.get(provider, ModelComponentType.WIDGET_X);

        if (bogeyFront != null && Math.abs(def.getBogeyFront(Gauge.from(Gauge.STANDARD)) + bogeyFront.center().x) > 0.5) {
            frontTrackers = new ExpireableList<>();
        }
        if (bogeyRear != null && Math.abs(def.getBogeyRear(Gauge.from(Gauge.STANDARD)) + bogeyRear.center().x) > 0.5) {
            rearTrackers = new ExpireableList<>();
        }

        independent_brakes = def.hasIndependentBrake() ?
                Control.get(provider, ModelComponentType.INDEPENDENT_BRAKE_X) :
                Collections.emptyList();
    }

    protected boolean unifiedBogies() {
        return true;
    }


    public final void onClientTick(EntityMoveableRollingStock stock) {
        effects((T) stock);
    }

    protected void effects(T stock) {
        headlights.forEach(x -> x.effects(stock, 0));
        getDraggableComponents().forEach(c -> c.effects(stock));
        getReadouts().forEach(c -> c.effects(stock));
        front_gauges.forEach(c -> c.effects(stock));
        rear_gauges.forEach(c -> c.effects(stock));
    }

    public final void onClientRemoved(EntityMoveableRollingStock stock) {
        removed((T) stock);
    }

    protected void removed(T stock) {
        headlights.forEach(x -> x.removed(stock));
    }

    public final void render(EntityMoveableRollingStock stock, float partialTicks) {
        OBJRender render = StockRenderCache.getRender(def.defID);

        List<ModelComponentType> available = stock.isBuilt() ? null : stock.getItemComponents()
                .stream().flatMap(x -> x.render.stream())
                .collect(Collectors.toList());

        try (
                OBJVBO.BoundOBJVBO bound = render.bind();
                OpenGL.With tex = render.bindTexture(stock.getTexture());
                OpenGL.With light = OpenGL.bool(GL11.GL_LIGHTING, true);
                OpenGL.With cull = OpenGL.bool(GL11.GL_CULL_FACE, false);
                OpenGL.With normals = OpenGL.bool(GL11.GL_NORMALIZE, true);
        ) {
            double distanceTraveled = stock.distanceTraveled + stock.getCurrentSpeed().minecraft() * stock.getTickSkew() * partialTicks * 1.1;
            distanceTraveled /= stock.gauge.scale();

            try (ComponentRenderer draw = new ComponentRenderer(stock, bound, available, hasInterior)) {
                GL11.glScaled(stock.gauge.scale(), stock.gauge.scale(), stock.gauge.scale());
                //noinspection unchecked
                render((T) stock, draw, distanceTraveled);
            }
        }
    }

    public void postRender(EntityMoveableRollingStock stock, float partialTicks) {
        postRender((T) stock);
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
                if (frontTrackers != null) {
                    TrackFollower data = frontTrackers.get(stock.getUUID());
                    if (data == null) {
                        data = new TrackFollower(bogeyFront.center());
                        frontTrackers.put(stock.getUUID(), data);
                    }
                    data.apply(stock, matrix);
                } else {
                    matrix.translate(-def.getBogeyFront(Gauge.standard()), 0, 0);
                    matrix.rotate(stock.getRotationYaw() - stock.getFrontYaw(), 0, 1, 0);
                    matrix.translate(def.getBogeyFront(Gauge.standard()), 0, 0);
                }
                bogeyFront.render(distanceTraveled, matrix);
                front_gauges.forEach(r -> r.render(stock, matrix));
            }
        }

        if (bogeyRear != null) {
            try (ComponentRenderer matrix = draw.push()) {
                if (rearTrackers != null) {
                    TrackFollower data = rearTrackers.get(stock.getUUID());
                    if (data == null) {
                        data = new TrackFollower(bogeyRear.center());
                        rearTrackers.put(stock.getUUID(), data);
                    }
                    data.apply(stock, matrix);
                } else {
                    matrix.translate(-def.getBogeyRear(Gauge.standard()), 0, 0);
                    matrix.rotate(stock.getRotationYaw() - stock.getRearYaw(), 0, 1, 0);
                    matrix.translate(def.getBogeyRear(Gauge.standard()), 0, 0);
                }
                bogeyRear.render(distanceTraveled, matrix);
                rear_gauges.forEach(r -> r.render(stock, matrix));
            }
        }
    }

    protected void renderWithInteriorLighting(T stock, ComponentRenderer draw) {
        draw.render(shell);
        draw.render(remaining);

        getReadouts().forEach(r -> r.render(stock, draw));

        getDraggableComponents().forEach(c -> c.render(stock, draw));
    }

    protected void postRender(T stock) {
        getDraggableComponents().forEach(c -> c.postRender(stock));
        getReadouts().forEach(c -> c.postRender(stock));
        headlights.forEach(x -> x.postRender(stock, 0));
    }

    public List<Control> getDraggableComponents() {
        List<Control> components = new ArrayList<>();
        components.addAll(doors);
        components.addAll(windows);
        components.addAll(widgets);
        components.addAll(independent_brakes);
        return components;
    }

    public List<Readout<T>> getReadouts() {
        return gauges;
    }

    public List<Door> getDoors() {
        return new ArrayList<>(doors);
    }
}
