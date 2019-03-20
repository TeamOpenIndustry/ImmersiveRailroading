package cam72cam.immersiverailroading.entity;

import java.util.UUID;

import com.google.common.base.Optional;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.model.RenderComponent;
import cam72cam.immersiverailroading.registry.CarArtilleryDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDefinition;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Rotations;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

public class CarArtillery extends CarFreight {

	private static DataParameter<Float> AIR_BRAKE = EntityDataManager.createKey(CarArtillery.class, DataSerializers.FLOAT);
	private static DataParameter<BlockPos> POINT_OF_AIM = EntityDataManager.createKey(CarArtillery.class, DataSerializers.BLOCK_POS);
	private static DataParameter<Float> RELOAD_TIME = EntityDataManager.createKey(CarArtillery.class, DataSerializers.FLOAT);
	private static DataParameter<Rotations> TURRET_ORIENT = EntityDataManager.createKey(CarArtillery.class, DataSerializers.ROTATIONS);
	protected static DataParameter<Optional<UUID>> SFX_PLAYER = EntityDataManager.createKey(CarArtillery.class, DataSerializers.OPTIONAL_UNIQUE_ID);
	
	private Rotations orientTarget;
	public float recoilStroke = 0;
	private static final float airBrakeNotch = 0.04f;
	
	public CarArtillery(World world) {
		this(world, null);
	}
	
	public CarArtillery(World world, String defID) {
		super(world, defID);
		
		this.getDataManager().register(AIR_BRAKE, 0f);
		this.getDataManager().register(POINT_OF_AIM, BlockPos.ORIGIN);
		this.getDataManager().register(RELOAD_TIME, 0f);
		this.getDataManager().register(TURRET_ORIENT, new Rotations(0, 0, 0));
		this.getDataManager().register(SFX_PLAYER, Optional.absent());

		this.entityCollisionReduction = 0.99F;
	}

