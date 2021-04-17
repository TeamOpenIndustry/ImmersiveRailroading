package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.physics.MovementSimulator;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import org.lwjgl.opengl.GL11;

public class TrackFollower {
    private final EntityMoveableRollingStock stock;
    private final MovementSimulator sim;
    private Vec3d pos;

    private final Vec3d point;
    float toPointYaw;
    float toPointPitch;
    float atPointYaw;

    public TrackFollower(EntityMoveableRollingStock stock, Vec3d point) {
        this.point = point;
        this.stock = stock;

        sim = new MovementSimulator(stock.getWorld(), stock.getCurrentTickPosOrFake(), stock.getDefinition().getBogeyFront(stock.gauge), stock.getDefinition().getBogeyRear(stock.gauge), stock.gauge.value());
    }

    public void apply() {
        if (!stock.getPosition().equals(pos)) {
            pos = stock.getPosition();

            // TODO atPointYaw instead of getRotationYaw(second)?
            Vec3d pointPos = sim.nextPosition(pos, stock.getRotationYaw(), stock.getRotationYaw(), -point.x);
            Vec3d pointPosNext = sim.nextPosition(pointPos, stock.getRotationYaw(), stock.getRotationYaw(), 0.5 * stock.gauge.scale());
            Vec3d delta = pos.subtract(pointPos).scale(-point.x); // Scale copies sign
            toPointYaw = VecUtil.toYaw(delta) + stock.getRotationYaw() + 180;
            toPointPitch = -VecUtil.toPitch(VecUtil.rotateYaw(delta, stock.getRotationYaw() + 180)) + 90 + stock.getRotationPitch();
            if (pointPos.distanceTo(pointPosNext) > 0.1 * stock.gauge.scale()) {
                atPointYaw = VecUtil.toYaw(pointPos.subtract(pointPosNext)) + stock.getRotationYaw() - toPointYaw + 180;
            } else {
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
}
