package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.physics.MovementTrack;
import cam72cam.immersiverailroading.thirdparty.trackapi.ITrack;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;
import org.lwjgl.opengl.GL11;

public class TrackFollower {
    private Vec3d pos;

    private final Vec3d point;
    float toPointYaw;
    float toPointPitch;
    float atPointYaw;

    public TrackFollower(Vec3d point) {
        this.point = point;
    }

    public void apply(EntityMoveableRollingStock stock) {
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
        }

        GL11.glRotated(toPointYaw, 0, 1, 0);
        GL11.glRotated(toPointPitch, 0, 0, 1);
        GL11.glTranslated(point.x, point.y, point.z);
        GL11.glRotated(atPointYaw, 0, 1, 0);
        // TODO pitch
        GL11.glTranslated(-point.x, -point.y, -point.z);
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

    public float getYaw() {
        return toPointYaw + atPointYaw;
    }
}
