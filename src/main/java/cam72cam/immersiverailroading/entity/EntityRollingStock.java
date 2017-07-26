package cam72cam.immersiverailroading.entity;

import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.registry.DefinitionManager;
import cam72cam.immersiverailroading.entity.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.library.KeyBindings;
import cam72cam.immersiverailroading.net.PassengerPositionsPacket;
import cam72cam.immersiverailroading.util.BufferUtil;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.immersiverailroading.util.VecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class EntityRollingStock extends Entity implements IEntityAdditionalSpawnData {
	protected String defID;

	public EntityRollingStock(World world, String defID) {
		super(world);

		this.defID = defID;

		// TODO
		setSize((float) 0, (float) 0);

		super.preventEntitySpawning = true;
		super.isImmuneToFire = true;
		super.entityCollisionReduction = 1F;
		super.ignoreFrustumCheck = true;
	}
	
	protected EntityRollingStockDefinition getDefinition() {
		return DefinitionManager.getDefinition(defID);
	}

	/*
	 * 
	 * Data RW for Spawn and Entity Load
	 */

	@Override
	public void readSpawnData(ByteBuf additionalData) {
		defID = BufferUtil.readString(additionalData);
		rollingStockInit();
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		BufferUtil.writeString(buffer, defID);
		rollingStockInit();
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		nbttagcompound.setString("defID", defID);
		
		if (passengerOffsets.size() > 0) {
			NBTTagCompound offsetTag = nbttagcompound.getCompoundTag("passengerOffsets");
			List<String> passengers = new ArrayList<String>();
			for (UUID passenger : passengerOffsets.keySet()) {
				passengers.add(passenger.toString());
				offsetTag.setDouble(passenger.toString() + ".x", passengerOffsets.get(passenger).x);
				offsetTag.setDouble(passenger.toString() + ".y", passengerOffsets.get(passenger).y);
				offsetTag.setDouble(passenger.toString() + ".z", passengerOffsets.get(passenger).z);
			}
			offsetTag.setString("passengers", String.join("|", passengers));
			nbttagcompound.setTag("passengerOffsets", offsetTag);
		}
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		defID = nbttagcompound.getString("defID");
		
		if (nbttagcompound.hasKey("passengerOffsets")) {
			NBTTagCompound offsetTag = nbttagcompound.getCompoundTag("passengerOffsets");
			System.out.println(offsetTag.getString("passengers"));
			for (String passenger : offsetTag.getString("passengers").split("\\|")) {
				Vec3d pos = new Vec3d(offsetTag.getDouble(passenger + ".x"), offsetTag.getDouble(passenger + ".y"), offsetTag.getDouble(passenger + ".z"));
				passengerOffsets.put(UUID.fromString(passenger), pos);
			}
		}
		
		rollingStockInit();
	}
	
	/**
	 * Fired after we have a definitionID.
	 * Here is where you construct objects based
	 * on the rolling stock definition
	 */
	protected void rollingStockInit() {
		if (!world.isRemote) {
			this.syncPassengerOffsets();
		}
	}

	@Override
	protected void entityInit() {
	}

	public Speed getCurrentSpeed() {
		return Speed.fromMinecraft(MathHelper.sqrt(motionX * motionX + motionZ * motionZ));
	}

	/*
	 * Player Interactions
	 */
	@Override
	public boolean canBeCollidedWith() {
		// Needed for right click, probably a forge or MC bug
		return true;
	}

	@Override
	public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
		if (player.isSneaking()) {
			return false;
		} else if (this.isBeingRidden()) {
			return true;
		} else {
			if (!this.world.isRemote) {
				passengerOffsets.put(player.getPersistentID(), new Vec3d(0, 0, 0));
				player.startRiding(this);
			}

			return true;
		}
	}
	
	@Override
	public boolean canRiderInteract() {
		return false;
	}

	@Override
	public boolean shouldRiderSit() {
		return false;
	}

	public Map<UUID, Vec3d> passengerOffsets = new HashMap<UUID, Vec3d>();
	public void handleKeyPress(Entity source, KeyBindings key) {
		Vec3d movement = null;
		switch (key) {
		case PLAYER_FORWARD:
			movement = new Vec3d(0.1, 0, 0);
			break;
		case PLAYER_BACKWARD:
			movement = new Vec3d(-0.1, 0, 0);
			break;
		case PLAYER_LEFT:
			movement = new Vec3d(0, 0, -0.1);
			break;
		case PLAYER_RIGHT:
			movement = new Vec3d(0, 0, 0.1);
			break;
		default:
			//ignore key
			return;
		}
		if (source.getRidingEntity() == this) {
			movement = VecUtil.rotateYaw(movement, source.getRotationYawHead());
			movement = VecUtil.rotateYaw(movement, 180-this.rotationYaw);
			
			Vec3d pos = passengerOffsets.get(source.getPersistentID()).add(movement);

			pos = this.getDefinition().correctPassengerBounds(pos);
			
			passengerOffsets.put(source.getPersistentID(), pos);
			syncPassengerOffsets();
		}
	}
	
	public void syncPassengerOffsets() {
		ImmersiveRailroading.net.sendToAllAround(new PassengerPositionsPacket(this), new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, ImmersiveRailroading.ENTITY_SYNC_DISTANCE));
	}
	
	//nasty hack
	private int ticksToSyncOffset = 0;
	@Override
	protected void addPassenger(Entity passenger) {
		super.addPassenger(passenger);
		if (!world.isRemote) {
			ticksToSyncOffset = 5;
		}
	}
	
	@Override
	public void updatePassenger(Entity passenger) {
		if (this.isPassenger(passenger)) {
			if (!passengerOffsets.containsKey(passenger.getPersistentID())) {
				passengerOffsets.put(passenger.getPersistentID(), new Vec3d(0, 0, 0));
			}
			
			Vec3d pos = this.getDefinition().getPlayerOffset();
			pos = pos.add(passengerOffsets.get(passenger.getPersistentID()));
			pos = VecUtil.rotateYaw(pos, this.rotationYaw);
			pos = pos.add(this.getPositionVector());
			passenger.setPosition(pos.x, pos.y, pos.z);
		}
	}

	@Override
	public boolean attackEntityFrom(DamageSource damagesource, float amount) {
		if (world.isRemote) {
			return false;
		}
		if (damagesource.isCreativePlayer()) {
			this.setDead();
			world.removeEntity(this);
			return false;
		}

		if (damagesource.getTrueSource() instanceof EntityPlayer && !damagesource.isProjectile()) {
			this.setDead();
			world.removeEntity(this);
			return false;
		}
		return false;
	}

	@Override
	public boolean canBePushed() {
		return false;
	}
	
	@Override
	public void onUpdate() {
		if (this.ticksExisted % 50 == 0 && !world.isRemote) {
			this.syncPassengerOffsets();
		}
		// Delayed sync after a user logs in.  We sync the entity they are riding N ticks after they have loaded
		// Otherwise the packet gets there before the entity is fully instantiated.
		if (!world.isRemote) {
			if (ticksToSyncOffset > 0) {
				ticksToSyncOffset--;
			} else if (ticksToSyncOffset == 0) {
				this.syncPassengerOffsets();
				ticksToSyncOffset = -1;
			}
		}
	}


	public void render(double x, double y, double z, float entityYaw, float partialTicks) {
		if (this.getDefinition() != null) {
			this.getDefinition().render(this, x, y, z, entityYaw, partialTicks);
		} else {
			this.getEntityWorld().removeEntity(this);
		}
	}

}