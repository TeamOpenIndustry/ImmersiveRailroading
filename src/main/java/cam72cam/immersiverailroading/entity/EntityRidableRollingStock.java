package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.Config.ConfigDamage;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.StockDeathType;
import cam72cam.immersiverailroading.net.PassengerPositionsPacket;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.BufferUtil;
import cam72cam.immersiverailroading.util.VecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import util.Matrix4;

public abstract class EntityRidableRollingStock extends EntityBuildableRollingStock {
	public EntityRidableRollingStock(World world, String defID) {
		super(world, defID);
	}
	
	@Override
	public void readSpawnData(ByteBuf additionalData) {
		passengerPositions = BufferUtil.readPlayerPositions(additionalData);
		staticPassengers = BufferUtil.readStaticPassengers(additionalData);
		super.readSpawnData(additionalData);
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		BufferUtil.writePlayerPositions(buffer, passengerPositions);
		BufferUtil.writeStaticPassengers(buffer, staticPassengers);
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
		if (staticPassengers.size() > 0) {
			NBTTagCompound passengers = nbttagcompound.getCompoundTag("staticPassengers");
			passengers.setInteger("count", staticPassengers.size());
			int i = 0;
			for (StaticPassenger passenger : staticPassengers) {
				passengers.setTag("" + i, passenger.writeNBT());
				i++;
			}
			nbttagcompound.setTag("staticPassengers", passengers);
		}
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		
		if (nbttagcompound.hasKey("passengerOffsets")) {
			NBTTagCompound offsetTag = nbttagcompound.getCompoundTag("passengerOffsets");
			for (String passenger : offsetTag.getString("passengers").split("\\|")) {
				Vec3d pos = new Vec3d(offsetTag.getDouble(passenger + ".x"), offsetTag.getDouble(passenger + ".y"), offsetTag.getDouble(passenger + ".z"));
				passengerPositions.put(UUID.fromString(passenger), pos);
			}
		}

		if (nbttagcompound.hasKey("staticPassengers")) {
			NBTTagCompound passengers = nbttagcompound.getCompoundTag("staticPassengers");
			int count = passengers.getInteger("count");
			for (int i = 0; i < count; i++) {
				staticPassengers.add(new StaticPassenger(passengers.getCompoundTag("" + i)));
			}
		}
	}
	

	@Override
	public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
		if (super.processInitialInteract(player, hand)) {
			return true;
		}
		