	public void fire() {
		if (world.isRemote) recoilStroke += 5;
		
		Vec3d muzzle = getDefinition().getComponent(RenderComponentType.GUN_BARREL, this.gauge).max();
		Vec3d projectile = getDefinition().getProjectile();
		if (Config.ConfigDamage.explosionsEnabled) {
				Explosion explosion = new Explosion(this.world, this, this.posX + muzzle.x, this.posY + muzzle.y, this.posZ + muzzle.z, 10f, false, false);
				ImmersiveRailroading.info("Muzzle at %f %f %f", explosion.getPosition().x, explosion.getPosition().y, explosion.getPosition().z);
				explosion.doExplosionA();
				explosion.doExplosionB(true);
		}
		
		dataManager.set(RELOAD_TIME, this.ticksExisted + 20f);
	}
	
	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		nbttagcompound.setLong("target", getAimPoint().toLong());
		nbttagcompound.setFloat("brake", getAirBrake());
		nbttagcompound.setFloat("reload", this.getReloadTime());
		nbttagcompound.setTag("turret", this.getTurretOrient().writeToNBT());
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		dataManager.set(POINT_OF_AIM, BlockPos.fromLong(nbttagcompound.getLong("target")));
		setAirBrake(nbttagcompound.getFloat("brake"));
		dataManager.set(RELOAD_TIME, nbttagcompound.getFloat("reload"));
		NBTTagList nbttaglist = nbttagcompound.getTagList("turret", 5);
		dataManager.set(TURRET_ORIENT, nbttaglist.hasNoTags() ? new Rotations(0,0,0) : new Rotations(nbttaglist));
		orientTarget = dataManager.get(TURRET_ORIENT);
	}
	
	@Override
	public void handleKeyPress(Entity source, KeyTypes key, boolean sprinting) {
		switch(key) {
		case AIR_BRAKE_UP:
			if (getAirBrake() < 1) {
				setAirBrake(getAirBrake() + airBrakeNotch);
			}
			break;
		case AIR_BRAKE_ZERO:
			setAirBrake(0f);
			break;
		case AIR_BRAKE_DOWN:
			if (getAirBrake() > 0) {
				setAirBrake(getAirBrake() - airBrakeNotch);
			}
			break;
		case HORN:
			aim(BlockPos.ORIGIN);
			ImmersiveRailroading.info("Taking aim");
			break;
		case DEAD_MANS_SWITCH:
			if (getReloadTime() > ticksExisted) break;
			Vec3d curOrientVec = new Vec3d(getTurretOrient().getX(), getTurretOrient().getY(), getTurretOrient().getZ());
			Vec3d targOrientVec = new Vec3d(orientTarget.getX(), orientTarget.getY(), orientTarget.getZ());
			Vec3d turningVec = targOrientVec.subtract(curOrientVec);
			if (turningVec.lengthSquared() <= 2) fire();
			break;
		default:
			super.handleKeyPress(source, key, sprinting);
			break;
		}
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		
		if (!world.isRemote) {
			if (orientTarget != null) {
				Vec3d curOrientVec = new Vec3d(getTurretOrient().getX(), getTurretOrient().getY(), getTurretOrient().getZ());
				Vec3d targOrientVec = new Vec3d(orientTarget.getX(), orientTarget.getY(), orientTarget.getZ());
				Vec3d turningVec = targOrientVec.subtract(curOrientVec);
				if (turningVec.lengthSquared() > 1) {
					Rotations newOrient = new Rotations( 
							(float)(curOrientVec.x + Math.min(Math.abs(turningVec.x), 0.05) * Math.signum(turningVec.x)), 
							(float)(curOrientVec.y + Math.min(Math.abs(turningVec.y), 0.05) * Math.signum(turningVec.y)), 
							(float)(curOrientVec.z + Math.min(Math.abs(turningVec.z), 0.05) * Math.signum(turningVec.z)));
					// ImmersiveRailroading.info("Turning %f, %f, %f", turningVec.x, turningVec.y, turningVec.z);
					dataManager.set(TURRET_ORIENT, newOrient);
				}
			}
		} else {
			if (recoilStroke > 0.05) {
				ImmersiveRailroading.info("recoilling %f", recoilStroke);
				recoilStroke -= 0.05;
			}
		}
	}
	
	@Override
	public CarArtilleryDefinition getDefinition() {
		return super.getDefinition(CarArtilleryDefinition.class);
	}
	
	@Override
	public GuiTypes guiType() {
		return GuiTypes.FREIGHT;
	}
	
	public float getAirBrake() {
		return dataManager.get(AIR_BRAKE);
	}
	public void setAirBrake(float newAirBrake) {
		if (this.getAirBrake() != newAirBrake) {
			dataManager.set(AIR_BRAKE, newAirBrake);
			triggerResimulate();
		}
	}
	
	public float getReloadTime() {
		return dataManager.get(RELOAD_TIME);
	}
	public Rotations getTurretOrient() {
		return dataManager.get(TURRET_ORIENT);
	}
	public BlockPos getAimPoint() {
		return dataManager.get(POINT_OF_AIM);
	}
	public void aim(BlockPos target) {
		Rotations set = angleToTarget(target);
		ImmersiveRailroading.info("Firing solution for %d, %d", target.getX(), target.getY());
		if (set != null) {
			ImmersiveRailroading.info("Laying gun");
			orientTarget = set;
			dataManager.set(POINT_OF_AIM, target);
		}
		else {
			ImmersiveRailroading.info("Out of line");
		}
	}
	
	public Rotations angleToTarget(BlockPos target) {
		CarArtilleryDefinition def = this.getDefinition();
		Vec3d projectileInfo = def.getProjectile();
		Vec3d targPos = new Vec3d(target);
		Vec3d pos = this.getPositionVector();
		double targetDistance = targPos.subtract(pos).lengthVector();
		if (targetDistance > def.getRange()) return null;
		
		float yawToTarget = VecUtil.toWrongYaw(targPos.subtract(pos)) - this.rotationYaw + 180;
		yawToTarget = (yawToTarget + 180) % 360 - 180;
		ImmersiveRailroading.info("Yawing, %f", yawToTarget);
		if (Math.abs(yawToTarget) > (def.orientLimit.getY()/2)) {
			return null;
		}

		// theta = [90deg -] 1/2 * arcsin((g * d) / v^2)
		float pitchToTarget = (float) Math.toDegrees(0.5 * Math.asin((9.81 * targetDistance) / Math.pow(projectileInfo.z, 2)));
		float pitchToTarget2 = 90 - pitchToTarget;
		ImmersiveRailroading.info("Pitching, %f", pitchToTarget);
		if (pitchToTarget < def.orientLimit.getX() && pitchToTarget > 45) {
			return new Rotations(pitchToTarget, yawToTarget, 0f);
		}
		else if (pitchToTarget2 < def.orientLimit.getX() && pitchToTarget2 > 45) {
			return new Rotations(pitchToTarget2, yawToTarget, 0f);
		}
		return null;
	}
}
