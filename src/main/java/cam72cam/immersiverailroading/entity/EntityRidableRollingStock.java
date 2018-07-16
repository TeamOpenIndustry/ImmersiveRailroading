package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.StockDeathType;
import cam72cam.immersiverailroading.net.PassengerPositionsPacket;
import cam72cam.immersiverailroading.util.BufferUtil;
import cam72cam.immersiverailroading.util.VecUtil;
import io.netty.buffer.ByteBuf;
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
	private final double pressDist = 0.05;
	public List<StaticPassenger> staticPassengers = new ArrayList<StaticPassenger>();
	
	public void handleKeyPress(Entity source, KeyTypes key) {
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
			movement = VecUtil.rotateYaw(movement, source.getRotationYawHead());
			movement = VecUtil.rotateYaw(movement, 180-this.rotationYaw);
			
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
			center = VecUtil.rotateYaw(center, this.rotationYaw);
			center = center.add(this.getPositionVector());
			Vec3d off = VecUtil.rotateYaw(center.subtract(ppos), -this.rotationYaw);
			
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
			pos = pos.add(passengerPositions.get(passenger.getPersistentID()));
			pos = VecUtil.rotateYaw(pos, this.rotationYaw);
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
			
			passengerPositions.remove(passenger.getPersistentID());
			passenger.setPositionAndUpdate(delta.x, passenger.posY, delta.z);
		}
	}
	
	public Vec3d dismountPos(Vec3d ppos) {
		Vec3d delta = VecUtil.fromYaw(this.getDefinition().getPassengerCompartmentWidth(gauge)/2 + 1.3 * gauge.scale(), this.rotationYaw + (ppos.z > 0 ? 90 : -90));
		
		ppos = ppos.add(this.getDefinition().getPassengerCenter(gauge));
		Vec3d offppos = VecUtil.rotateYaw(ppos, this.rotationYaw);
		
		delta = delta.addVector(offppos.x, offppos.y, 0);
		delta = delta.add(this.getPositionVector());
		return new Vec3d(delta.x, this.posY, delta.z);
	}

	public void handlePassengerPositions(Map<UUID, Vec3d> passengerPositions) {
		this.passengerPositions = passengerPositions;
		for (UUID id : passengerPositions.keySet()) {
			for (Entity ent : world.loadedEntityList) {
				if (ent.getPersistentID().equals(id)) {
					Vec3d pos = this.getDefinition().getPassengerCenter(gauge);
					pos = pos.add(passengerPositions.get(id));
					pos = VecUtil.rotateYaw(pos, this.rotationYaw);
					pos = pos.add(this.getPositionVector());
					if (ent instanceof EntityPlayer && shouldRiderSit()) {
						pos = pos.subtract(0, 0.75, 0);
					}
					ent.setPosition(pos.x, pos.y, pos.z);
					break;
				}
			}
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
		center = VecUtil.rotateYaw(center, this.rotationYaw);
		center = center.add(this.getPositionVector());
		Vec3d off = VecUtil.rotateYaw(center.subtract(pos), -this.rotationYaw);
		
		off = this.getDefinition().correctPassengerBounds(gauge, off);
		int wiggle = sp.isVillager ? 10 : 2;
		off = off.addVector((Math.random()-0.5) * wiggle, 0, (Math.random()-0.5) * wiggle);
		off = this.getDefinition().correctPassengerBounds(gauge, off);
		off = off.addVector(0, -off.y, 0);
		
		passengerPositions.put(sp.uuid, off);
		entityliving.setDead();
		
		sendToObserving(new PassengerPositionsPacket(this));
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
				ppos = dismountPos(ppos);

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
