package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.inventory.SlotFilter;
import cam72cam.immersiverailroading.items.nbt.ItemComponent;
import cam72cam.immersiverailroading.items.nbt.ItemDefinition;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.model.RenderComponent;
import cam72cam.immersiverailroading.proxy.ChunkManager;
import cam72cam.immersiverailroading.registry.CarArtilleryDefinition;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Rotations;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class CarArtillery extends CarFreight {

	private static DataParameter<Float> RECOIL_FORCE = EntityDataManager.createKey(CarArtillery.class, DataSerializers.FLOAT);
	private static DataParameter<Float> AIR_BRAKE = EntityDataManager.createKey(CarArtillery.class, DataSerializers.FLOAT);
	private static DataParameter<BlockPos> POINT_OF_AIM = EntityDataManager.createKey(CarArtillery.class, DataSerializers.BLOCK_POS);
	private static DataParameter<Float> RELOAD_TIME = EntityDataManager.createKey(CarArtillery.class, DataSerializers.FLOAT);
	private static DataParameter<Rotations> TURRET_ORIENT = EntityDataManager.createKey(CarArtillery.class, DataSerializers.ROTATIONS);
	//protected static DataParameter<Optional<UUID>> SFX_PLAYER = EntityDataManager.createKey(CarArtillery.class, DataSerializers.OPTIONAL_UNIQUE_ID);
	
	private Rotations orientTarget = new Rotations(0, 0, 0);
	public float recoilStroke = 0;
	private static final float airBrakeNotch = 0.04f;
	
	public CarArtillery(World world) {
		this(world, null);
	}
	
	public CarArtillery(World world, String defID) {
		super(world, defID);
		
		this.getDataManager().register(RECOIL_FORCE, 0f);
		this.getDataManager().register(AIR_BRAKE, 0f);
		this.getDataManager().register(POINT_OF_AIM, BlockPos.ORIGIN);
		this.getDataManager().register(RELOAD_TIME, 0f);
		this.getDataManager().register(TURRET_ORIENT, new Rotations(0, 0, 0));
		//this.getDataManager().register(SFX_PLAYER, Optional.absent());

		this.entityCollisionReduction = 0.99F;
	}

	public void attemptFire() {
		if (getReloadTime() > ticksExisted) return;
		Vec3d curOrientVec = new Vec3d(getTurretOrient().getX(), getTurretOrient().getY(), getTurretOrient().getZ());
		Vec3d targOrientVec = new Vec3d(orientTarget.getX(), orientTarget.getY(), orientTarget.getZ());
		Vec3d turningVec = targOrientVec.subtract(curOrientVec);
		int chargeSlot = -1, projectileSlot = -1;
		for (int i = 0; i < this.cargoItems.getSlots(); i++) {
			ItemStack itemStack = this.cargoItems.getStackInSlot(i);
			if (itemStack.getItem() == IRItems.ITEM_ROLLING_STOCK_COMPONENT && 
				ItemComponent.getComponentType(itemStack) == ItemComponentType.GUN_PROJECTILE && 
				ItemDefinition.getID(itemStack).equals(this.defID)) {
				projectileSlot = i;
			}
			if (itemStack.getItem().equals(Items.GUNPOWDER)) {
				chargeSlot = i;
			}
			if (projectileSlot != -1 && chargeSlot != -1) break;
		}
		if (projectileSlot == -1 || chargeSlot == -1) return;
		if (aim(this.getAimPoint()) && turningVec.lengthSquared() <= 2) fire();
		this.cargoItems.getStackInSlot(chargeSlot).shrink(this.getDefinition().getChargeAmount());
		this.cargoItems.getStackInSlot(projectileSlot).shrink(1);
	}
	
	private void fire() {
		Vec3d shotInfo = getDefinition().getProjectileInfo();
		dataManager.set(RECOIL_FORCE, (float)(shotInfo.x * shotInfo.z));
		
		Vec3d curOrientVec = new Vec3d(getTurretOrient().getX(), getTurretOrient().getY(), getTurretOrient().getZ());
		Vec3d muzzle = muzzlePosition();
		muzzle = VecUtil.rotateWrongYaw(muzzle, this.rotationYaw + 180);
		if (Config.ConfigDamage.explosionsEnabled) {
				world.createExplosion(this, this.posX + muzzle.x, this.posY + muzzle.y, this.posZ + muzzle.z, 3f, false);
		}
		
		double dispersion = this.getDefinition().getAccuracy() * (new Vec3d(getAimPoint()).subtract(this.getPositionVector()).lengthVector() / this.getDefinition().getRange());
		Vec3d hitCoord = new Vec3d(getAimPoint().getX() + dispersion * (this.rand.nextDouble() - 0.5), 255, getAimPoint().getZ() + dispersion * (this.rand.nextDouble() - 0.5));
		int ticksInFlight = (int)Math.floor((2 * shotInfo.z * Math.sin(Math.toRadians(curOrientVec.x))) / 9.81) * 20;
		ImmersiveRailroading.info("Firing at %f,%f", hitCoord.x, hitCoord.z);
		ChunkManager.flagEntityPos(world, new BlockPos(hitCoord));
		world.spawnEntity(new EntityArtilleryStrike(this.world, hitCoord, (float)shotInfo.x, (float)shotInfo.z, (float)shotInfo.y, ticksInFlight));
		dataManager.set(RELOAD_TIME, this.ticksExisted + (20 * this.getDefinition().getReloadTime()));
		triggerResimulate();
	}
	
	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		nbttagcompound.setFloat("brake", getAirBrake());
		nbttagcompound.setFloat("reload", this.getReloadTime());
		nbttagcompound.setTag("turret", this.getTurretOrient().writeToNBT());
		nbttagcompound.setLong("targetPoint", getAimPoint().toLong());
		nbttagcompound.setTag("targetRot", this.orientTarget.writeToNBT());
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		setAirBrake(nbttagcompound.getFloat("brake"));
		//dataManager.set(RELOAD_TIME, nbttagcompound.getFloat("reload"));
		NBTTagList nbttaglist = nbttagcompound.getTagList("turret", 5);
		dataManager.set(TURRET_ORIENT, nbttaglist.hasNoTags() ? new Rotations(0,0,0) : new Rotations(nbttaglist));
		dataManager.set(POINT_OF_AIM, BlockPos.fromLong(nbttagcompound.getLong("targetPoint")));
		NBTTagList nbttaglist2 = nbttagcompound.getTagList("targetRot", 5);
		orientTarget = nbttaglist2.hasNoTags() ? new Rotations(0,0,0) : new Rotations(nbttaglist2);
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
			if (aim(this.getAimPoint()) && turningVec.lengthSquared() <= 2) attemptFire();
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
							(float)(curOrientVec.x + Math.min(Math.abs(turningVec.x), this.getDefinition().orientSpeed.getX()/20) * Math.signum(turningVec.x)), 
							(float)(curOrientVec.y + Math.min(Math.abs(turningVec.y), this.getDefinition().orientSpeed.getY()/20) * Math.signum(turningVec.y)), 
							(float)(curOrientVec.z)
					);
					dataManager.set(TURRET_ORIENT, newOrient);
				}
			}
		} else {
			if (dataManager.get(RECOIL_FORCE) != 0 && recoilStroke == 0) recoilStroke = this.getDefinition().getRecoilLength();
			if (recoilStroke > 0) {
				Vec3d shotInfo = getDefinition().getProjectileInfo();
				// Fudged value
				recoilStroke -= 15/shotInfo.x;
			} 
			if (recoilStroke < 0) {
				recoilStroke = 0;
			}	
		}
	}
	
	@Override
	public CarArtilleryDefinition getDefinition() {
		return super.getDefinition(CarArtilleryDefinition.class);
	}
	
	@Override
	public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
		if (player.getHeldItemMainhand().getItem() == Items.LEAD && this.isBuilt()) {
			this.attemptFire();
			return true;
		}
		if (player.getHeldItemMainhand().getItem() == IRItems.ITEM_ROLLING_STOCK_COMPONENT && 
			ItemComponent.getComponentType(player.getHeldItem(hand)) == ItemComponentType.GUN_PROJECTILE && 
			ItemDefinition.getID(player.getHeldItem(hand)).equals(this.defID)) {
			return false;
		}
		
		return super.processInitialInteract(player, hand);
	}
	
	@Override
	public GuiTypes guiType() {
		return GuiTypes.ARTILLERY;
	}
	
	@Override
	protected void initContainerFilter() {
		cargoItems.filter.clear();
		this.cargoItems.filter.put(getInventorySize(), SlotFilter.ANY);
		this.cargoItems.filter.put(getInventorySize()-1, SlotFilter.GUNPOWDER);
		this.cargoItems.defaultFilter = SlotFilter.ANY;
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
	
	public float getApplyRecoilForce() {
		float a = -dataManager.get(RECOIL_FORCE);
		dataManager.set(RECOIL_FORCE, 0f);
		return a;
	}
	
	public float getReloadTime() {
		return dataManager.get(RELOAD_TIME);
	}
	/**Returns rotation as (Pitch, Yaw, Roll [Not used]) **/
	public Rotations getTurretOrient() {
		return dataManager.get(TURRET_ORIENT);
	}
	public BlockPos getAimPoint() {
		return dataManager.get(POINT_OF_AIM);
	}
	public Boolean aim(BlockPos target) {
		Rotations set = angleToTarget(target);
		ImmersiveRailroading.info("Firing solution for %d, %d", target.getX(), target.getY());
		if (set != null) {
			ImmersiveRailroading.info("Laying gun");
			orientTarget = set;
			dataManager.set(POINT_OF_AIM, target);
			return true;
		}
		else {
			ImmersiveRailroading.info("Out of line");
			return false;
		}
	}
	
	public Rotations angleToTarget(BlockPos target) {
		CarArtilleryDefinition def = this.getDefinition();
		Vec3d projectileInfo = def.getProjectileInfo();
		Vec3d targPos = new Vec3d(target);
		Vec3d pos = this.getPositionVector();
		double targetDistance = targPos.subtract(pos).lengthVector();
		if (targetDistance > def.getRange()) return null;
		
		float yawToTarget = VecUtil.toWrongYaw(targPos.subtract(pos)) - this.rotationYaw;
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
			return new Rotations(pitchToTarget - this.rotationPitch, yawToTarget, 0f);
		}
		else if (pitchToTarget2 < def.orientLimit.getX() && pitchToTarget2 > 45) {
			return new Rotations(pitchToTarget2 - this.rotationPitch, yawToTarget, 0f);
		}
		return null;
	}
	
	public Vec3d muzzlePosition() {
		CarArtilleryDefinition def = this.getDefinition(); 
		RenderComponent barrel = def.getComponent(RenderComponentType.GUN_BARREL, this.gauge);
		RenderComponent turret = def.getComponent(RenderComponentType.GUN_TURRET, this.gauge);
		Vec3d pivotX = def.getComponent(RenderComponentType.GUN_PIVOT_X, this.gauge).center();
		Vec3d pivotY = new Vec3d(0, 0, 0);
		if(turret != null) pivotY = def.getComponent(RenderComponentType.GUN_PIVOT_Y, this.gauge).center();
		util.Matrix4 matrix = new util.Matrix4();
		
		Vec3d rot = new Vec3d(getTurretOrient().getX(), getTurretOrient().getY(), getTurretOrient().getZ());
		if (turret == null) {
			matrix.rotate(Math.toRadians(-rot.y), 0, 1, 0);
		} else {
			matrix.translate(pivotY.x, pivotY.y, pivotY.z);
			matrix.rotate(Math.toRadians(-rot.y), 0, 1, 0);
			matrix.translate(-pivotY.x, -pivotY.y, -pivotY.z);
		}
		matrix.translate(pivotX.x, pivotX.y, pivotX.z);
		matrix.rotate(Math.toRadians(-rot.x), 0, 0, 1);
		matrix.translate(-pivotX.x, -pivotX.y, -pivotX.z);
		matrix.translate(-barrel.length()/2, 0, 0);
		return matrix.apply(barrel.center());
	}
	
	public double slipCoefficient() {
		double slipMult = 1.0;
		World world = getEntityWorld();
		if (world.isRaining() && world.canSeeSky(getPosition())) {
			Biome biome = world.getBiome(getPosition());
			if (biome.canRain()) {
				slipMult = 0.6;
			}
			if (biome.isSnowyBiome()) {
				slipMult = 0.4;
			}
		}
		// Wheel balance messing with friction
		if (this.getCurrentSpeed().metric() != 0) {
			double balance = 1 - 0.004 * Math.abs(this.getCurrentSpeed().metric());
			slipMult *= balance;
		}
		return slipMult;
	}
}
