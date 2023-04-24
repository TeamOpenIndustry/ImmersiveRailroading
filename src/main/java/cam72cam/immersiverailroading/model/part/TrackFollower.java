package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.physics.MovementTrack;
import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.immersiverailroading.thirdparty.trackapi.ITrack;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;
import util.Matrix4;

import java.util.UUID;
import java.util.function.Function;

public class TrackFollower {
    private final EntityMoveableRollingStock stock;
    private final float offset;
    private Vec3d pos;

    private final Vec3d point;
    public float toPointYaw;
    float toPointPitch;
    public float atPointYaw;
    private Matrix4 matrix;

    public TrackFollower(EntityMoveableRollingStock stock, Vec3d point, float offset) {
        this.stock = stock;
        this.point = point;
        this.offset = offset;
    }

    public Matrix4 getMatrix() {
        double recomputeDist = 0.1 * stock.gauge.scale();

        if (pos == null || stock.getPosition().distanceToSquared(pos) > recomputeDist * recomputeDist) {
            Vec3d point = this.point.scale(stock.gauge.scale());

            pos = stock.getPosition();
            // Don't need to path to a point that's already on the track.  TODO This can also be used to improve accuracy of the offset rendering
            Vec3d offsetPos = pos.add(VecUtil.fromWrongYaw(offset, stock.getRotationYaw()));
            double distance = (-point.x) - offset;

            Vec3d pointPos = nextPosition(stock.getWorld(), stock.gauge, offsetPos, stock.getRotationYaw(), stock.getRotationYaw(), -0.5 * stock.gauge.scale() + distance);
            Vec3d pointPosNext = nextPosition(stock.getWorld(), stock.gauge, pointPos, stock.getRotationYaw(), stock.getRotationYaw(), 0.5 * stock.gauge.scale());
            Vec3d delta = pos.subtract(pointPos).scale(-point.x); // Scale copies sign
            if (pointPos.distanceTo(pointPosNext) > 0.1 * stock.gauge.scale()) {
                toPointYaw = VecUtil.toYaw(delta) + stock.getRotationYaw() + 180;
                atPointYaw = VecUtil.toYaw(pointPos.subtract(pointPosNext)) + stock.getRotationYaw() - toPointYaw + 180;
                toPointPitch = -VecUtil.toPitch(VecUtil.rotateYaw(delta, stock.getRotationYaw() + 180)) + 90 + stock.getRotationPitch();
            } else {
                pos = null; // Mark for re-compute
                atPointYaw = 0;
            }

            matrix = new Matrix4();
            matrix.rotate(Math.toRadians(toPointYaw), 0, 1, 0);
            matrix.rotate(Math.toRadians(toPointPitch), 0, 0, 1);
            matrix.translate(this.point.x, this.point.y, this.point.z);
            matrix.rotate(Math.toRadians(atPointYaw), 0, 1, 0);
            // TODO pitch
            matrix.translate(-this.point.x, -this.point.y, -this.point.z);
        }
        return matrix;
    }

    public Vec3d nextPosition(World world, Gauge gauge, Vec3d currentPosition, float rotationYaw, float bogeyYaw, double distance) {
        ITrack rail = MovementTrack.findTrack(world, currentPosition, rotationYaw, gauge.value());
        if (rail == null) {
            return currentPosition;
        }
        Vec3d result = rail.getNextPosition(currentPosition, VecUtil.fromWrongYaw(distance, bogeyYaw));
        if (result == null) {
            return currentPosition;
        }
        return result;
    }

    public static class TrackFollowers {
        private final ExpireableMap<UUID, TrackFollower> trackers = new ExpireableMap<>();
        private final Function<EntityMoveableRollingStock, TrackFollower> point;

        public TrackFollowers(Function<EntityMoveableRollingStock, TrackFollower> point) {
            this.point = point;
        }

        public TrackFollower get(EntityMoveableRollingStock stock) {
            TrackFollower tracker = trackers.get(stock.getUUID());
            if (tracker == null) {
                tracker = point.apply(stock);
                trackers.put(stock.getUUID(), tracker);
            }
            return tracker;
        }

        public void remove(EntityMoveableRollingStock stock) {
            trackers.put(stock.getUUID(), null);
        }
    }
}
