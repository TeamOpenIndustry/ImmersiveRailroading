package cam72cam.immersiverailroading.entity;

import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public abstract class EntityRollingStock extends Entity implements IEntityAdditionalSpawnData {
	protected String defID;

	public EntityRollingStock(World world, String defID) {
		super(world);

		this.defID = defID;

		// TODO
		setSize((float) 10, (float) 10);

		super.preventEntitySpawning = true;
		super.isImmuneToFire = true;
		super.entityCollisionReduction = 0.8F;
		super.ignoreFrustumCheck = true;
	}

	/*
	 * 
	 * Data RW for Spawn and Entity Load
	 */

	@Override
	public void readSpawnData(ByteBuf additionalData) {
		byte[] defBytes = new byte[additionalData.readInt()];
		additionalData.readBytes(defBytes);
		defID = new String(defBytes, StandardCharsets.UTF_8);
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		buffer.writeInt(defID.getBytes(StandardCharsets.UTF_8).length);
		buffer.writeBytes(defID.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		nbttagcompound.setString("defID", defID);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		defID = nbttagcompound.getString("defID");
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
	public boolean canRiderInteract() {
		return true;
	}

	public boolean isRideable() {
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
				player.startRiding(this);
			}

			return true;
		}
	}

	@Override
	public boolean shouldRiderSit() {
		return false;
	}

	@Override
	public boolean attackEntityFrom(DamageSource damagesource, float amount) {
		if (damagesource.isCreativePlayer()) {
			this.setDead();
			return false;
		}

		if (damagesource.getTrueSource() instanceof EntityPlayer && !damagesource.isProjectile()) {
			this.setDead();
			return false;
		}
		return false;
	}

	@Override
	public void onUpdate() {
		if (this.isDead) {
			System.out.println("WE DEAD");
		}
	}

	@Override
	public boolean canBePushed() {
		return false;
	}

	@Override
	public void applyEntityCollision(Entity par1Entity) {
		// TODO @cam72cam
	}

	public abstract void render(double x, double y, double z, float entityYaw, float partialTicks);

}