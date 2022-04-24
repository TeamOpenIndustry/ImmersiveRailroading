package cam72cam.immersiverailroading.physics;

import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.immersiverailroading.physics.Units.*;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import net.minecraft.block.BlockLiquid;

import java.util.ArrayList;
import java.util.List;


public class StockState {
    /* Constants */

    final EntityCoupleableRollingStock stock;

    final Vec3d position;
    final Velocity velocity;
    final Force traction;
    final Mass mass;
    final Vec3d couplerFront;
    final Vec3d couplerRear;


    // Degrees
    final float yaw;
    final float pitch;

    /* Accumulators */
    final BogeyForce bogeyFront;
    final BogeyForce bogeyRear;

    Force collision = Force.ZERO;

    // New broken per tick
    List<Vec3i> blocksBroken;
    Acceleration blockAcceleration;

    public StockState(EntityCoupleableRollingStock stock) {
        this.stock = stock;

        this.velocity = Velocity.fromMT(stock.getVelocity());
        this.position = stock.getPosition();
        this.yaw = stock.getRotationYaw();
        this.pitch = stock.getRotationPitch();
        this.couplerFront = VecUtil.fromWrongYawPitch(
                (float)stock.getDefinition().getCouplerPosition(EntityCoupleableRollingStock.CouplerType.FRONT, stock.gauge),
                yaw,
                pitch
        ).add(position);
        this.couplerRear = VecUtil.fromWrongYawPitch(
                -(float)stock.getDefinition().getCouplerPosition(EntityCoupleableRollingStock.CouplerType.BACK, stock.gauge),
                yaw,
                pitch
        ).add(position);


        if (stock instanceof Locomotive) {
            Locomotive loco = (Locomotive) stock;
            traction = Force.fromNewtons(
                    VecUtil.fromWrongYawPitch((float)loco.getTractiveEffortNewtons(stock.getCurrentSpeed()), yaw, pitch)
            );
        } else {
            traction = Force.fromNewtons(Vec3d.ZERO);
        }


        this.mass = Mass.fromKG(stock.getWeight());
        this.blocksBroken = new ArrayList<>();

        Vec3d positionFront = VecUtil.fromWrongYawPitch(stock.getDefinition().getBogeyFront(stock.gauge), yaw, pitch).add(position);
        Vec3d positionRear = VecUtil.fromWrongYawPitch(stock.getDefinition().getBogeyRear(stock.gauge), yaw, pitch).add(position);

        Vec3d prev = stock.getTickCount() > 0 ? position.subtract(stock.getVelocity()) : position;
        Vec3d prevPositionFront = VecUtil.fromWrongYawPitch(stock.getDefinition().getBogeyFront(stock.gauge), stock.getPrevRotationYaw(), stock.getPrevRotationPitch()).add(prev);
        Vec3d prevPositionRear = VecUtil.fromWrongYawPitch(stock.getDefinition().getBogeyRear(stock.gauge), stock.getPrevRotationYaw(), stock.getPrevRotationPitch()).add(prev);
        Vec3d frontDelta = positionFront.subtract(prevPositionFront);
        Vec3d rearDelta = positionRear.subtract(prevPositionRear);

        Mass bogeyMass = mass.scale(0.5);
        Force bogeyTraction = traction.scale(0.5);

        bogeyFront = new BogeyForce(positionFront, Velocity.fromMT(frontDelta), bogeyTraction, bogeyMass);
        bogeyRear = new BogeyForce(positionRear, Velocity.fromMT(rearDelta), bogeyTraction, bogeyMass);
    }

    public StockState(StockState prev) {
        Force frontForce = prev.bogeyFront.totalForce().add(prev.collision.scale(0.5));
        Force rearForce = prev.bogeyRear.totalForce().add(prev.collision.scale(0.5));

        Velocity velocityFront = prev.bogeyFront.velocity.add(
                frontForce.toAcceleration(prev.bogeyFront.mass).deltaVelocity()
        );
        Velocity velocityRear = prev.bogeyRear.velocity.add(
                rearForce.toAcceleration(prev.bogeyRear.mass).deltaVelocity()
        );

        Vec3d frontNext = prev.bogeyFront.position.add(velocityFront.asMT());
        Vec3d rearNext = prev.bogeyRear.position.add(velocityRear.asMT());

        this.yaw = VecUtil.toWrongYaw(frontNext.subtract(rearNext));
        this.pitch = VecUtil.toPitch(rearNext.rotateYaw(-this.yaw).subtract(frontNext.rotateYaw(-this.yaw))) - 90;
        this.velocity = prev.velocity.add(frontForce.add(rearForce).toAcceleration(prev.mass).deltaVelocity());
        this.position = velocity.applyTo(prev.position);

        this.stock = prev.stock;
        if (stock instanceof Locomotive) {
            Locomotive loco = (Locomotive) stock;
            traction = Force.fromNewtons(
                    VecUtil.fromWrongYawPitch((float)loco.getTractiveEffortNewtons(stock.getCurrentSpeed()), yaw, pitch)
            );
        } else {
            traction = Force.fromNewtons(Vec3d.ZERO);
        }
        this.mass = prev.mass;
        this.blocksBroken = new ArrayList<>();

        this.couplerFront = VecUtil.fromWrongYawPitch(
                (float)stock.getDefinition().getCouplerPosition(EntityCoupleableRollingStock.CouplerType.FRONT, stock.gauge),
                yaw,
                pitch
        ).add(position);
        this.couplerRear = VecUtil.fromWrongYawPitch(
                -(float)stock.getDefinition().getCouplerPosition(EntityCoupleableRollingStock.CouplerType.BACK, stock.gauge),
                yaw,
                pitch
        ).add(position);


        Vec3d positionFront = VecUtil.fromWrongYawPitch(stock.getDefinition().getBogeyFront(stock.gauge), this.yaw, this.pitch).add(this.position);
        Vec3d positionRear = VecUtil.fromWrongYawPitch(stock.getDefinition().getBogeyRear(stock.gauge), this.yaw, this.pitch).add(this.position);
        Mass bogeyMass = mass.scale(0.5);
        Force bogeyTraction = traction.scale(0.5);
        bogeyFront = new BogeyForce(positionFront, Velocity.fromMT(positionFront.subtract(prev.bogeyFront.position)), bogeyTraction, bogeyMass);
        bogeyRear = new BogeyForce(positionRear, Velocity.fromMT(positionRear.subtract(prev.bogeyRear.position)), bogeyTraction, bogeyMass);
    }

