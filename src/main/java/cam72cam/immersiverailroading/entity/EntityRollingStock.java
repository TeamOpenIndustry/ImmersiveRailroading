package cam72cam.immersiverailroading.entity;

import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import java.nio.charset.StandardCharsets;

import cam72cam.immersiverailroading.entity.registry.DefinitionManager;
import cam72cam.immersiverailroading.entity.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.library.KeyBindings;
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

	@Override
	public void updatePassenger(Entity passenger) {
		if (this.isPassenger(passenger)) {
			Vec3d pos = this.getDefinition().getPlayerOffset();
			pos = VecUtil.rotateYaw(pos, this.rotationYaw);
			pos = pos.add(this.getPositionVector());
			passenger.setPosition(pos.x, pos.y, pos.z);
		}
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
	public boolean canBePushed() {
		return false;
	}


	public void render(double x, double y, double z, float entityYaw, float partialTicks) {
		if (this.getDefinition() != null) {
			this.getDefinition().render(this, x, y, z, entityYaw, partialTicks);
		} else {
			this.getEntityWorld().removeEntity(this);
		}
	}

	public void handleKeyPress(Entity source, KeyBindings key) {
	}

}