package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.Bogey;
import cam72cam.immersiverailroading.model.part.Frame;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.model.part.TrackFollower;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.render.ExpireableList;
import cam72cam.immersiverailroading.render.StockRenderCache;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.render.VBO;
import cam72cam.mod.render.obj.OBJRender;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.stream.Collectors;

public class StockModel<T extends EntityMoveableRollingStock> extends OBJModel {
    private final EntityRollingStockDefinition def;
    public final List<ModelComponent> allComponents;
    private Frame frame;
    private Bogey bogeyFront;
    private Bogey bogeyRear;
    private ModelComponent shell;
    private ModelComponent remaining;

    private ExpireableList<UUID, TrackFollower> frontTrackers = null;
    private ExpireableList<UUID, TrackFollower> rearTrackers = null;

    public StockModel(EntityRollingStockDefinition def) throws Exception {
        super(def.modelLoc, def.darken, def.internal_model_scale, def.textureNames.keySet());
        this.def = def;

        ComponentProvider provider = new ComponentProvider(this);
        parseComponents(provider, def);
        this.remaining = provider.parse(RenderComponentType.REMAINING);
        this.allComponents = provider.components();
    }

    protected void parseComponents(ComponentProvider provider, EntityRollingStockDefinition def) {
        this.frame = new Frame(provider);
        this.shell = provider.parse(RenderComponentType.SHELL);
        this.bogeyFront = Bogey.get(provider, unifiedBogies(), "FRONT");
        this.bogeyRear = Bogey.get(provider, unifiedBogies(), "REAR");

        if (bogeyFront != null && Math.abs(def.getBogeyFront(Gauge.from(Gauge.STANDARD)) + bogeyFront.center().x) > 0.5) {
            frontTrackers = new ExpireableList<>();
        }
        if (bogeyRear != null && Math.abs(def.getBogeyRear(Gauge.from(Gauge.STANDARD)) + bogeyRear.center().x) > 0.5) {
            rearTrackers = new ExpireableList<>();
        }
    }

    protected boolean unifiedBogies() {
        return true;
    }

    public final void render(EntityMoveableRollingStock stock, float partialTicks) {
        OBJRender render = StockRenderCache.getRender(def.defID);

        List<RenderComponentType> available = stock.isBuilt() ? null : stock.getItemComponents()
                .stream().flatMap(x -> x.render.stream())
                .collect(Collectors.toList());

        try (
                VBO.BoundVBO bound = render.bind();
                OpenGL.With tex = render.bindTexture(stock.getTexture());
                OpenGL.With light = OpenGL.bool(GL11.GL_LIGHTING, true);
                OpenGL.With cull = OpenGL.bool(GL11.GL_CULL_FACE, false);
        ) {
            double distanceTraveled = stock.distanceTraveled + stock.getCurrentSpeed().minecraft() * stock.getTickSkew() * partialTicks * 1.1;
            distanceTraveled /= stock.gauge.scale();

            try (ComponentRenderer draw = new ComponentRenderer(bound, available)) {
                GL11.glScaled(stock.gauge.scale(), stock.gauge.scale(), stock.gauge.scale());
                //noinspection unchecked
                render((T) stock, draw, distanceTraveled);
            }
        }
    }

    protected void render(T stock, ComponentRenderer draw, double distanceTraveled) {
        frame.render(distanceTraveled, draw);

        draw.render(shell);


        if (bogeyFront != null) {
            try (ComponentRenderer matrix = draw.push()) {
                if (frontTrackers != null) {
                    TrackFollower data = frontTrackers.get(stock.getUUID());
                    if (data == null) {
                        data = new TrackFollower(stock, bogeyFront.center());
                        frontTrackers.put(stock.getUUID(), data);
                    }
                    data.apply();
                } else {
                    GL11.glTranslated(-def.getBogeyFront(stock.gauge), 0, 0);
                    GL11.glRotated(stock.getRotationYaw() - stock.getFrontYaw(), 0, 1, 0);
                    GL11.glTranslated(def.getBogeyFront(stock.gauge), 0, 0);
                }
                bogeyFront.render(distanceTraveled, matrix);
            }
        }

        if (bogeyRear != null) {
            try (ComponentRenderer matrix = draw.push()) {
                if (rearTrackers != null) {
                    TrackFollower data = rearTrackers.get(stock.getUUID());
                    if (data == null) {
                        data = new TrackFollower(stock, bogeyRear.center());
                        rearTrackers.put(stock.getUUID(), data);
                    }
                    data.apply();
                } else {
                    GL11.glTranslated(-def.getBogeyRear(stock.gauge), 0, 0);
                    GL11.glRotated(stock.getRotationYaw() - stock.getRearYaw(), 0, 1, 0);
                    GL11.glTranslated(def.getBogeyRear(stock.gauge), 0, 0);
                }
                bogeyRear.render(distanceTraveled, matrix);
            }
        }

        draw.render(remaining);
    }
}