    public void calculateIndependentAccelerations() {
        bogeyFront.independentForces(stock.getWorld());
        bogeyRear.independentForces(stock.getWorld());
    }

    private Vec3d calculateOverlap(Vec3d otherCouplerPos, Vec3d myCouplerPos, Vec3d center) {
        if (otherCouplerPos.distanceToSquared(center) < myCouplerPos.distanceToSquared(center)) {
            // Overlap!
            return otherCouplerPos.subtract(myCouplerPos);
        }
        return null;
    }

    private Vec3d calculateOverlap(Vec3d otherCouplerPos) {
        Vec3d frontCheck = calculateOverlap(otherCouplerPos, couplerFront, position);
        return frontCheck != null ? frontCheck : calculateOverlap(otherCouplerPos, couplerRear, position);
    }

    public void calculateStockInteractions(List<StockState> states) {
        for (StockState state : states) {
            if (state == this || state.collision != Force.ZERO) {
                continue;
            }

            // Check for overlap
            if (calculateOverlap(state.couplerFront) != null) {
                System.out.println(stock.getWorld().getTicks());
                System.out.println("BANG Front!");
            }
            if (calculateOverlap(state.couplerRear) != null) {
                System.out.println(stock.getWorld().getTicks());
                System.out.println("BANG Rear!");
            }

            if (calculateOverlap(state.couplerFront) != null || calculateOverlap(state.couplerRear) != null) {
                Force fSelf = new Force(Acceleration.fromMTT(velocity.asMT()), mass);
                Force fOther = new Force(Acceleration.fromMTT(state.velocity.asMT()), state.mass);
                Force fTotal = fSelf.add(fOther).invert();
                System.out.println(fTotal.toNewtons());
                collision = collision.add(fTotal);
                state.collision = state.collision.add(fTotal);
            }
        }
    }

    public StockState compute() {
        return new StockState(this);
    }

    public void apply() {
        stock.state = this;
        stock.setPosition(position);
        stock.setVelocity(velocity.asMT());
        stock.setRotationYaw(yaw);
        stock.setRotationPitch(pitch);
    }

    public static class BogeyForce {
        private final Vec3d position;
        private final Mass mass;
        private final Force gravity;
        private final Velocity velocity;
        private Force traction;
        private Force groundCollision;
        private Force trackSides;

        private BogeyForce(Vec3d position, Velocity velocity, Force traction, Mass mass) {
            this.position = position;
            this.velocity = velocity;
            this.gravity = new Force(Acceleration.GRAVITY, mass);
            this.traction = traction;
            this.mass = mass;
            this.groundCollision = Force.ZERO;
            this.trackSides = Force.ZERO;
        }

        public void independentForces(World world) {
            Double yLowerLimit = null;

            TileRailBase track = world.getBlockEntity(new Vec3i(position), TileRailBase.class);
            if (track != null) {
                // Redirect inertia
                Vec3d velocity = this.velocity.asMT();
                Vec3d corrected = track.getNextPosition(position, velocity);

                // Set lower y limit to tracked
                yLowerLimit = corrected.y;

                // Relative
                corrected = corrected.subtract(position);

                // Remove y component
                corrected = new Vec3d(corrected.x, 0, corrected.z);
                velocity = new Vec3d(velocity.x, 0, velocity.z);

                // Scale to original length
                corrected = corrected.scale(velocity.length()/corrected.length());

                // Cancel out xz inertia and add redirected xz component
                trackSides = new Force(Acceleration.fromMTT(corrected.subtract(velocity)), mass);
            } else if (!world.isAir(new Vec3i(position)) && !(world.internal.getBlockState(new Vec3i(position).internal()).getBlock() instanceof BlockLiquid)) {
                yLowerLimit = (double) new Vec3i(position).up().y;
            }

            if (yLowerLimit != null && yLowerLimit > position.y) {
                this.groundCollision = new Force(Acceleration.fromMTT(new Vec3d(0, yLowerLimit - position.y, 0)), mass);
            } else {
                traction = Force.ZERO;
            }
        }

        public Force totalForce() {
            return gravity.add(groundCollision).add(traction).add(trackSides);
        }
    }
}
