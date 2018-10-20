package cam72cam.immersiverailroading.entity;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.LockType;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class EntityLockableRollingStock extends Entity {
	protected String defID;
	
	private static DataParameter<Integer> LOCK_TYPE = EntityDataManager.createKey(EntityRollingStock.class, DataSerializers.VARINT);	//0=everyone can enter/break, 1=everyone can enter, 2=no one can enter/break
	private static DataParameter<String> LOCK_OWNER = EntityDataManager.createKey(EntityRollingStock.class, DataSerializers.STRING);

	public EntityLockableRollingStock(World world, String defID) {
		super(world);
		
		this.defID = defID;
		
		this.getDataManager().register(LOCK_TYPE, 0);
		this.getDataManager().register(LOCK_OWNER, "");
	}
	
	public boolean canDisassemble (EntityPlayer player) {
		return getLockOwner() == null || getLockOwner().equals(player.getPersistentID()) || playerHasOp(player);
	}
	
	public boolean playerHasOp (EntityPlayer player) {
		return  FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null;
	}
	
	public boolean hasPermission (EntityPlayer player) {
		return getLockType() == LockType.UNLOCKED || (getLockOwner().equals(player.getPersistentID()) || getLockOwner() == null) || playerHasOp(player);
	}
	
	public boolean canRide (Entity entity) {
		if (entity instanceof EntityPlayer) {
			return hasPermission((EntityPlayer) entity);
		}
		return true;
	}
	
	public LockType getLockType () {
		return LockType.values()[this.getDataManager().get(LOCK_TYPE)];
	}
	
	public void setLockType (LockType type) {
		this.getDataManager().set(LOCK_TYPE, type.ordinal());
	}
	
	public UUID getLockOwner() {
		if (this.getDataManager().get(LOCK_OWNER) == "") {
			return null;
		}
		return UUID.fromString(this.getDataManager().get(LOCK_OWNER));
	}
	
	public void setLockOwner(String playerId) {
		this.getDataManager().set(LOCK_OWNER, playerId);
	}
	
	public void switchLockType (EntityPlayer player) {
		setLockType(getLockType().next());
		setLockOwner(EntityPlayer.getUUID(player.getGameProfile()).toString());
		player.sendMessage(new TextComponentString(String.format(ChatText.CHANGE_STOCK_LOCK.toString(), getLockType())));
	}
	
	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt) {
		nbt.setString("lock_type", getLockType().toString());
		nbt.setString("lock_owner", getLockOwner() != null ? getLockOwner().toString() : "");
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt) {
		setLockType(LockType.valueOf(nbt.getString("lock_type")));
		setLockOwner(nbt.getString("lock_owner"));
	}

	@Override
	public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
		if (player.getHeldItem(hand).getItem() == IRItems.ITEM_LOCK_KEY && hasPermission(player)) {
			switchLockType(player);
			return true;
		}
		return false;
	}

	@Override
	protected void entityInit() {
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
}