		if (player.isSneaking()) {
			return false;
		} else if (player.isRiding() && player.getRidingEntity().getPersistentID() == this.getPersistentID()) {
			return false;
		} else {
			if (!this.world.isRemote) {
				passengerPositions.put(player.getPersistentID(), new Vec3d(0, 0, 0));
				sendToObserving(new PassengerPositionsPacket(this));
				player.startRiding(this);
			}

			return true;
		}
	}
	
	@Override
	public boolean canFitPassenger(Entity passenger) {
		return this.getPassengers().size() + this.staticPassengers.size() < this.getDefinition().getMaxPassengers();
	}
	
	@Override
	public boolean canRiderInteract() {
		return false;
	}

	@Override
	public boolean shouldRiderSit() {
		if (this.getDefinition().shouldSit != null) {
			return this.getDefinition().shouldSit;
		}
		return this.gauge.shouldSit();
	}

	public Map<UUID, Vec3d> passengerPositions = new HashMap<UUID, Vec3d>();
	public Map<UUID, Vec3d> passengerDismountAttempts = new HashMap<UUID, Vec3d>();
	public Map<Integer, Vec3d> dismounts = new HashMap<Integer, Vec3d>();
	private final double pressDist = 0.05;
	public List<StaticPassenger> staticPassengers = new ArrayList<StaticPassenger>();
	
	public void handleKeyPress(Entity source, KeyTypes key, boolean sprinting) {
		Vec3d movement = null;
		switch (key) {
		case PLAYER_FORWARD:
			movement = new Vec3d(pressDist, 0, 0);
			break;
		case PLAYER_BACKWARD:
			movement = new Vec3d(-pressDist, 0, 0);
			break;
		case PLAYER_LEFT:
			movement = new Vec3d(0, 0, -pressDist);
			break;
		case PLAYER_RIGHT:
			movement = new Vec3d(0, 0, pressDist);
			break;
		default:
			//ignore key
			return;
		}
		if (source.getRidingEntity() == this) {
			if (sprinting) {
				movement = movement.scale(3);
			}
			
			movement = VecUtil.rotateWrongYaw(movement, source.getRotationYawHead());
			movement = VecUtil.rotateWrongYaw(movement, 180-this.rotationYaw);
			
			Vec3d pos = passengerPositions.get(source.getPersistentID()).add(movement);

			
			if (this instanceof EntityCoupleableRollingStock) {
				if (this.getDefinition().isAtFront(gauge, pos) && ((EntityCoupleableRollingStock)this).isCoupled(CouplerType.FRONT)) {
					source.startRiding(((EntityCoupleableRollingStock)this).getCoupled(CouplerType.FRONT));
					return;
				}
				if (this.getDefinition().isAtRear(gauge, pos) && ((EntityCoupleableRollingStock)this).isCoupled(CouplerType.BACK)) {
					source.startRiding(((EntityCoupleableRollingStock)this).getCoupled(CouplerType.BACK));
					return;
				}
			}
			
			pos = this.getDefinition().correctPassengerBounds(gauge, pos);
			
			passengerPositions.put(source.getPersistentID(), pos);
			sendToObserving(new PassengerPositionsPacket(this));
		}
	}
	
	@Override
	protected void addPassenger(Entity passenger) {
		Vec3d ppos = new Vec3d(passenger.lastTickPosX, passenger.lastTickPosY, passenger.lastTickPosZ);
		super.addPassenger(passenger);
		
		if (!world.isRemote) {
			Vec3d center = this.getDefinition().getPassengerCenter(gauge);
			center = VecUtil.rotateWrongYaw(center, this.rotationYaw);
			center = center.add(this.getPositionVector());
			Vec3d off = VecUtil.rotateWrongYaw(center.subtract(ppos), -this.rotationYaw);
			
			off = this.getDefinition().correctPassengerBounds(gauge, off);
			off = off.addVector(0, -off.y, 0);
			
			passengerPositions.put(passenger.getPersistentID(), off);
			updatePassenger(passenger);
			sendToObserving(new PassengerPositionsPacket(this));
		}
	}
	
	@Override
	public void updatePassenger(Entity passenger) {
		if (this.isPassenger(passenger) && passengerPositions.containsKey(passenger.getPersistentID())) {
			Vec3d pos = this.getDefinition().getPassengerCenter(gauge);
			Vec3d ppos = passengerPositions.get(passenger.getPersistentID());
			pos = pos.add(ppos);
			pos = VecUtil.rotatePitch(pos, rotationPitch);
			pos = VecUtil.rotateWrongYaw(pos, this.rotationYaw);
			pos = pos.add(this.getPositionVector());
			if (passenger instanceof EntityPlayer && shouldRiderSit()) {
				pos = pos.subtract(0, 0.75, 0);
			}
			passenger.setPosition(pos.x, pos.y, pos.z);
			passenger.motionX = this.motionX;
			passenger.motionY = this.motionY;
			passenger.motionZ = this.motionZ;
			
			passenger.prevRotationYaw = passenger.rotationYaw;
			passenger.rotationYaw += (this.rotationYaw - this.prevRotationYaw);
		}
	}
	
	@Override
	public void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		
		if (passengerPositions.containsKey(passenger.getPersistentID()) ) {
			Vec3d ppos = passengerPositions.get(passenger.getPersistentID());
			Vec3d delta = dismountPos(ppos);
			delta = new Vec3d(delta.x, passenger.posY, delta.z);
			
			if (passenger instanceof EntityPlayer && !passenger.isDead && !this.isDead) {
				if(!ppos.equals(passengerDismountAttempts.get(passenger.getPersistentID()))) {
					delta = dismountFreePos(ppos, (EntityPlayer)passenger);
					if (delta.y == -1) {
						passengerDismountAttempts.put(passenger.getPersistentID(), ppos);
						passenger.startRiding(this, true); // Should I override dismountRidingEntity for this instead?
						return;
					}
				} else {
					passenger.startRiding(this, true); // Should I override dismountRidingEntity for this instead?
					return;
				}
			}
			
			passenger.setPositionAndUpdate(delta.x, delta.y, delta.z);
			if (!world.isRemote) {
				dismounts.put(passenger.getEntityId(), new Vec3d(delta.x, delta.y, delta.z));
				passengerPositions.remove(passenger.getPersistentID());
				sendToObserving(new PassengerPositionsPacket(this));
			}
		}
	}
	
	public void onUpdate() {
		super.onUpdate();

		if (!world.isRemote) {
			for (Integer id : dismounts.keySet()) {
				Entity ent = world.getEntityByID(id);
				if (ent != null) {
					Vec3d pos = dismounts.get(id);
					ent.setPosition(pos.x, pos.y, pos.z);
				}
			}
			dismounts.clear();
		}
	}
	
	public Vec3d dismountPos(Vec3d ppos) {
		Vec3d pos = this.getDefinition().getPassengerCenter(gauge);
		pos = pos.add(ppos);
		pos = VecUtil.rotateWrongYaw(pos, this.rotationYaw);
		pos = pos.add(this.getPositionVector());
		
		Vec3d delta = VecUtil.fromWrongYaw(this.getDefinition().getPassengerCompartmentWidth(gauge)/2 + 1.3 * gauge.scale(), this.rotationYaw + (ppos.z > 0 ? 90 : -90));
		
		return delta.add(pos);
	}
	
	public Vec3d dismountFreePos(Vec3d ppos, EntityPlayer passenger) {
		float dismountAngle = ppos.z > 0 ? 90 : -90;
		ppos = new Vec3d(ppos.x, ppos.y, 0);
		Vec3d pos = this.getDefinition().getPassengerCenter(gauge);
		pos = pos.add(ppos);
		pos = VecUtil.rotateWrongYaw(pos, this.rotationYaw);
		pos = pos.add(this.getPositionVector());
		Vec3d delta;
		List<Vec3d> possibleDismount = new ArrayList<Vec3d>();

		for (float i = 0.5f; i < 3; i += 1) {
			delta = VecUtil.fromWrongYaw(this.getDefinition().getWidth(gauge) / 2 + i * (1 / gauge.scale()), this.rotationYaw + dismountAngle);
			delta = delta.add(pos);
			delta = new Vec3d(delta.x, passenger.posY, delta.z);
			// ImmersiveRailroading.info("Attempting to dismount near %f %f %f", delta.x, delta.y, delta.z);
			BlockPos dlp = new BlockPos(delta);
			BlockPos dhp = new BlockPos(delta.add(new Vec3d(0, 1, 0)));
			if (!world.isBlockLoaded(dhp) || !world.isBlockLoaded(dlp)) {
				return new Vec3d(0, -1, 0);
			}
			IBlockState dhstate = world.getBlockState(dhp);
			IBlockState dlstate = world.getBlockState(dlp);

			if (delta.x - Math.floor(delta.x) > 0.7) {
				delta = new Vec3d(Math.floor(delta.x) + 0.7, delta.y, delta.z);
			} else if (delta.x - Math.floor(delta.x) < 0.3) {
				delta = new Vec3d(Math.floor(delta.x) + 0.3, delta.y, delta.z);
			}
			if (delta.z - Math.floor(delta.z) > 0.7) {
				delta = new Vec3d(delta.x, delta.y, Math.floor(delta.z) + 0.7);
			} else if (delta.z - Math.floor(delta.z) < 0.3) {
				delta = new Vec3d(delta.x, delta.y, Math.floor(delta.z) + 0.3);
			}

			if ((dhstate.getCollisionBoundingBox(world, dhp) == null || BlockUtil.isIRRail(world, dhp)) && (dlstate.getCollisionBoundingBox(world, dlp) == null || BlockUtil.isIRRail(world, dlp))) {
				if (!passenger.getEntityBoundingBox().offset(delta.subtract(passenger.getPositionVector())).intersects(this.getCollisionBoundingBox())) {
					// ImmersiveRailroading.info("Possible dismount at %s %s %s", delta.x, delta.y, delta.z);
					possibleDismount.add(delta);
				}
				// ImmersiveRailroading.warn("Train obstructing dismount at %s %s %s", delta.x, delta.y, delta.z);
				ImmersiveRailroading.warn("This is not intended to happen! Please contact the devs with the relavant train: %s", this.getDefinition().name());
			} else {
				// ImmersiveRailroading.info("Block obstructing dismount at %s %s %s", delta.x, delta.y, delta.z);
				break;
			}
		}
		if(!possibleDismount.isEmpty()) {
			// ImmersiveRailroading.info("Successful dismount at %s %s %s", delta.x, delta.y, delta.z);
			return possibleDismount.get(possibleDismount.size() - 1);
		}
		
		// ImmersiveRailroading.info("All dismounts obstructed, attempting to revert to default dismount point", delta.x, delta.y, delta.z);
		if (this instanceof EntityMoveableRollingStock) {
			if(((EntityMoveableRollingStock)this).getCurrentSpeed().metric() / ConfigDamage.entitySpeedDamage > 1) {
				passenger.sendMessage(ChatText.DISMOUNT_FAIL_DEFAULT.getMessage());
				return new Vec3d(0,-1,0);
			} 
		}
		delta = this.getPositionVector().add(new Vec3d(0, this.getDefinition().getHeight(gauge), 0));
		BlockPos dlp = new BlockPos(delta);
		BlockPos dhp = new BlockPos(delta.add(new Vec3d(0, 1, 0)));
		if (!world.isBlockLoaded(dhp) || !world.isBlockLoaded(dlp)) {
			return new Vec3d(0,-1,0);
		}
		IBlockState dhstate = world.getBlockState(dhp);
		IBlockState dlstate = world.getBlockState(dlp);
		if (dhstate.getCollisionBoundingBox(world, dhp) == null && dlstate.getCollisionBoundingBox(world, dlp) == null) {
			//ImmersiveRailroading.info("Default position at %s %s %s clear, dismounting.", delta.x, delta.y, delta.z);
			return delta;
		}
		
		//ImmersiveRailroading.info("Default position at %s %s %s obstructed. Dismount failed.", delta.x, delta.y, delta.z);
		passenger.sendMessage(ChatText.DISMOUNT_FAIL_DEFAULT.getMessage());
		return new Vec3d(0,-1,0);
	}

	public void handlePassengerPositions(Map<UUID, Vec3d> passengerPositions) {
		this.passengerPositions = passengerPositions;
		for (Entity passenger : this.getPassengers()) {
			this.updatePassenger(passenger);
		}
	}
	
	public static class StaticPassenger {
		public ResourceLocation ident;
		public NBTTagCompound data;
		public UUID uuid;
		public float rotation;
		private BlockPos startPos;
		public boolean isVillager;
		public Object cache;

		public StaticPassenger(EntityLiving entityliving ) {
			ident = EntityList.getKey(entityliving);
			data = entityliving.writeToNBT(new NBTTagCompound());
			uuid = entityliving.getPersistentID();
			startPos = entityliving.getPosition();
			isVillager = entityliving instanceof EntityVillager;
			rotation = (float) (Math.random() * 360);
		}
		
		public StaticPassenger(NBTTagCompound init) {
			ident = new ResourceLocation(init.getString("ident"));
			data = init.getCompoundTag("data");
			uuid = UUID.fromString(init.getString("uuid"));
			rotation = init.getFloat("rotation");
			startPos = NBTUtil.getPosFromTag(init.getCompoundTag("pos"));
			isVillager = init.getBoolean("isVillager");
		}
		
		public NBTTagCompound writeNBT() {
			NBTTagCompound init = new NBTTagCompound();
			init.setString("ident", ident.toString());
			init.setTag("data", data);
			init.setString("uuid", uuid.toString());
			init.setFloat("rotation", rotation);
			init.setTag("pos", NBTUtil.createPosTag(startPos));
			init.setBoolean("isVillager", isVillager);
			return init;
		}

		public EntityLiving respawn(World world, Vec3d pos) {
			Entity ent = EntityList.createEntityByIDFromName(ident, world);
			ent.readFromNBT(data);
			ent.setPosition(pos.x, pos.y, pos.z);
			return (EntityLiving) ent;
		}
	}

	public void addStaticPassenger(EntityLiving entityliving, Vec3d pos) {
		StaticPassenger sp = new StaticPassenger(entityliving);
		staticPassengers.add(sp);
		
		Vec3d center = this.getDefinition().getPassengerCenter(gauge);
		center = VecUtil.rotateWrongYaw(center, this.rotationYaw);
		center = center.add(this.getPositionVector());
		Vec3d off = VecUtil.rotateWrongYaw(center.subtract(pos), -this.rotationYaw);
		
		off = this.getDefinition().correctPassengerBounds(gauge, off);
		int wiggle = sp.isVillager ? 10 : 2;
		off = off.addVector((Math.random()-0.5) * wiggle, 0, (Math.random()-0.5) * wiggle);
		off = this.getDefinition().correctPassengerBounds(gauge, off);
		off = off.addVector(0, -off.y, 0);
		
		passengerPositions.put(sp.uuid, off);
		sendToObserving(new PassengerPositionsPacket(this));
		entityliving.setDead();
		
	}
	
	public EntityLiving removeStaticPasssenger(Vec3d pos, boolean isVillager) {
		if (staticPassengers.size() > 0) {
			int index = -1;
			for (int i = staticPassengers.size()-1; i >= 0; i--) {
				if (staticPassengers.get(i).isVillager == isVillager) {
					index = i;
					break;
				}
			}
			if (index == -1) {
				return null;
			}
			StaticPassenger passenger = staticPassengers.get(index);
			staticPassengers.remove(index);
			Vec3d ppos = passengerPositions.remove(passenger.uuid);
			
			sendToObserving(new PassengerPositionsPacket(this));
			
			if (passenger.isVillager) {
				ppos = dismountPos(ppos).addVector(0, 1, 0);

				double distanceMoved = pos.distanceTo(new Vec3d(passenger.startPos));

				int payout = (int) Math.floor(distanceMoved * Config.ConfigBalance.villagerPayoutPerMeter);
				
				List<Item> payouts = Config.ConfigBalance.getVillagerPayout(); 
				if (payouts.size() != 0) {
					int type = (int)(Math.random() * 100) % payouts.size();
					world.spawnEntity(new EntityItem(world, pos.x, pos.y, pos.z, new ItemStack(payouts.get(type), payout)));
				}
				
				pos = ppos;
			}
			
			EntityLiving ent = passenger.respawn(world, pos);
			world.spawnEntity(ent);
			
			return ent;
		}
		return null;
	}
	
	@Override
	public void onDeath(StockDeathType type) {
		super.onDeath(type);
		
		while (this.removeStaticPasssenger(this.getPositionVector(), true) != null) {
			//Unmounts all riding ents
		}
		
		while (this.removeStaticPasssenger(this.getPositionVector(), false) != null) {
			//Unmounts all riding ents
		}
	}
}
