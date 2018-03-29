package cam72cam.immersiverailroading.entity;
import cam72cam.immersiverailroading.Config.ConfigBalance;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.StockDeathType;
import cam72cam.immersiverailroading.sound.ISound;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
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

	private ISound wheel_sound;

	private float sndRand;
    
	protected static DataParameter<Integer> CARGO_ITEMS = EntityDataManager.createKey(Freight.class, DataSerializers.VARINT);
	protected static DataParameter<Integer> PERCENT_FULL = EntityDataManager.createKey(Freight.class, DataSerializers.VARINT);

	public Freight(World world, String defID) {
		super(world, defID);
		
		this.getDataManager().register(CARGO_ITEMS, 0);
		this.getDataManager().register(PERCENT_FULL, 0);
	}
	
	protected void onInventoryChanged() {
		if (!world.isRemote) {
			handleMass();
		}
	}

	public abstract int getInventorySize();

	/*
	 * 
	 * EntityRollingStock Overrides
	 */
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		
		if (world.isRemote) {
			if (ConfigSound.soundEnabled) {
				if (this.wheel_sound == null) {
					wheel_sound = ImmersiveRailroading.proxy.newSound(this.getDefinition().wheel_sound, true, 40, gauge);
					this.sndRand = (float)Math.random()/10;
				}
				
				if (Math.abs(this.getCurrentSpeed().metric()) > 5) {
					if (!wheel_sound.isPlaying()) {
						wheel_sound.play(this.getPositionVector());
					}
					float adjust = (float) Math.abs(this.getCurrentSpeed().metric()) / 300;
					wheel_sound.setPitch(adjust + 0.7f + this.sndRand);
					wheel_sound.setVolume(0.5f * adjust);
					
					wheel_sound.setPosition(getPositionVector());
					wheel_sound.setVelocity(getVelocity());
					wheel_sound.update();
				} else {
					if (wheel_sound.isPlaying()) {
						wheel_sound.stop();;
					}
				}
			}
		}
	}
	
	@Override
	public void onAssemble() {
		super.onAssemble();
		this.cargoItems.setSize(this.getInventorySize());
	}
	
	@Override
	public void onDissassemble() {
		super.onDissassemble();
		
		if (!world.isRemote) {		
			for (int slot = 0; slot < cargoItems.getSlots(); slot++) {
				ItemStack itemstack = cargoItems.getStackInSlot(slot);
				if (itemstack.getCount() != 0) {
					Vec3d pos = this.getPositionVector().add(VecUtil.fromYaw(4, this.rotationYaw+90));
					world.spawnEntity(new EntityItem(this.world, pos.x, pos.y, pos.z, itemstack.copy()));
					itemstack.setCount(0);
				}
			}
		}
		
		this.cargoItems.setSize(0);
	}

	@Override
	public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
		if ((super.processInitialInteract(player, hand))) {
			return true;
		}
		
		if (!this.isBuilt()) {
			return false;
		}

		// See ItemLead.attachToFence
		double dist = 10.0D;
		double i = player.posX;
		double j = player.posY;
		double k = player.posZ;

		for (EntityLiving entityliving : world.getEntitiesWithinAABB(EntityLiving.class, new AxisAlignedBB((double) i - dist, (double) j - 7.0D,
				(double) k - dist, (double) i + dist, (double) j + dist, (double) k + dist))) {
			if (entityliving.getLeashed() && entityliving.getLeashHolder() == player) {
				if (canFitPassenger(entityliving) && this.getDefinition().acceptsLivestock()) {
					entityliving.clearLeashed(true, !player.isCreative());
					this.addStaticPassenger(entityliving, player.getPositionVector());
					return true;
				}
			}
		}

		if (player.getHeldItemMainhand().getItem() == Items.LEAD) {
			EntityLiving passenger = this.removeStaticPasssenger(player.getPositionVector(), false);
			if (passenger != null) {
				if (passenger.canBeLeashedTo(player)) {
					passenger.setLeashHolder(player, true);
					player.getHeldItemMainhand().shrink(1);
					return true;
				}
				return true;
			}
		}
		
		// I don't believe the positions are used
		if (guiType() != null) {
			player.openGui(ImmersiveRailroading.instance, guiType().ordinal(), world, this.getEntityId(), 0, 0);
			return true;
		}
		return false;
	}

	protected GuiTypes guiType() {
		return GuiTypes.FREIGHT;
	}

	/**
	 * Handle mass depending on item count
	 */
	protected void handleMass() {
		int itemInsideCount = 0;
		int stacksWithStuff = 0;
		for (int slot = 0; slot < cargoItems.getSlots(); slot++) {
			itemInsideCount += cargoItems.getStackInSlot(slot).getCount();
			if (cargoItems.getStackInSlot(slot).getCount() != 0) {
				stacksWithStuff += 1;
			}
		}
		this.getDataManager().set(CARGO_ITEMS, itemInsideCount);
		this.getDataManager().set(PERCENT_FULL, stacksWithStuff * 100 / this.getInventorySize());
	}
	
	public int getPercentCargoFull() {
		return this.getDataManager().get(PERCENT_FULL);
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		nbttagcompound.setTag("items", cargoItems.serializeNBT());
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		ItemStackHandler temp = new ItemStackHandler();
		temp.deserializeNBT((NBTTagCompound) nbttagcompound.getTag("items"));
		cargoItems.setSize(this.getInventorySize());
		for (int slot = 0; slot < temp.getSlots(); slot ++) {
			if (slot < cargoItems.getSlots()) {
				cargoItems.setStackInSlot(slot, temp.getStackInSlot(slot));
			} else {
				world.spawnEntity(new EntityItem(this.world, this.posX, this.posY, this.posZ, temp.getStackInSlot(slot)));
			}
		}
		handleMass();
	}
	
	@Override
	public void onDeath(StockDeathType type) {
		super.onDeath(type);
		
		if (world.isRemote) {
			return;
		}
		
		for (int slot = 0; slot < cargoItems.getSlots(); slot++) {
			ItemStack itemstack = cargoItems.getStackInSlot(slot);
			if (itemstack.getCount() != 0) {
				world.spawnEntity(new EntityItem(this.world, this.posX, this.posY, this.posZ, itemstack.copy()));
				itemstack.setCount(0);
			}
		}
	}
	
	@Override
	public void setDead() {
		super.setDead();
		if (this.wheel_sound != null) {
			wheel_sound.stop();
		}
	}
	
	@Override
	public double getWeight() {
		double fLoad = ConfigBalance.blockWeight * this.getDataManager().get(CARGO_ITEMS);
		fLoad = fLoad + super.getWeight();
		return fLoad;
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