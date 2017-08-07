package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.net.PassengerPositionsPacket;
import cam72cam.immersiverailroading.util.BufferUtil;
import cam72cam.immersiverailroading.util.VecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class EntityRidableRollingStock extends EntityRollingStock {
	public EntityRidableRollingStock(World world, String defID) {
		super(world, defID);
	}
	
	@Override
	public void readSpawnData(ByteBuf additionalData) {
		passengerPositions = BufferUtil.readPlayerPositions(additionalData);
		super.readSpawnData(additionalData);
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		BufferUtil.writePlayerPositions(buffer, passengerPositions);
		super.writeSpawnData(buffer);
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		
		if (passengerPositions.size() > 0) {
			NBTTagCompound offsetTag = nbttagcompound.getCompoundTag("passengerOffsets");
			List<String> passengers = new ArrayList<String>();
			for (UUID passenger : passengerPositions.keySet()) {
				passengers.add(passenger.toString());
				offsetTag.setDouble(passenger.toString() + ".x", passengerPositions.get(passenger).x);
				offsetTag.setDouble(passenger.toString() + ".y", passengerPositions.get(passenger).y);
				offsetTag.setDouble(passenger.toString() + ".z", passengerPositions.get(passenger).z);
			}
			offsetTag.setString("passengers", String.join("|", passengers));
			nbttagcompound.setTag("passengerOffsets", offsetTag);
		}
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		
		if (nbttagcompound.hasKey("passengerOffsets")) {
			NBTTagCompound offsetTag = nbttagcompound.getCompoundTag("passengerOffsets");
			System.out.println(offsetTag.getString("passengers"));
			for (String passenger : offsetTag.getString("passengers").split("\\|")) {
				Vec3d pos = new Vec3d(offsetTag.getDouble(passenger + ".x"), offsetTag.getDouble(passenger + ".y"), offsetTag.getDouble(passenger + ".z"));
				passengerPositions.put(UUID.fromString(passenger), pos);
			}
		}
	}
	
	@Override
	public void rollingStockInit() {
		super.rollingStockInit();
	}
	

	@Override
	public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
		if (player.isSneaking()) {
			return false;
		} else if (this.isBeingRidden()) {
			return true;
		} else {
			if (!this.world.isRemote) {
				passengerPositions.put(player.getPersistentID(), new Vec3d(0, 0, 0));
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

	public Map<UUID, Vec3d> passengerPositions = new HashMap<UUID, Vec3d>();
	public void handleKeyPress(Entity source, KeyTypes key) {
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
			
			Vec3d pos = passengerPositions.get(source.getPersistentID()).add(movement);

			pos = this.getDefinition().correctPassengerBounds(pos);
			
			passengerPositions.put(source.getPersistentID(), pos);
			sendToObserving(new PassengerPositionsPacket(this));
		}
	}
	
	@Override
	protected void addPassenger(Entity passenger) {
		super.addPassenger(passenger);
	}
	
	@Override
	public void updatePassenger(Entity passenger) {
		if (this.isPassenger(passenger)) {
			if (!passengerPositions.containsKey(passenger.getPersistentID())) {
				passengerPositions.put(passenger.getPersistentID(), new Vec3d(0, 0, 0));
			}
			
			Vec3d pos = this.getDefinition().getPassengerCenter();
			pos = pos.add(passengerPositions.get(passenger.getPersistentID()));
			pos = VecUtil.rotateYaw(pos, this.rotationYaw);
			pos = pos.add(this.getPositionVector());
			passenger.setPosition(pos.x, pos.y, pos.z);
		}
	}
	
	@Override
	public void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		Vec3d delta = VecUtil.fromYaw(this.getDefinition().getPassengerCompartmentWidth()/2 + 1.3, this.rotationYaw - 90);
		passenger.setPositionAndUpdate(passenger.posX += delta.x, passenger.posY, passenger.posZ += delta.z);
	}
}
