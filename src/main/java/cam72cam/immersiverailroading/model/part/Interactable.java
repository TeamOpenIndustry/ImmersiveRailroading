package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;

// TODO rename to Widget?
public class Interactable<T extends EntityMoveableRollingStock> {
    public final ModelComponent part;
    public long lookedAt;

    public Interactable(ModelComponent part) {
        this.part = part;
    }

    public Vec3d center(EntityRollingStock stock) {
        return stock.getModelMatrix().apply(part.center);
    }

    public boolean disabled() {
        return false;
    }

    public IBoundingBox getBoundingBox(EntityRollingStock stock) {
        return IBoundingBox.from(
                stock.getModelMatrix().apply(part.min),
                stock.getModelMatrix().apply(part.max)
        );
    }
}
