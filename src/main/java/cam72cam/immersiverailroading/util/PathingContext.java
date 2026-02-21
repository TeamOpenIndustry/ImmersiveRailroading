package cam72cam.immersiverailroading.util;

import cam72cam.mod.math.Vec3d;

/**
 * Packaged variables for pathing rail.
 * Notice that this uses UMC Vec3d.
 * @see trackapi.lib.PathingContext
 */
public class PathingContext {
    /**
     * nextPosition or currentPosition.
     */
    public final Vec3d pos;

    /**
     * distance between nextPosition and currentPosition.
     */
    public final double deltaMovement;

    /**
     * nextRoll or currentRoll.
     */
    public final double roll;

    public PathingContext(Vec3d pos, double deltaMovement, double roll) {
        this.pos = pos;
        this.roll = roll;
        this.deltaMovement = deltaMovement;
    }

    public PathingContext(trackapi.lib.PathingContext pathingContext) {
        this (
                new Vec3d(pathingContext.pos),
                pathingContext.deltaMovement,
                pathingContext.roll
        );
    }

    public trackapi.lib.PathingContext convert() {
        return new trackapi.lib.PathingContext(
                pos.internal(),
                deltaMovement,
                roll
        );
    }

//    private PathingContext(PathingContext pathingContext) {
//        this (
//                pathingContext.pos,
//                pathingContext.deltaMovement,
//                pathingContext.roll
//        );
//    }
//    public PathingContext copy() {
//        return new PathingContext(this);
//    }
//
//    public PathingContext withRoll (double newRoll) {
//        return new PathingContext(pos, deltaMovement, newRoll);
//    }
//
//    /**
//     * This simply return a PathingContext with only pos changed
//     */
//    public PathingContext withPos (Vec3d newPos) {
//        return new PathingContext(newPos, deltaMovement, roll);
//    }

    /**
     * This return a PathingContext with newPos and moved distance
     */
    public PathingContext toPosAndRoll(Vec3d newPos, double newRoll) {
        return new PathingContext(newPos, newPos.distanceTo(pos), newRoll);
    }
}
