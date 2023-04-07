package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.physics.SimulationState;
import cam72cam.immersiverailroading.entity.physics.chrono.ChronoState;
import cam72cam.immersiverailroading.entity.physics.chrono.ServerChronoState;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.immersiverailroading.physics.TickPos;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.RealBB;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.custom.ICollision;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.sound.ISound;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class EntityMoveableRollingStock extends EntityRidableRollingStock implements ICollision {

    public static final String DAMAGE_SOURCE_HIT = "immersiverailroading:hitByTrain";
    public static final String DAMAGE_SOURCE_HIT_IN_DARKNESS = "immersiverailroading:hitByTrainInDarkness";

    @TagField("frontYaw")
    private Float frontYaw;
    @TagField("rearYaw")
    private Float rearYaw;
    @TagField("distanceTraveled")
    public double distanceTraveled = 0;
    private Speed currentSpeed;
    @TagField(value = "positions", mapper = TickPos.ListTagMapper.class)
    public List<TickPos> positions = new ArrayList<>();
    public List<SimulationState> states = new ArrayList<>();
    private RealBB boundingBox;
    private float[][] heightMapCache;
    @TagSync
    @TagField("IND_BRAKE")
    private float independentBrake = 0;

    @TagSync
    @TagField("TOTAL_BRAKE")
    private float totalBrake = 0;

    private float sndRand;

    private ISound wheel_sound;
    private ISound clackFront;
    private ISound clackRear;
    private Vec3i clackFrontPos;
    private Vec3i clackRearPos;

    private double swayMagnitude;
    private double swayImpulse;

    private HashSet<TileRailBase> railCache = new HashSet<>();

    @Override
    public void load(TagCompound data) {
        super.load(data);

        if (frontYaw == null) {
            frontYaw = getRotationYaw();
        }
        if (rearYaw == null) {
            rearYaw = getRotationYaw();
        }
    }

    public void initPositions(TickPos tp) {
        this.positions = new ArrayList<>();
        this.positions.add(tp);
    }

    /*
     * Entity Overrides for BB
     */

    public void clearHeightMap() {
        this.heightMapCache = null;
        this.boundingBox = null;
    }

    private float[][] getHeightMap() {
        if (this.heightMapCache == null) {
            this.heightMapCache = this.getDefinition().createHeightMap(this);
        }
        return this.heightMapCache;
    }

    @Override
    public RealBB getCollision() {
        if (this.boundingBox == null) {
            this.boundingBox = this.getDefinition().getBounds(this.getRotationYaw(), this.gauge)
                    .offset(getPosition())
                    .withHeightMap(this.getHeightMap())
                    .contract(new Vec3d(0, 0.5 * this.gauge.scale(), 0)).offset(new Vec3d(0, 0.5 * this.gauge.scale(), 0));
        }
        return this.boundingBox;
    }

    /*
     * Speed Info
     */

    public Speed getCurrentSpeed() {
        if (currentSpeed == null) {
            //Fallback
            // does not work for curves
            Vec3d motion = this.getVelocity();
            float speed = (float) Math.sqrt(motion.x * motion.x + motion.y * motion.y + motion.z * motion.z);
            if (Float.isNaN(speed)) {
                speed = 0;
            }
            currentSpeed = Speed.fromMinecraft(speed);
        }
        return currentSpeed;
    }

    public void setCurrentSpeed(Speed newSpeed) {
        this.currentSpeed = newSpeed;
    }

    /** This is where fun network synchronization is handled
     * So normally every 2 seconds we get a new packet with stock positional information for the next 4 seconds
     */
    public void handleTickPosPacket(List<TickPos> newPositions) {

        if (newPositions.size() != 0) {
            this.clearPositionCache();

            if (ChronoState.getState(getWorld()) == null) {
                positions.clear();
            } else {
                int tickID = (int) Math.floor(ChronoState.getState(getWorld()).getTickID());
                List<Integer> newIds = newPositions.stream().map(p -> p.tickID).collect(Collectors.toList());
                positions.removeAll(positions.stream()
                        // old OR far in the future OR to be replaced
                        .filter(p -> p.tickID < tickID - 30 || p.tickID > tickID + 60 || newIds.contains(p.tickID))
                        .collect(Collectors.toList())
                );
            }
            // unordered
            positions.addAll(newPositions);
        }
    }

    public SimulationState getCurrentState() {
        int tickID = ServerChronoState.getState(getWorld()).getServerTickID();
        for (SimulationState state : states) {
            if (state.tickID == tickID) {
                return state;
            }
        }
        return null;
    }

    public TickPos getTickPos() {
        if (ChronoState.getState(getWorld()) == null) {
            return null;
        }
        double tick = ChronoState.getState(getWorld()).getTickID();
        int currentTickID = (int) Math.floor(tick);
        int nextTickID = (int) Math.ceil(tick);
        TickPos current = null;
        TickPos next = null;

        for (TickPos position : positions) {
            if (position.tickID == currentTickID) {
                current = position;
            }
            if (position.tickID == nextTickID) {
                next = position;
            }
            if (current != null && next != null) {
                break;
            }
        }
        if (current == null) {
            return null;
        }
        if (next == null || current == next || getWorld().isServer) {
            return current;
        }
        // Skew
        return TickPos.skew(current, next, tick);
    }

    @Override
    public void onDrag(Control<?> control, double newValue) {
        super.onDrag(control, newValue);
        switch (control.part.type) {
            case INDEPENDENT_BRAKE_X:
                if (getDefinition().isLinearBrakeControl()) {
                    setIndependentBrake(getControlPosition(control));
                }
                break;
        }
    }

    @Override
    public void onDragRelease(Control<?> control) {
        super.onDragRelease(control);
        if (!getDefinition().isLinearBrakeControl() && control.part.type == ModelComponentType.INDEPENDENT_BRAKE_X) {
            setControlPosition(control, 0.5f);
        }
    }

    @Override
    protected float defaultControlPosition(Control<?> control) {
        switch (control.part.type) {
            case INDEPENDENT_BRAKE_X:
                return getDefinition().isLinearBrakeControl() ? 0 : 0.5f;
            default:
                return super.defaultControlPosition(control);
        }
    }

    @Override
    public void onTick() {
        super.onTick();

        if (getWorld().isServer) {
            if (getDefinition().hasIndependentBrake()) {
                for (Control<?> control : getDefinition().getModel().getControls()) {
                    if (!getDefinition().isLinearBrakeControl() && control.part.type == ModelComponentType.INDEPENDENT_BRAKE_X) {
                        setIndependentBrake(Math.max(0, Math.min(1, getIndependentBrake() + (getControlPosition(control) - 0.5f) / 8)));
                    }
                }
            }

            if (this.getTickCount() % 10 == 0) {
                // Wipe this now and again to force a refresh
                // Could also be implemented as a wipe from the track rail base (might be more efficient?)
                lastRetarderPos = null;
            }

            if (this.getTickCount() % 5 == 0) {
                float trainBrake = 0;
                if (this instanceof EntityCoupleableRollingStock) {
                    // This could be slow, but I don't want to do this properly till the next despaghettification
                    trainBrake = (float) ((EntityCoupleableRollingStock) this).getDirectionalTrain(false).stream()
                            .map(m -> m.stock)
                            .map(s -> s instanceof Locomotive ? (Locomotive) s : null)
                            .filter(Objects::nonNull)
                            .mapToDouble(Locomotive::getTrainBrake)
                            .max().orElse(0);

                }
                this.totalBrake = Math.min(1, Math.max(getIndependentBrake(), trainBrake));
            }



            SimulationState state = getCurrentState();
            if (state != null) {
                for (Vec3i bp : state.blocksToBreak) {
                    getWorld().breakBlock(bp, Config.ConfigDamage.dropSnowBalls || !getWorld().isSnow(bp));
                }
                HashSet<TileRailBase> oldRailCache = railCache;
                railCache = new HashSet<>();
                for (Vec3i bp : state.trackToUpdate) {
                    TileRailBase te = getWorld().getBlockEntity(bp, TileRailBase.class);
                    if (te != null) {
                        te.cleanSnow();
                        te.stockOverhead(this);
                        railCache.add(te);
                    }
                }
                for (TileRailBase te : oldRailCache) {
                    if (!railCache.contains(te)) {
                        te.stockOverhead(null);
                    }
                }
            }
        }

        if (getWorld().isClient) {
            getDefinition().getModel().onClientTick(this);


            if (ConfigSound.soundEnabled) {
                if (this.wheel_sound == null) {
                    wheel_sound = this.createSound(this.getDefinition().wheel_sound, true, 40);
                    this.sndRand = (float) Math.random() / 10;
                }
                if (this.clackFront == null) {
                    clackFront = this.createSound(this.getDefinition().clackFront, false, 30);
                }
                if (this.clackRear == null) {
                    clackRear = this.createSound(this.getDefinition().clackRear, false, 30);
                }
                float adjust = (float) Math.abs(this.getCurrentSpeed().metric()) / 300;
                float pitch = adjust + 0.7f;
                if (getDefinition().shouldScalePitch()) {
                    // TODO this is probably wrong...
                    pitch = (float) (pitch/ gauge.scale());
                }
                float volume = 0.01f + adjust;

                if (Math.abs(this.getCurrentSpeed().metric()) > 5 && MinecraftClient.getPlayer().getPosition().distanceTo(getPosition()) < 40) {
                    if (!wheel_sound.isPlaying()) {
                        wheel_sound.play(getPosition());
                    }
                    wheel_sound.setPitch(pitch + this.sndRand);
                    wheel_sound.setVolume(volume);

                    wheel_sound.setPosition(getPosition());
                    wheel_sound.setVelocity(getVelocity());
                    wheel_sound.update();
                } else {
                    if (wheel_sound.isPlaying()) {
                        wheel_sound.stop();
                    }
                }

                volume = Math.min(1, volume * 2);
                swayMagnitude -= 0.07;
                double swayMin = getCurrentSpeed().metric() / 300 / 3;
                swayMagnitude = Math.max(swayMagnitude, swayMin);

                if (swayImpulse > 0) {
                    swayMagnitude += 0.3;
                    swayImpulse -= 0.7;
                }
                swayMagnitude = Math.min(swayMagnitude, 3);

                Vec3i posFront = new Vec3i(VecUtil.fromWrongYawPitch(getDefinition().getBogeyFront(gauge), getRotationYaw(), getRotationPitch()).add(getPosition()));
                if (BlockUtil.isIRRail(getWorld(), posFront)) {
                    TileRailBase rb = getWorld().getBlockEntity(posFront, TileRailBase.class);
                    rb = rb != null ? rb.getParentTile() : null;
                    if (rb != null && !rb.getPos().equals(clackFrontPos) && rb.clacks()) {
                        if (!clackFront.isPlaying() && !clackRear.isPlaying()) {
                            clackFront.setPitch(pitch);
                            clackFront.setVolume(volume);
                            clackFront.play(new Vec3d(posFront));
                        }
                        clackFrontPos = rb.getPos();
                        if (getWorld().getTicks() % ConfigGraphics.StockSwayChance == 0) {
                            swayImpulse += 7 * rb.getBumpiness();
                            swayImpulse = Math.min(swayImpulse, 20);
                        }
                    }
                }
                Vec3i posRear = new Vec3i(VecUtil.fromWrongYawPitch(getDefinition().getBogeyRear(gauge), getRotationYaw(), getRotationPitch()).add(getPosition()));
                if (BlockUtil.isIRRail(getWorld(), posRear)) {
                    TileRailBase rb = getWorld().getBlockEntity(posRear, TileRailBase.class);
                    rb = rb != null ? rb.getParentTile() : null;
                    if (rb != null && !rb.getPos().equals(clackRearPos) && rb.clacks()) {
                        if (!clackFront.isPlaying() && !clackRear.isPlaying()) {
                            clackRear.setPitch(pitch);
                            clackRear.setVolume(volume);
                            clackRear.play(new Vec3d(posRear));
                        }
                        clackRearPos = rb.getPos();
                    }
                }
            }
        }

        // Apply position onTick
        TickPos currentPos = getTickPos();
        if (currentPos == null) {
            // Not loaded yet or not moving
            return;
        }

        Vec3d prevPos = this.getPosition();
        double prevPosX = prevPos.x;
        double prevPosY = prevPos.y;
        double prevPosZ = prevPos.z;

        this.setRotationYaw(currentPos.rotationYaw);
        this.setRotationPitch(currentPos.rotationPitch);
        this.frontYaw = currentPos.frontYaw;
        this.rearYaw = currentPos.rearYaw;

        this.currentSpeed = currentPos.speed;

        distanceTraveled += (float) this.currentSpeed.minecraft() * getTickSkew();
        distanceTraveled = distanceTraveled % 32000;// Wrap around to prevent double float issues

        this.setPosition(currentPos.position);
        this.setVelocity(getPosition().subtract(prevPosX, prevPosY, prevPosZ));

        if (this.getVelocity().length() > 0.001) {
            this.clearPositionCache();
        }

        if (Math.abs(this.getCurrentSpeed().metric()) > 1) {
			List<Entity> entitiesWithin = getWorld().getEntities((Entity entity) -> (entity.isLiving() || entity.isPlayer()) && this.getCollision().intersects(entity.getBounds()), Entity.class);
			for (Entity entity : entitiesWithin) {
				if (entity instanceof EntityMoveableRollingStock) {
					// rolling stock collisions handled by looking at the front and
					// rear coupler offsets
					continue;
				} 
	
				if (entity.getRiding() instanceof EntityMoveableRollingStock) {
					// Don't apply bb to passengers
					continue;
				}
				
				if (entity.isPlayer()) {
					if (entity.getTickCount() < 20 * 5) {
						// Give the internal a chance to getContents out of the way
						continue;
					}
				}
	
				
				// Chunk.getEntitiesOfTypeWithinAABB() does a reverse aabb intersect
				// We need to do a forward lookup
                // TODO move this to UMC?
				if (!this.getCollision().intersects(entity.getBounds())) {
					// miss
					continue;
				}
	
				// Move entity

				entity.setVelocity(this.getVelocity().scale(2));
				// Force update
				//TODO entity.onUpdate();
	
				double speedDamage = Math.abs(this.getCurrentSpeed().metric()) / Config.ConfigDamage.entitySpeedDamage;
				if (speedDamage > 1) {
				    boolean isDark = Math.max(getWorld().getSkyLightLevel(entity.getBlockPosition()), getWorld().getBlockLightLevel(entity.getBlockPosition())) < 8.0F;
				    entity.directDamage(isDark ? DAMAGE_SOURCE_HIT_IN_DARKNESS : DAMAGE_SOURCE_HIT, speedDamage);
				}
			}
	
			// Riding on top of cars
			final RealBB bb = this.getCollision().offset(new Vec3d(0, gauge.scale()*2, 0));
            List<Entity> entitiesAbove = getWorld().getEntities((Entity entity) -> (entity.isLiving() || entity.isPlayer()) && bb.intersects(entity.getBounds()), Entity.class);
			for (Entity entity : entitiesAbove) {
				if (entity instanceof EntityMoveableRollingStock) {
					continue;
				}
				if (entity.getRiding() instanceof EntityMoveableRollingStock) {
					continue;
				}
	
				// Chunk.getEntitiesOfTypeWithinAABB() does a reverse aabb intersect
				// We need to do a forward lookup
				if (!bb.intersects(entity.getBounds())) {
					// miss
					continue;
				}
				
				//Vec3d pos = entity.getPositionVector();
				//pos = pos.addVector(this.motionX, this.motionY, this.motionZ);
				//entity.setPosition(pos.x, pos.y, pos.z);

				entity.setVelocity(this.getVelocity().add(0, entity.getVelocity().y, 0));
			}
	    }

        if (getWorld().isServer) {
            setControlPosition("MOVINGFORWARD", getCurrentSpeed().minecraft() > 0 ? 1 : 0);
            setControlPosition("NOTMOVING", getCurrentSpeed().minecraft() == 0 ? 1 : 0);
            setControlPosition("MOVINGBACKWARD", getCurrentSpeed().minecraft() < 0 ? 1 : 0);
        }
    }

    protected void clearPositionCache() {
        this.boundingBox = null;
    }

    public double getRollDegrees() {
        if (Math.abs(getCurrentSpeed().metric() * gauge.scale()) < 4) {
            // don't calculate it
            return 0;
        }

        double sway = Math.cos(Math.toRadians(this.getTickCount() * 13)) *
                swayMagnitude / 5 *
                getDefinition().getSwayMultiplier() *
                ConfigGraphics.StockSwayMultiplier;

        double tilt = getDefinition().getTiltMultiplier() * (getPrevRotationYaw() - getRotationYaw()) * (getCurrentSpeed().minecraft() > 0 ? 1 : -1);

        return sway + tilt;
    }

    /*
     *
     * Client side render guessing
     */

    public float getFrontYaw() {
        if (this.frontYaw != null) {
            return this.frontYaw;
        }
        return this.getRotationYaw();
    }

    public void setFrontYaw(float frontYaw) {
        this.frontYaw = frontYaw;
    }

    public float getRearYaw() {
        if (this.rearYaw != null) {
            return this.rearYaw;
        }
        return this.getRotationYaw();
    }

    public void setRearYaw(float rearYaw) {
        this.rearYaw = rearYaw;
    }

    private Vec3i lastRetarderPos = null;
    private int lastRetarderValue = 0;

    public int getSpeedRetarderSlowdown(TickPos latest) {
        if (new Vec3i(latest.position).equals(lastRetarderPos)) {
            return lastRetarderValue;
        }

        int over = 0;
        int max = 0;
        for (Vec3i bp : getWorld().blocksInBounds(this.getCollision().offset(new Vec3d(0, gauge.scale(), 0)))) {
            TileRailBase te = getWorld().getBlockEntity(bp, TileRailBase.class);
            if (te != null) {
                if (te.getAugment() == Augment.SPEED_RETARDER) {
                    max = Math.max(max, getWorld().getRedstone(bp));
                    over += 1;
                }
            }
        }
        lastRetarderPos = new Vec3i(latest.position);
        lastRetarderValue = over * max;
        return lastRetarderValue;
    }

    public float getTickSkew() {
        ChronoState state = ChronoState.getState(getWorld());
        return state != null ? (float) state.getTickSkew() : 1;
    }

    @Override
    public void onRemoved() {
        super.onRemoved();

        if (getWorld().isClient) {
            this.getDefinition().getModel().onClientRemoved(this);
        }

        if (this.wheel_sound != null) {
            wheel_sound.stop();
        }
        if (this.clackFront != null) {
            clackFront.stop();
        }
        for (TileRailBase te : railCache) {
            te.stockOverhead(null);
        }
    }

    @Override
    public void handleKeyPress(Player source, KeyTypes key, boolean disableIndependentThrottle) {
        float independentBrakeNotch = 0.04f;

        if (source.hasPermission(Permissions.BRAKE_CONTROL)) {
            switch (key) {
                case INDEPENDENT_BRAKE_UP:
                    setIndependentBrake(getIndependentBrake() + independentBrakeNotch);
                    break;
                case INDEPENDENT_BRAKE_ZERO:
                    setIndependentBrake(0f);
                    break;
                case INDEPENDENT_BRAKE_DOWN:
                    setIndependentBrake(getIndependentBrake() - independentBrakeNotch);
                    break;
                default:
                    super.handleKeyPress(source, key, disableIndependentThrottle);
            }
        } else {
            super.handleKeyPress(source, key, disableIndependentThrottle);
        }
    }

    public float getIndependentBrake() {
        return getDefinition().hasIndependentBrake() ? independentBrake : 0;
    }
    public void setIndependentBrake(float newIndependentBrake) {
        newIndependentBrake = Math.min(1, Math.max(0, newIndependentBrake));
        if (this.getIndependentBrake() != newIndependentBrake && getDefinition().hasIndependentBrake()) {
            if (getDefinition().isLinearBrakeControl()) {
                setControlPositions(ModelComponentType.INDEPENDENT_BRAKE_X, newIndependentBrake);
            }
            independentBrake = newIndependentBrake;
        }
    }
    public float getTotalBrake() {
        return totalBrake;
    }

    @Deprecated
    public TickPos getCurrentTickPosAndPrune() {
        return getTickPos();
    }

    public double getBrakeShoeFriction() {
        return getDefinition().getBrakeShoeFriction();
    }
}
