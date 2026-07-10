package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.physics.MovementTrack;
import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.immersiverailroading.thirdparty.trackapi.ITrack;
import cam72cam.immersiverailroading.thirdparty.trackapi.IRPathingData;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.util.DegreeFuncs;
import cam72cam.mod.world.World;
import util.Matrix4;

import java.util.UUID;
import java.util.function.Function;

public class TrackFollower {
    private final EntityMoveableRollingStock stock;
    private final float max;
    private final float min;



    private final float offset;
    private final boolean front;
    private Vec3d pos;
    private float yawReadout;
    private float rollReadout;//Bogey from the whole body
    private final Matrix4 matrix;

    public TrackFollower(EntityMoveableRollingStock stock, ModelComponent frame, WheelSet wheels, boolean front) {
        this.stock = stock;
        this.offset = front ? stock.getDefinition().getBogeyFront(stock.gauge) : stock.getDefinition().getBogeyRear(stock.gauge);
        this.front = front;
        this.matrix = new Matrix4();

        if (wheels != null && wheels.wheels.size() > 1) {
            this.max = -(float) (wheels.wheels.stream().mapToDouble(w -> w.wheel.center.x).min().getAsDouble() * stock.gauge.scale());
            this.min = -(float) (wheels.wheels.stream().mapToDouble(w -> w.wheel.center.x).max().getAsDouble() * stock.gauge.scale());
        } else if (wheels != null && wheels.wheels.size() == 1) {
            this.max = -(float) (wheels.wheels.getFirst().wheel.min.x * stock.gauge.scale());
            this.min = -(float) (wheels.wheels.getFirst().wheel.max.x * stock.gauge.scale());
        } else if (frame != null) {
            this.max = -(float) (frame.min.x * stock.gauge.scale());
            this.min = -(float) (frame.max.x * stock.gauge.scale());
        } else {
            this.max = this.min = offset;
        }
    }

    public Matrix4 getMatrix() {
        //Outer matrix is scaled
        double recomputeDist = 0.1 * stock.gauge.scale();
        if (pos == null || stock.getPosition().distanceToSquared(pos) > recomputeDist * recomputeDist) {
            pos = stock.getPosition();
            float offsetYaw = (front ? stock.getFrontYaw() : stock.getRearYaw());
            float offsetRoll = (front ? stock.getFrontRoll() : stock.getRearRoll());
            if (offset >= min && offset <= max) {
                yawReadout = stock.getRotationYaw() - offsetYaw;
                rollReadout = offsetRoll - stock.getRotationRoll();
                matrix.setIdentity();
                matrix.translate(-offset / stock.gauge.scale(), 0, 0);
                matrix.rotate(Math.toRadians(rollReadout), 1, 0, 0);
                matrix.rotate(Math.toRadians(yawReadout), 0, 1, 0);
                matrix.translate(offset / stock.gauge.scale(), 0, 0);
            } else {
                // Don't need to path to a point that's already on the track.  TODO This can also be used to improve accuracy of the offset rendering
                Vec3d offsetPos = pos.add(VecUtil.fromWrongYawPitch(offset, stock.getRotationYaw(), stock.getRotationPitch()));
                double toMinPoint = min - offset;
                double betweenPoints = max - min;

                float toPointYaw = 0;
                float atPointYaw = 0;
                float toPointPitch = 0;
                float atPointPitch = 0;

                IRPathingData pointPos = new IRPathingData(offsetPos, (toMinPoint < 0 ? rollReadout : -rollReadout) + stock.getRotationRoll());
                nextPosition(stock.getWorld(), stock.gauge, pointPos, stock.getRotationYaw(), offsetYaw, toMinPoint);

                IRPathingData pointPosNext = pointPos.clone();// pointPos need to be retained
                nextPosition(stock.getWorld(), stock.gauge, pointPosNext, stock.getRotationYaw(), offsetYaw, betweenPoints);

                Vec3d delta = stock.getPosition().subtract(pointPos.getUMCPos()).scale(max); // Scale copies sign
                if (pointPos.getUMCPos().distanceTo(pointPosNext.getUMCPos()) > 0.1 * stock.gauge.scale()) {
                    toPointYaw = VecUtil.toYaw(delta) + stock.getRotationYaw() + 180;
                    atPointYaw = VecUtil.toYaw(pointPos.getUMCPos().subtract(pointPosNext.getUMCPos())) + stock.getRotationYaw() + 180 - toPointYaw ;

                    toPointPitch = -VecUtil.toPitch(VecUtil.rotateYaw(delta, stock.getRotationYaw() + 180)) + 90 + stock.getRotationPitch();
                    atPointPitch = -VecUtil.toPitch(VecUtil.rotateYaw(pointPos.getUMCPos().subtract(pointPosNext.getUMCPos()), stock.getRotationYaw() + 180)) + 90 + stock.getRotationPitch() - toPointPitch;
                } else {
                    pos = null; // Force recompute
                }

                yawReadout = toPointYaw + atPointYaw;

                rollReadout = (float) (toMinPoint < 0 ? pointPos.getRoll() : -pointPos.getRoll()) - stock.getRotationRoll();// TODO: pointPosNext might be more accurate, but need to fix sign issue
                if(toMinPoint < 0) {
                    rollReadout = -rollReadout;
                }

                float min = this.min;
                // TODO This implies the code above is broken, but works around some of the weirder edge cases.
                if (DegreeFuncs.delta(0, toPointYaw) > 90) {
                    min = -min;
                    toPointYaw = toPointYaw - 180;
                }
                if (DegreeFuncs.delta(0, atPointYaw) > 90) {
                    atPointYaw -= 180;
                    min = -min;
                }

                matrix.setIdentity();
                matrix.rotate(Math.toRadians(toPointYaw), 0, 1, 0);
                matrix.rotate(Math.toRadians(toPointPitch), 0, 0, 1);
                matrix.rotate(Math.toRadians(rollReadout), 1, 0, 0);
                matrix.translate(-min / stock.gauge.scale(), 0, 0);
                matrix.rotate(Math.toRadians(atPointYaw), 0, 1, 0);
                matrix.rotate(Math.toRadians(atPointPitch), 0, 0, 1);
                matrix.translate(min / stock.gauge.scale(), 0, 0);
            }
        }
        return matrix;
    }

    //Notice that we use bogeyYaw and distance to construct motion direction, so both of them affect plus-minus sign of roll
    public void nextPosition(World world, Gauge gauge, IRPathingData currentPosition, float rotationYaw, float bogeyYaw, double distance) {
        ITrack rail = MovementTrack.findTrack(world, currentPosition.getUMCPos(), rotationYaw, gauge.value());
        if (rail == null) {
            return;
        }
        rail.getNextPosition(currentPosition, VecUtil.fromWrongYaw(distance, bogeyYaw), rail.getTrackGauges()[0]);
    }

    public float getYawReadout() {
        return yawReadout;
    }
    //TODO Do we need delta roll readout?
    public float getRollReadout() {
        return rollReadout;
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
