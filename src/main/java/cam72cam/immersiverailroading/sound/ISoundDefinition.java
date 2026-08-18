package cam72cam.immersiverailroading.sound;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.util.DataBlock;

// TODO maybe rename?
public interface ISoundDefinition {
    void play(EntityMoveableRollingStock stock);
    void removed(EntityMoveableRollingStock stock);

    enum PlayState {
        INSIDE,
        OUTSIDE,
        BOTH
    }
}
