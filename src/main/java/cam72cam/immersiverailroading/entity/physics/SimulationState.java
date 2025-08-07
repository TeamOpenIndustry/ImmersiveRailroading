package cam72cam.immersiverailroading.entity.physics;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.entity.Tender;
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
import cam72cam.mod.util.FastMath;
import cam72cam.mod.world.World;

import java.util.*;
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

    public double directResistance;

    public Configuration config;
    public boolean dirty = true;
    public boolean atRest = true;
    public double collided;
    public boolean sliding;
    public boolean frontPushing;
    public boolean frontPulling;
    public boolean rearPushing;
    public boolean rearPulling;
    public Consist consist;
    public float slackFrontPercent;
    public float slackRearPercent;

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
        public double rollingResistanceCoefficient;
        private final Function<List<Vec3i>, Double> directResistanceNewtons;

        // We don't actually want to use this value, it's only for dirty checking
        private double tractiveEffortFactors;
        private Function<Speed, Double> tractiveEffortNewtons;

        public Double desiredBrakePressure;
        public double independentBrakePosition;

        public boolean hasPressureBrake;

        public Configuration(EntityCoupleableRollingStock stock) {
            id = stock.getUUID();
            gauge = stock.gauge;
            world = stock.getWorld();

            width = stock.getDefinition().getWidth(gauge);
            length = stock.getDefinition().getLength(gauge);
            height = stock.getDefinition().getHeight(gauge);
            double pitchOffset = (Math.abs(Math.sin(Math.toRadians(stock.getRotationPitch())) * length));
            bounds = s -> stock.getDefinition().getBounds(s.yaw, gauge)
                    .offset(s.position.add(0, -(s.position.y % 1) - pitchOffset, 0))
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
            // When FuelRequired is false, most of the time the locos are empty.  Work around that here
            double designMassKg = !Config.ConfigBalance.FuelRequired && (stock instanceof Locomotive || stock instanceof Tender) ? massKg : stock.getMaxWeight();

            if (stock instanceof Locomotive) {
                Locomotive locomotive = (Locomotive) stock;
                tractiveEffortNewtons = locomotive::getTractiveEffortNewtons;
                tractiveEffortFactors = locomotive.getThrottle() + (locomotive.getReverser() * 10);
                desiredBrakePressure = (double)locomotive.getTrainBrake();
            } else {
                tractiveEffortNewtons = speed -> 0d;
                tractiveEffortFactors = 0;
                desiredBrakePressure = null;
            }


            double staticFriction = PhysicalMaterials.STEEL.staticFriction(PhysicalMaterials.STEEL);
            this.maximumAdhesionNewtons = massKg * staticFriction * 9.8 * stock.getBrakeAdhesionEfficiency();
            this.designAdhesionNewtons = designMassKg * staticFriction * 9.8 * stock.getBrakeSystemEfficiency();
            this.independentBrakePosition = stock.getIndependentBrake();
            this.directResistanceNewtons = stock::getDirectFrictionNewtons;
            this.hasPressureBrake = stock.getDefinition().hasPressureBrake();

            this.rollingResistanceCoefficient = stock.getDefinition().rollingResistanceCoefficient;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Configuration) {
                Configuration other = (Configuration) o;
                return couplerEngagedFront == other.couplerEngagedFront &&
                        couplerEngagedRear == other.couplerEngagedRear &&
                        Math.abs(tractiveEffortFactors - other.tractiveEffortFactors) < 0.01 &&
                        Math.abs(massKg - other.massKg)/massKg < 0.01 &&
                        (desiredBrakePressure == null || Math.abs(desiredBrakePressure - other.desiredBrakePressure) < 0.001) &&
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

        consist = stock.consist;

        // If we just placed it, need to adjust it.  Otherwise, it already existed and is just loading in
        dirty = stock.newlyPlaced;
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

        this.bounds = prev.bounds;

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
        directResistance = prev.directResistance;

        slackFrontPercent = prev.slackFrontPercent;
        slackRearPercent = prev.slackRearPercent;

        consist = prev.consist;
    }

    public void calculateCouplerPositions() {
        Vec3d bogeyFront = VecUtil.fromWrongYawPitch(config.offsetFront, yaw, pitch);
        Vec3d bogeyRear = VecUtil.fromWrongYawPitch(config.offsetRear, yaw, pitch);

        Vec3d positionFront = couplerPositionFront = position.add(bogeyFront);
        Vec3d positionRear = couplerPositionRear = position.add(bogeyRear);

        ITrack trackFront = MovementTrack.findTrack(config.world, positionFront, yaw, config.gauge.value());
        ITrack trackRear = MovementTrack.findTrack(config.world, positionRear, yaw, config.gauge.value());

        if (trackFront != null && trackRear != null) {
            Vec3d couplerVecFront = VecUtil.fromWrongYaw(config.couplerDistanceFront - config.offsetFront, yawFront);
            Vec3d couplerVecRear = VecUtil.fromWrongYaw(config.couplerDistanceRear - config.offsetRear, yawRear);

            couplerPositionFront = trackFront.getNextPosition(positionFront, couplerVecFront);
            couplerPositionRear = trackRear.getNextPosition(positionRear, couplerVecRear);
            //couplerPositionFront = couplerPositionFront.subtract(position).normalize().scale(Math.abs(config.couplerDistanceFront)).add(position);
            //couplerPositionRear = couplerPositionRear.subtract(position).normalize().scale(Math.abs(config.couplerDistanceRear)).add(position);
        }
        if (Objects.equals(couplerPositionFront, positionFront)) {
            couplerPositionFront = position.add(VecUtil.fromWrongYaw(config.couplerDistanceFront, yaw));
        }
        if (Objects.equals(couplerPositionRear, positionRear)) {
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
                    if (bp.y >= position.y - (position.y % 1)) { // Prevent it from breaking blocks under the pitched train (bb expanded)
                        interferingBlocks.add(bp);
                        interferingResistance += config.world.getBlockHardness(bp);
                    }
                }
            }
        }
    }

    public SimulationState next() {
        SimulationState next = new SimulationState(this);
        next.dirty = false;
        return next;
    }

    public SimulationState next(double distance, List<Vec3i> blocksAlreadyBroken) {
        SimulationState next = new SimulationState(this);
        next.moveAlongTrack(distance);
        if (this.position.equals(next.position)) {
            next.velocity = 0;
        } else {
            next.calculateCouplerPositions();
            next.bounds = next.config.bounds.apply(next);

            // We will actually break the blocks
            this.blocksToBreak = this.interferingBlocks;
            // We can now ignore those positions for the rest of the simulation
            blocksAlreadyBroken.addAll(this.blocksToBreak);

            // Calculate the next states interference
            double minDist = Math.max(0.5, Math.abs(velocity*4));
            if (next.recalculatedAt.distanceToSquared(next.position) > minDist * minDist) {
                next.calculateBlockCollisions(blocksAlreadyBroken);
                next.recalculatedAt = next.position;
                next.directResistance = config.directResistanceNewtons.apply(trackToUpdate);
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

        boolean isTable = false;
        if (Math.abs(distance) < 0.0001) {
            TileRailBase frontBase = trackFront instanceof TileRailBase ? (TileRailBase) trackFront : null;
            TileRailBase rearBase  = trackRear instanceof TileRailBase ? (TileRailBase) trackRear : null;
            isTable = checkTileType(frontBase, TrackItems.TURNTABLE)
                      || checkTileType(rearBase, TrackItems.TURNTABLE)
                      || checkTileType(frontBase, TrackItems.TRANSFERTABLE)
                      || checkTileType(rearBase, TrackItems.TRANSFERTABLE);
            if (!isTable) {
                return;
            }
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
            pitch = (float) Math.toDegrees(FastMath.atan2(bogeyDelta.y, nextRear.distanceTo(nextFront)));
            // TODO Rescale fixes issues with curves losing precision, but breaks when correcting stock positions
            position = position.add(deltaCenter/*.normalize().scale(distance)*/);
        }

        if (isReversed) {
            yawFront += 180;
            yawRear += 180;
        }

        if (isTable) {
            yawFront = yaw;
            yawRear = yaw;
        }

        // Fix bogeys pointing in opposite directions
        if (DegreeFuncs.delta(yawFront, yaw) > 90 || DegreeFuncs.delta(yawFront, yawRear) > 90) {
            yawFront = yaw;
            yawRear = yaw;
        }
    }

    public double forcesNewtons() {
        double gradeForceNewtons = config.massKg * -9.8 * Math.sin(Math.toRadians(pitch)) * Config.ConfigBalance.slopeMultiplier;
        return config.tractiveEffortNewtons(Speed.fromMinecraft(velocity)) + gradeForceNewtons;
    }

    public boolean atRest() {
        return velocity == 0 && Math.abs(forcesNewtons()) < frictionNewtons();
    }

    public double frictionNewtons() {
        // https://evilgeniustech.com/idiotsGuideToRailroadPhysics/OtherLocomotiveForces/#rolling-resistance
        double rollingResistanceNewtons = config.rollingResistanceCoefficient * (config.massKg * 9.8);
        // https://www.arema.org/files/pubs/pgre/PGChapter2.pdf
        // ~15 lb/ton -> 0.01 weight ratio -> 0.001 uS with gravity
        double startingFriction = velocity == 0 ? 0.001 * config.massKg * 9.8 : 0;
        // TODO This is kinda directional?
        double blockResistanceNewtons = interferingResistance * 1000 * Config.ConfigDamage.blockHardness;

        double brakeAdhesionNewtons = config.designAdhesionNewtons * Math.min(1, Math.max(brakePressure, config.independentBrakePosition));

        this.sliding = false;
        if (brakeAdhesionNewtons > config.maximumAdhesionNewtons && Math.abs(velocity) > 0.01) {
            // WWWWWHHHEEEEE!!! SLIDING!!!!
            double kineticFriction = PhysicalMaterials.STEEL.kineticFriction(PhysicalMaterials.STEEL);
            brakeAdhesionNewtons = config.massKg * kineticFriction;
            this.sliding = true;
        }

        brakeAdhesionNewtons *= Config.ConfigBalance.brakeMultiplier;

        return rollingResistanceNewtons + blockResistanceNewtons + brakeAdhesionNewtons + directResistance + startingFriction;
    }

    private boolean checkTileType(TileRailBase base, TrackItems type) {
        return base != null
                && base.getParentTile() != null
                && base.getParentTile().info.settings.type == type;
    }
}
