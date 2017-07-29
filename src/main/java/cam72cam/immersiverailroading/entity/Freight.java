package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiTypes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public abstract class Freight extends EntityCoupleableRollingStock {
	protected ItemStackHandler cargoItems = new ItemStackHandler(0) {
        @Override
        protected void onContentsChanged(int slot) {
            // We need to tell the tile entity that something has changed so
            // that the chest contents is persisted
        	Freight.this.onInventoryChanged();
        }
    };
    
	protected static DataParameter<Integer> CARGO_MASS = EntityDataManager.createKey(Freight.class, DataSerializers.VARINT);

	public Freight(World world, String defID) {
		super(world, defID);
		
		this.getDataManager().register(CARGO_MASS, 0);
	}
	
	protected void onInventoryChanged() {
		if (!world.isRemote ) {
			handleMass();
		}
	}

	public abstract int getInventorySize();

	/*
	 * 
	 * EntityRollingStock Overrides
	 */

	@Override
	public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
		if ((super.processInitialInteract(player, hand))) {
			return true;
		}
		// I don't believe the positions are used
		player.openGui(ImmersiveRailroading.instance, guiType().ordinal(), world, this.getEntityId(), 0, 0);
		return true;
	}

	protected GuiTypes guiType() {
		return GuiTypes.FREIGHT;
	}

	/**
	 * Handle mass depending on item count
	 */
	protected void handleMass() {
		int itemInsideCount = 0;
		for (int slot = 0; slot < cargoItems.getSlots(); slot++) {
			itemInsideCount += cargoItems.getStackInSlot(slot).getCount();
		}
		this.getDataManager().set(CARGO_MASS, itemInsideCount);
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		nbttagcompound.setTag("items", cargoItems.serializeNBT());
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		cargoItems.deserializeNBT((NBTTagCompound) nbttagcompound.getTag("items"));
	}
	
	@Override
	public void rollingStockInit() {
		super.rollingStockInit();
		cargoItems.setSize(this.getInventorySize());
	}
	
	@Override
	public void setDead() {
		super.setDead();
		
		for (int slot = 0; slot < cargoItems.getSlots(); slot++) {
			ItemStack itemstack = cargoItems.getStackInSlot(slot);
			if (itemstack.getCount() != 0) {
				this.dropItem(itemstack.getItem(), itemstack.getCount());
				itemstack.setCount(0);
			}
		}
	}
	


    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
	@Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) cargoItems;
        }
        return super.getCapability(capability, facing);
    }
}