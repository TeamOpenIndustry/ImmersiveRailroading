package cam72cam.immersiverailroading.render.entity;

import cam72cam.immersiverailroading.entity.EntityBuildableRollingStock;
import cam72cam.immersiverailroading.model.RenderComponent;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.render.VBO;

import java.util.HashMap;
import java.util.Map;

public class EntityStockRenderer<D extends  EntityRollingStockDefinition> {
    protected final D definition;
    protected final Map<RenderComponent, VBO> components = new HashMap<>();

    protected EntityBuildableRollingStock stock;
    protected boolean isBuilt;

    public EntityStockRenderer(D definition) {
        this.definition = definition;
        for (RenderComponent component : definition.getComponents()) {

        }
    }

    public void render(EntityBuildableRollingStock stock) {
        this.stock = stock;
        this.isBuilt = stock.isBuilt();

    }
}
