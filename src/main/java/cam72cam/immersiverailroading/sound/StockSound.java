package cam72cam.immersiverailroading.sound;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;

public interface StockSound {
    void play(EntityMoveableRollingStock stock);
    void removed(EntityMoveableRollingStock stock);

    enum PlayState {
        INSIDE,
        OUTSIDE,
        BOTH
    }
}
