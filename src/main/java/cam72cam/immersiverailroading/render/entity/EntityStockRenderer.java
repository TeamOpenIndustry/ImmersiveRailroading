package cam72cam.immersiverailroading.render.entity;

import cam72cam.immersiverailroading.entity.EntityBuildableRollingStock;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;

public class EntityStockRenderer<D extends  EntityRollingStockDefinition> {
    protected final D definition;

    protected EntityBuildableRollingStock stock;
    protected boolean isBuilt;

    public EntityStockRenderer(D definition) {
        this.definition = definition;
    }

    public void render(EntityBuildableRollingStock stock) {
        this.stock = stock;
        this.isBuilt = stock.isBuilt();

    }
}
