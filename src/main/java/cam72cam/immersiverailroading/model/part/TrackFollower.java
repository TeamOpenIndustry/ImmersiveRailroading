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
    private Vec3d pos;

    private final Vec3d point;
    float toPointYaw;
    float toPointPitch;
    float atPointYaw;
    private Matrix4 matrix;

    public TrackFollower(EntityMoveableRollingStock stock, Vec3d point) {
        this.stock = stock;
        this.point = point;
    }

    public Matrix4 getMatrix() {
        Vec3d point = this.point.scale(stock.gauge.scale());

        if (!stock.getPosition().equals(pos)) {
            pos = stock.getPosition();

            Vec3d startPos = VecUtil.fromWrongYaw(-point.x, stock.getRotationYaw()).add(stock.getPosition());
            Vec3d pointPos = nextPosition(stock.getWorld(), stock.gauge, startPos, stock.getRotationYaw(), stock.getRotationYaw(), -0.5 * stock.gauge.scale());
            if (startPos.equals(pointPos)) {
                pointPos = nextPosition(stock.getWorld(), stock.gauge, pos, stock.getRotationYaw(), stock.getRotationYaw(), -0.5 * stock.gauge.scale() - point.x);
            }
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
        private final Function<EntityMoveableRollingStock, Vec3d> point;

        public TrackFollowers(Function<EntityMoveableRollingStock, Vec3d> point) {
            this.point = point;
        }

        public TrackFollower get(EntityMoveableRollingStock stock) {
            TrackFollower tracker = trackers.get(stock.getUUID());
            if (tracker == null) {
                tracker = new TrackFollower(stock, point.apply(stock));
                trackers.put(stock.getUUID(), tracker);
            }
            return tracker;
        }

        public void remove(EntityMoveableRollingStock stock) {
            trackers.put(stock.getUUID(), null);
        }
    }
}
