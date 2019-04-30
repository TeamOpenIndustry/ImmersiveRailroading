package cam72cam.mod.entity.custom;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.math.Vec3d;

public interface IRidable {
    boolean canFitPassenger(Entity passenger);
    boolean shouldRiderSit(Entity ent);
    void updatePassenger(Entity passenger);
    Vec3d getMountPosition(Entity entity);
    Vec3d getDismountPosition(Entity ent);
    void onDismountPassenger(Entity entity);

    static IRidable get(Object o) {
        if (o instanceof IRidable) {
            return (IRidable)o;
        }
        return NOP;
    }
    IRidable NOP = new IRidable() {
        @Override
        public boolean canFitPassenger(Entity passenger) {
            return false;
        }

        @Override
        public boolean shouldRiderSit(Entity ent) {
            return false;
        }

        @Override
        public void updatePassenger(Entity passenger) {

        }

        @Override
        public Vec3d getMountPosition(Entity entity) {
            return Vec3d.ZERO;
        }

        @Override
        public Vec3d getDismountPosition(Entity ent) {
            return Vec3d.ZERO;
        }

        @Override
        public void onDismountPassenger(Entity entity) {

        }
    };
}
