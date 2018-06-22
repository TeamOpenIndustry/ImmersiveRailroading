package cam72cam.immersiverailroading.entity;

import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemLockKey;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.StockDeathType;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializer;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class EntityRollingStock extends Entity implements IEntityAdditionalSpawnData {
	
	public static final DataSerializer<UUID> PLAYER_UUID = new DataSerializer<UUID>()
    {
        public void write(PacketBuffer buf, UUID playerId)
        {
            buf.writeUniqueId(playerId);
        }
        public UUID read(PacketBuffer buf) throws IOException
        {
            return buf.readUniqueId();
        }
        public DataParameter<UUID> createKey(int id)
        {
            return new DataParameter<UUID>(id, this);
        }
        public UUID copyValue(UUID playerId)
        {
            return playerId;
        }
    };
	
	private static DataParameter<Integer> LOCK_TYPE = EntityDataManager.createKey(EntityRollingStock.class, DataSerializers.VARINT);	//0=everyone can enter/break, 1=everyone can enter, 2=no one can enter/break
	private static DataParameter<UUID> LOCK_OWNER = EntityDataManager.createKey(EntityRollingStock.class, PLAYER_UUID);
	
	protected String defID;
	public Gauge gauge;
	public String tag = "";

	public EntityRollingStock(World world, String defID) {
		super(world);

		this.defID = defID;

		super.preventEntitySpawning = true;
		super.isImmuneToFire = true;
		super.entityCollisionReduction = 1F;
		super.ignoreFrustumCheck = true;
		
		this.getDataManager().register(LOCK_TYPE, 0);
	}
	
	@Override
	public String getName() {
		return this.getDefinition().name();
	}

	public EntityRollingStockDefinition getDefinition() {
		return this.getDefinition(EntityRollingStockDefinition.class);
	}
	public <T extends EntityRollingStockDefinition> T getDefinition(Class<T> type) {
		EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
		if (def == null) {
			try {
				return type.getConstructor(String.class, JsonObject.class).newInstance(defID, (JsonObject)null);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
					| SecurityException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return type.cast(def);
		}
	}
	public String getDefinitionID() {
		return this.defID;
	}
	
	public int getLockType () {
		return this.getDataManager().get(LOCK_TYPE);
	}
	
	public void setLockType (int value) {
		this.getDataManager().set(LOCK_TYPE, value);
	}
	
	public void switchLockType () {
		int lockType = getLockType();
		lockType++;
		if (lockType == 3) {
			lockType = 0;
		}
		setLockType(lockType);
	}
	
	public UUID getLockOwner() {
		return this.getDataManager().get(LOCK_OWNER);
	}
	
	public void setLockOwner(UUID playerId) {
		this.getDataManager().set(LOCK_OWNER, playerId);
	}
	
	@Override
	public void onUpdate() {
		if (!world.isRemote && this.ticksExisted % 5 == 0) {
			EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
			if (def == null) {
				world.removeEntity(this);
			}
		}
	}

	/*
	 * 
	 * Data RW for Spawn and Entity Load
	 */

	@Override
	public void readSpawnData(ByteBuf additionalData) {
		defID = BufferUtil.readString(additionalData);
		gauge = Gauge.from(additionalData.readDouble());
		tag = BufferUtil.readString(additionalData);
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		BufferUtil.writeString(buffer, defID);
		buffer.writeDouble(gauge.value());
		BufferUtil.writeString(buffer, tag);
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		nbttagcompound.setString("defID", defID);
		nbttagcompound.setDouble("gauge", gauge.value());
		nbttagcompound.setString("tag", tag);
		nbttagcompound.setInteger("lock_type", getLockType());
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		defID = nbttagcompound.getString("defID");
		if (nbttagcompound.hasKey("gauge")) {
			gauge = Gauge.from(nbttagcompound.getDouble("gauge"));
		} else {
			gauge = Gauge.from(Gauge.STANDARD);
		}
		
		tag = nbttagcompound.getString("tag");
		setLockType(nbttagcompound.getInteger("lock_type"));
	}

	@Override
	protected void entityInit() {
	}

	/*
	 * Player Interactions
	 */
	
	@Override
	public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
		if (player.getHeldItem(hand).getItem() == IRItems.ITEM_LOCK_KEY) {
			System.out.println(getLockType());
			switchLockType();
			System.out.println(getLockType());
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean canBeCollidedWith() {
		// Needed for right click, probably a forge or MC bug
		return true;
	}
	
	public void onDeath(StockDeathType type) {
		setDead();
	}

	@Override
	public boolean attackEntityFrom(DamageSource damagesource, float amount) {
		if (world.isRemote) {
			return false;
		}
		
		if (damagesource.isExplosion()) {
			if (amount > 5) {
				if (!this.isDead) {
					this.onDeath(amount > 20 ? StockDeathType.CATACYSM : StockDeathType.EXPLOSION);
				}
				world.removeEntity(this);
				return false;
			}
		}
		
		if (damagesource.getTrueSource() instanceof EntityPlayer && !damagesource.isProjectile()) {
			EntityPlayer player = (EntityPlayer) damagesource.getTrueSource();
			if (player.isSneaking()) {
				if (!this.isDead) {
					this.onDeath(StockDeathType.PLAYER);
				}
				world.removeEntity(this);
				return false;
			}
		}
		
		return false;
	}
	
	@Override
	public <T extends Entity> Collection<T> getRecursivePassengersByType(Class<T> entityClass) {
		try {
			throw new Exception("Hack the planet");
		} catch (Exception ex) {
			for (StackTraceElement tl : ex.getStackTrace()) {
				if (tl.getFileName().contains("PlayerList.java")) {
					return new ArrayList<T>();
				}
			}
		}
		return super.getRecursivePassengersByType(entityClass);
	}

	@Override
	public boolean canBePushed() {
		return false;
	}

	/**
	 * @return Stock Weight in Kg
	 */
	public double getWeight() {
		return this.getDefinition().getWeight(gauge);
	}

	/*
	 * Helpers
	 */

	public void sendToObserving(IMessage packet) {
		ImmersiveRailroading.net.sendToAllAround(packet,
				new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, ImmersiveRailroading.ENTITY_SYNC_DISTANCE));
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public boolean isInRangeToRenderDist(double distance)
    {
        return true;
    }
	
	@Override
	public boolean shouldRenderInPass(int pass) {
		return false;
	}

	public void triggerResimulate() {
	}
}