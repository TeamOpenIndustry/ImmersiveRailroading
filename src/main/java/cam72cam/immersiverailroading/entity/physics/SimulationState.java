package cam72cam.immersiverailroading.entity.physics;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.entity.physics.chrono.ServerChronoState;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.PhysicalMaterials;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.physics.MovementTrack;
import cam72cam.immersiverailroading.thirdparty.trackapi.ITrack;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.DegreeFuncs;
import cam72cam.mod.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class SimulationState {
    public int tickID;

    public Vec3d position;
    public double velocity;
    public float yaw;
    public float pitch;
    public IBoundingBox bounds;

    // Render purposes
    public float yawFront;
    public float yawRear;

    public Vec3d couplerPositionFront;
    public Vec3d couplerPositionRear;
    public UUID interactingFront;
    public UUID interactingRear;

    public float brakePressure;

    public Vec3d recalculatedAt;
    // All positions in the stock bounds
    public List<Vec3i> collidingBlocks;
    // Any track within those bounds
    public List<Vec3i> trackToUpdate;
    // Blocks that the stock would need to break to move
    public List<Vec3i> interferingBlocks;
    // How much force required to break the interfering blocks
    public float interferingResistance;
    // Blocks that were actually broken and need to be removed
    public List<Vec3i> blocksToBreak;

    public Configuration config;
    public boolean dirty = true;
    public boolean canBeUnloaded = true;

    public static class Configuration {
        public UUID id;
        public Gauge gauge;
        public World world;

        public double width;
        public double length;
        public double height;
        public Function<SimulationState, IBoundingBox> bounds;

        public float offsetFront;
        public float offsetRear;

        public boolean couplerEngagedFront;
        public boolean couplerEngagedRear;
        public double couplerDistanceFront;
        public double couplerDistanceRear;
        public double couplerSlackFront;
        public double couplerSlackRear;

        public double massKg;

        public double maximumAdhesionNewtons;
        public double designAdhesionNewtons;

        // We don't actually want to use this value, it's only for dirty checking
        private double tractiveEffortFactors;
        private Function<Speed, Double> tractiveEffortNewtons;

        public double desiredBrakePressure;
        public double independentBrakePosition;

        public boolean hasPressureBrake;

        public Configuration(EntityCoupleableRollingStock stock) {
            id = stock.getUUID();
            gauge = stock.gauge;
            world = stock.getWorld();

            width = stock.getDefinition().getWidth(gauge);
            length = stock.getDefinition().getLength(gauge);
            height = stock.getDefinition().getHeight(gauge);
            bounds = s -> stock.getDefinition().getBounds(s.yaw, gauge)
                    .offset(s.position.add(0, -(s.position.y % 1), 0))
                    .contract(new Vec3d(0, 0, 0.5 * gauge.scale()));
                    //.contract(new Vec3d(0, 0.5 * this.gauge.scale(), 0))
                    //.offset(new Vec3d(0, 0.5 * this.gauge.scale(), 0));

            offsetFront = stock.getDefinition().getBogeyFront(gauge);
            offsetRear = stock.getDefinition().getBogeyRear(gauge);

            couplerEngagedFront = stock.isCouplerEngaged(EntityCoupleableRollingStock.CouplerType.FRONT);
            couplerEngagedRear = stock.isCouplerEngaged(EntityCoupleableRollingStock.CouplerType.BACK);
            couplerDistanceFront = stock.getDefinition().getCouplerPosition(EntityCoupleableRollingStock.CouplerType.FRONT, gauge);
            couplerDistanceRear = -stock.getDefinition().getCouplerPosition(EntityCoupleableRollingStock.CouplerType.BACK, gauge);
            couplerSlackFront = stock.getDefinition().getCouplerSlack(EntityCoupleableRollingStock.CouplerType.FRONT, gauge);
            couplerSlackRear = stock.getDefinition().getCouplerSlack(EntityCoupleableRollingStock.CouplerType.BACK, gauge);

            this.massKg = stock.getWeight();
            double designMassKg = stock.getMaxWeight();

            if (stock instanceof Locomotive) {
                Locomotive locomotive = (Locomotive) stock;
                tractiveEffortNewtons = locomotive::getTractiveEffortNewtons;
                tractiveEffortFactors = locomotive.getThrottle() + (locomotive.getReverser() * 10);
                desiredBrakePressure = locomotive.getTrainBrake();
            } else {
                tractiveEffortNewtons = speed -> 0d;
                tractiveEffortFactors = 0;
                desiredBrakePressure = 0;
            }


            double staticFriction = PhysicalMaterials.STEEL.staticFriction(PhysicalMaterials.STEEL);
            this.maximumAdhesionNewtons = massKg * staticFriction;
            this.designAdhesionNewtons = designMassKg * staticFriction * stock.getBrakeSystemEfficiency();
            this.independentBrakePosition = stock.getIndependentBrake();
            this.hasPressureBrake = stock.getDefinition().hasPressureBrake();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Configuration) {
                Configuration other = (Configuration) o;
                return couplerEngagedFront == other.couplerEngagedFront &&
                        couplerEngagedRear == other.couplerEngagedRear &&
                        Math.abs(tractiveEffortFactors - other.tractiveEffortFactors) < 0.01 &&
                        Math.abs(massKg - other.massKg)/massKg < 0.01 &&
                        Math.abs(desiredBrakePressure - other.desiredBrakePressure) < 0.01 &&
                        Math.abs(independentBrakePosition - other.independentBrakePosition) < 0.01;
            }
            return false;
        }

        public double tractiveEffortNewtons(Speed speed) {
            return this.tractiveEffortNewtons.apply(speed);
        }
    }

    public SimulationState(EntityCoupleableRollingStock stock) {
        tickID = ServerChronoState.getState(stock.getWorld()).getServerTickID();
        position = stock.getPosition();
        velocity = stock.getVelocity().length() *
                (DegreeFuncs.delta(VecUtil.toWrongYaw(stock.getVelocity()), stock.getRotationYaw()) < 90 ? 1 : -1);
        yaw = stock.getRotationYaw();
        pitch = stock.getRotationPitch();

        interactingFront = stock.getCoupledUUID(EntityCoupleableRollingStock.CouplerType.FRONT);
        interactingRear = stock.getCoupledUUID(EntityCoupleableRollingStock.CouplerType.BACK);

        brakePressure = stock.getBrakePressure();

        config = new Configuration(stock);

        bounds = config.bounds.apply(this);

        yawFront = stock.getFrontYaw();
        yawRear = stock.getRearYaw();

        recalculatedAt = position;

        calculateCouplerPositions();

        calculateBlockCollisions(Collections.emptyList());
        blocksToBreak = Collections.emptyList();
    }

    private SimulationState(SimulationState prev) {
        this.tickID = prev.tickID + 1;
        this.position = prev.position;
        this.velocity = prev.velocity;
        this.yaw = prev.yaw;
        this.pitch = prev.pitch;

        this.interactingFront = prev.interactingFront;
        this.interactingRear = prev.interactingRear;

        this.brakePressure = prev.brakePressure;

        this.config = prev.config;

        this.bounds = config.bounds.apply(this);

        this.yawFront = prev.yawFront;
        this.yawRear = prev.yawRear;
        couplerPositionFront = prev.couplerPositionFront;
        couplerPositionRear = prev.couplerPositionRear;

        recalculatedAt = prev.recalculatedAt;
        collidingBlocks = prev.collidingBlocks;
        trackToUpdate = prev.trackToUpdate;
        interferingBlocks = prev.interferingBlocks;
        interferingResistance = prev.interferingResistance;
        blocksToBreak = Collections.emptyList();
    }

    public void calculateCouplerPositions() {
        Vec3d bogeyFront = VecUtil.fromWrongYawPitch(config.offsetFront, yaw, pitch);
        Vec3d bogeyRear = VecUtil.fromWrongYawPitch(config.offsetRear, yaw, pitch);

        Vec3d positionFront = position.add(bogeyFront);
        Vec3d positionRear = position.add(bogeyRear);

        ITrack trackFront = MovementTrack.findTrack(config.world, positionFront, yaw, config.gauge.value());
        ITrack trackRear = MovementTrack.findTrack(config.world, positionRear, yaw, config.gauge.value());

        if (trackFront != null && trackRear != null) {
            Vec3d couplerVecFront = VecUtil.fromWrongYaw(config.couplerDistanceFront - config.offsetFront, yawFront);
            Vec3d couplerVecRear = VecUtil.fromWrongYaw(config.couplerDistanceRear - config.offsetRear, yawRear);

            couplerPositionFront = trackFront.getNextPosition(positionFront, couplerVecFront);
            couplerPositionRear = trackRear.getNextPosition(positionRear, couplerVecRear);
        } else {
            couplerPositionFront = position.add(VecUtil.fromWrongYaw(config.couplerDistanceFront, yaw));
            couplerPositionRear = position.add(VecUtil.fromWrongYaw(config.couplerDistanceRear, yaw));
        }
    }

    public void calculateBlockCollisions(List<Vec3i> blocksAlreadyBroken) {
        this.collidingBlocks = config.world.blocksInBounds(this.bounds);
        this.trackToUpdate = new ArrayList<>();
        this.interferingBlocks = new ArrayList<>();
        this.interferingResistance = 0;

        for (Vec3i bp : collidingBlocks) {
            if (blocksAlreadyBroken.contains(bp)) {
                continue;
            }

            if (BlockUtil.isIRRail(config.world, bp)) {
                trackToUpdate.add(bp);
            } else {
                if (Config.ConfigDamage.TrainsBreakBlocks && !BlockUtil.isIRRail(config.world, bp.up())) {
                    interferingBlocks.add(bp);
                    interferingResistance += config.world.getBlockHardness(bp);
                }
            }
        }
    }

    public SimulationState next(double distance, List<Vec3i> blocksAlreadyBroken) {
        SimulationState next = new SimulationState(this);
        next.moveAlongTrack(distance);
        if (this.position.equals(next.position)) {
            next.velocity = 0;
        } else {
            next.calculateCouplerPositions();
            // We will actually break the blocks
            this.blocksToBreak = this.interferingBlocks;
            // We can now ignore those positions for the rest of the simulation
            blocksAlreadyBroken.addAll(this.blocksToBreak);

            // Calculate the next states interference
            double minDist = Math.max(0.5, Math.abs(velocity*4));
            if (next.recalculatedAt.distanceToSquared(next.position) > minDist * minDist) {
                next.calculateBlockCollisions(blocksAlreadyBroken);
                next.recalculatedAt = next.position;
            } else {
                // We put off calculating collisions for now
                next.interferingBlocks = Collections.emptyList();
                next.interferingResistance = 0;
            }
        }
        return next;
    }

    public void update(EntityCoupleableRollingStock stock) {
        Configuration oldConfig = config;
        config = new Configuration(stock);
        dirty = dirty || !config.equals(oldConfig);
    }

    private void moveAlongTrack(double distance) {
        Vec3d positionFront = VecUtil.fromWrongYawPitch(config.offsetFront, yaw, pitch).add(position);
        Vec3d positionRear = VecUtil.fromWrongYawPitch(config.offsetRear, yaw, pitch).add(position);

        // Find tracks
        ITrack trackFront = MovementTrack.findTrack(config.world, positionFront, yawFront, config.gauge.value());
        ITrack trackRear = MovementTrack.findTrack(config.world, positionRear, yawRear, config.gauge.value());
        if (trackFront == null || trackRear == null) {
            return;
        }

        if (Math.abs(distance) < 0.0001) {
            boolean isTurnTable;

            TileRailBase frontBase = trackFront instanceof TileRailBase ? (TileRailBase) trackFront : null;
            TileRailBase rearBase  = trackRear instanceof TileRailBase ? (TileRailBase) trackRear : null;
            isTurnTable = frontBase != null &&
                    (
                            frontBase.getTicksExisted() < 100 ||
                                    frontBase.getParentTile() != null &&
                                            frontBase.getParentTile().info.settings.type == TrackItems.TURNTABLE
                    );
            isTurnTable = isTurnTable || rearBase != null &&
                    (
                            rearBase.getTicksExisted() < 100 ||
                                    rearBase.getParentTile() != null &&
                                            rearBase.getParentTile().info.settings.type == TrackItems.TURNTABLE
                    );

            if (!isTurnTable) {
                return;
            }
        }

        // Fix bogeys pointing in opposite directions
        if (DegreeFuncs.delta(yawFront, yaw) > 90) {
            yawFront = yaw;
        }
        if (DegreeFuncs.delta(yawRear, yaw) > 90) {
            yawRear = yaw;
        }

        boolean isReversed = distance < 0;
        if (isReversed) {
            distance = -distance;
            yawFront += 180;
            yawRear += 180;
        }

        Vec3d nextFront = trackFront.getNextPosition(positionFront, VecUtil.fromWrongYaw(distance, yawFront));
        Vec3d nextRear = trackRear.getNextPosition(positionRear, VecUtil.fromWrongYaw(distance, yawRear));

        if (!nextFront.equals(positionFront) && !nextRear.equals(positionRear)) {
            yawFront = VecUtil.toWrongYaw(nextFront.subtract(positionFront));
            yawRear = VecUtil.toWrongYaw(nextRear.subtract(positionRear));

            // TODO flatten this vector calculation
            Vec3d deltaCenter = nextFront.subtract(position).scale(config.offsetRear)
                    .subtract(nextRear.subtract(position).scale(config.offsetFront))
                    .scale(-1/(config.offsetFront-config.offsetRear));

            Vec3d bogeyDelta = nextFront.subtract(nextRear);
            yaw = VecUtil.toWrongYaw(bogeyDelta);
            pitch = (float) Math.toDegrees(Math.atan2(bogeyDelta.y, nextRear.distanceTo(nextFront)));
            // TODO Rescale fixes issues with curves losing precision, but breaks when correcting stock positions
            position = position.add(deltaCenter/*.normalize().scale(distance)*/);
        }

        if (isReversed) {
            yawFront += 180;
            yawRear += 180;
        }
    }

    public double forcesNewtons() {
        double gradeForceNewtons = config.massKg * -9.8 * Math.sin(Math.toRadians(pitch)) * Config.ConfigBalance.slopeMultiplier;
        return config.tractiveEffortNewtons(Speed.fromMinecraft(velocity)) + gradeForceNewtons;
    }

    public double frictionNewtons() {
        // https://evilgeniustech.com/idiotsGuideToRailroadPhysics/OtherLocomotiveForces/#rolling-resistance
        double rollingResistanceNewtons = 0.002 * (config.massKg * 9.8);
        // TODO This is kinda directional?
        double blockResistanceNewtons = interferingResistance * 1000 * Config.ConfigDamage.blockHardness;

        double brakeAdhesionNewtons = config.designAdhesionNewtons * Math.min(1, Math.max(brakePressure, config.independentBrakePosition));

        if (brakeAdhesionNewtons > config.maximumAdhesionNewtons && Math.abs(velocity) > 0.1) {
            // WWWWWHHHEEEEE!!! SLIDING!!!!
            double kineticFriction = PhysicalMaterials.STEEL.kineticFriction(PhysicalMaterials.STEEL);
            brakeAdhesionNewtons = config.massKg * kineticFriction;
        }

        brakeAdhesionNewtons *= Config.ConfigBalance.brakeMultiplier;

        return rollingResistanceNewtons + blockResistanceNewtons + brakeAdhesionNewtons;
    }
}
