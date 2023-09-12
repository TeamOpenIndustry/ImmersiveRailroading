package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.model.components.ModelComponent;
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
    private final float min;
    private final float max;



    private final float offset;
    private final boolean front;
    private Vec3d pos;

    private float toPointYaw;
    private float toPointPitch;
    private float atPointYaw;
    private final Matrix4 matrix;

    public TrackFollower(EntityMoveableRollingStock stock, ModelComponent frame, WheelSet wheels, boolean front) {
        this.stock = stock;
        this.offset = front ? stock.getDefinition().getBogeyFront(stock.gauge) : stock.getDefinition().getBogeyRear(stock.gauge);
        this.front = front;
        this.matrix = new Matrix4();

        if (wheels != null && wheels.wheels.size() > 1) {
            this.min = -(float) (wheels.wheels.stream().mapToDouble(w -> w.wheel.center.x).min().getAsDouble() * stock.gauge.scale());
            this.max = -(float) (wheels.wheels.stream().mapToDouble(w -> w.wheel.center.x).max().getAsDouble() * stock.gauge.scale());
        } else if (wheels != null && wheels.wheels.size() == 1) {
            this.min = -(float) (wheels.wheels.get(0).wheel.min.x * stock.gauge.scale());
            this.max = -(float) (wheels.wheels.get(0).wheel.max.x * stock.gauge.scale());
        } else if (frame != null) {
            this.min = -(float) (frame.min.x * stock.gauge.scale());
            this.max = -(float) (frame.max.x * stock.gauge.scale());
        } else {
            this.min = this.max = offset;
        }
    }

    public Matrix4 getMatrix() {
        double recomputeDist = 0.1 * stock.gauge.scale();
        if (pos == null || stock.getPosition().distanceToSquared(pos) > recomputeDist * recomputeDist) {
            pos = stock.getPosition();
            float offsetYaw = (front ? stock.getFrontYaw() : stock.getRearYaw());
            if (offset >= min && offset <= max) {
                matrix.setIdentity();
                matrix.translate(-offset, 0, 0);
                matrix.rotate(Math.toRadians(stock.getRotationYaw() - offsetYaw), 0, 1, 0);
                matrix.translate(offset, 0, 0);
            } else {
                // Don't need to path to a point that's already on the track.  TODO This can also be used to improve accuracy of the offset rendering
                Vec3d offsetPos = pos.add(VecUtil.fromWrongYaw(offset, stock.getRotationYaw()));
                double toMinPoint = max - offset;
                double betweenPoints = min - max;

                Vec3d pointPos = nextPosition(stock.getWorld(), stock.gauge, offsetPos, stock.getRotationYaw(), offsetYaw, toMinPoint);
                Vec3d pointPosNext = nextPosition(stock.getWorld(), stock.gauge, pointPos, stock.getRotationYaw(), offsetYaw, betweenPoints);
                Vec3d delta = stock.getPosition().subtract(pointPos).scale(min); // Scale copies sign
                if (pointPos.distanceTo(pointPosNext) > 0.1 * stock.gauge.scale()) {
                    toPointYaw = VecUtil.toYaw(delta) + stock.getRotationYaw() + 180;
                    atPointYaw = VecUtil.toYaw(pointPos.subtract(pointPosNext)) + stock.getRotationYaw() + 180 - toPointYaw ;

                    toPointPitch = -VecUtil.toPitch(VecUtil.rotateYaw(delta, stock.getRotationYaw() + 180)) + 90 + stock.getRotationPitch();
                } else {
                    pos = null; // Mark for re-compute
                    atPointYaw = 0;
                }

                matrix.setIdentity();
                matrix.rotate(Math.toRadians(toPointYaw), 0, 1, 0);
                matrix.rotate(Math.toRadians(toPointPitch), 0, 0, 1);
                matrix.translate(-max / stock.gauge.scale(), 0, 0);
                matrix.rotate(Math.toRadians(atPointYaw), 0, 1, 0);
                // TODO pitch
                matrix.translate(max / stock.gauge.scale(), 0, 0);
            }
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

    public float getYawReadout() {
        return offset >= min && offset <= max ? stock.getRotationYaw() - (front ? stock.getFrontYaw() : stock.getRearYaw()) : toPointYaw + atPointYaw;
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
