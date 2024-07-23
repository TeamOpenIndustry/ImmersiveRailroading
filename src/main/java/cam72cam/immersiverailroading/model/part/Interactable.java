package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.model.animation.IAnimatable;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;

// TODO rename to Widget?
public class Interactable<A extends IAnimatable> {
    public final ModelComponent part;
    public long lookedAt;

    public Interactable(ModelComponent part) {
        this.part = part;
    }

    public Vec3d center(IAnimatable animatable) {
        return animatable.getModelMatrix().apply(part.center);
    }

    public boolean disabled() {
        return false;
    }

    public IBoundingBox getBoundingBox(IAnimatable animatable) {
        return IBoundingBox.from(
                animatable.getModelMatrix().apply(part.min),
                animatable.getModelMatrix().apply(part.max)
        );
    }
}
