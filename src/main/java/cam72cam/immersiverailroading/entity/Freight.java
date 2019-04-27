package cam72cam.immersiverailroading.entity;
import cam72cam.immersiverailroading.Config.ConfigBalance;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.inventory.FilteredStackHandler;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.FreightDefinition;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.entity.DamageType;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Hand;
import cam72cam.mod.util.TagCompound;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public abstract class Freight extends EntityCoupleableRollingStock {
	protected FilteredStackHandler cargoItems = new FilteredStackHandler(0) {
        @Override
        protected void onContentsChanged(int slot) {
            // We need to tell the tile entity that something has changed so
            // that the chest contents is persisted
        	Freight.this.onInventoryChanged();
        }
    };
    
	protected static DataParameter<Integer> CARGO_ITEMS = EntityDataManager.createKey(Freight.class, DataSerializers.VARINT);
	protected static DataParameter<Integer> PERCENT_FULL = EntityDataManager.createKey(Freight.class, DataSerializers.VARINT);

	public Freight(net.minecraft.world.World world) {
		super(world);
		
		this.getDataManager().register(CARGO_ITEMS, 0);
		this.getDataManager().register(PERCENT_FULL, 0);
	}
	
	protected void onInventoryChanged() {
		if (world.isServer) {
			handleMass();
		}
	}

	public abstract int getInventorySize();

	public boolean showCurrentLoadOnly() {
		return this.getDefinition().shouldShowCurrentLoadOnly();
	}
	
	@Override
	public FreightDefinition getDefinition() {
		return this.getDefinition(FreightDefinition.class);
	}

	/*
	 * 
	 * EntityRollingStock Overrides
	 */
	
	@Override
	public void onAssemble() {
		super.onAssemble();
		this.cargoItems.setSize(this.getInventorySize());
	}
	
	@Override
	public void onDissassemble() {
		super.onDissassemble();
		
		if (world.isServer) {
			for (int slot = 0; slot < cargoItems.getSlots(); slot++) {
				ItemStack itemstack = cargoItems.getStackInSlot(slot);
				if (itemstack.getCount() != 0) {
					Vec3d pos = self.getPosition().add(VecUtil.fromWrongYaw(4, this.rotationYaw+90));
					world.dropItem(new cam72cam.mod.item.ItemStack(itemstack.copy()), new Vec3i(pos));
					itemstack.setCount(0);
				}
			}
		}
		
		this.cargoItems.setSize(0);
	}

	@Override
	public ClickResult onClick(Player player, Hand hand) {
		ClickResult clickRes = super.onClick(player, hand);
		if (clickRes != ClickResult.PASS) {
			return clickRes;
		}

		if (!this.isBuilt()) {
			return ClickResult.PASS;
		}

		// See ItemLead.attachToFence
		double dist = 10.0D;
		double i = player.internal.posX;
		double j = player.internal.posY;
		double k = player.internal.posZ;

		for (EntityLiving entityliving : world.internal.getEntitiesWithinAABB(EntityLiving.class, new AxisAlignedBB((double) i - dist, (double) j - 7.0D,
				(double) k - dist, (double) i + dist, (double) j + dist, (double) k + dist))) {
			if (entityliving.getLeashed() && entityliving.getLeashHolder() == player.internal) {
				if (canFitPassenger(entityliving) && this.getDefinition().acceptsLivestock()) {
					entityliving.clearLeashed(true, !player.isCreative());
					this.addPassenger(new Entity(entityliving));
					return ClickResult.ACCEPTED;
				}
			}
		}

		if (player.getHeldItem(Hand.PRIMARY).item == Items.LEAD) {
			Entity passenger = this.removePassenger((StaticPassenger sp) -> !sp.isVillager);
			if (passenger != null) {
				EntityLiving living = passenger.as(EntityLiving.class);
				if (living.canBeLeashedTo(player.internal)) {
					living.setLeashHolder(player.internal, true);
					player.getHeldItem(Hand.PRIMARY).shrink(1);
				}
				return ClickResult.ACCEPTED;
			}
		}
		
		// I don't believe the positions are used
		if (guiType() != null) {
			player.internal.openGui(ImmersiveRailroading.instance, guiType().ordinal(), world.internal, this.getEntityId(), 0, 0);
			return ClickResult.ACCEPTED;
		}
		return ClickResult.PASS;
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
	public void save(TagCompound nbttagcompound) {
		super.save(nbttagcompound);
		nbttagcompound.set("items", new TagCompound(cargoItems.serializeNBT()));
	}

	@Override
	public void load(TagCompound nbttagcompound) {
		super.load(nbttagcompound);
		ItemStackHandler temp = new ItemStackHandler();
		temp.deserializeNBT(nbttagcompound.get("items").internal);
		cargoItems.setSize(this.getInventorySize());
		for (int slot = 0; slot < temp.getSlots(); slot ++) {
			if (slot < cargoItems.getSlots()) {
				cargoItems.setStackInSlot(slot, temp.getStackInSlot(slot));
			} else {
				world.dropItem(new cam72cam.mod.item.ItemStack(temp.getStackInSlot(slot)), self.getPosition());
			}
		}
		handleMass();
		initContainerFilter();
	}
	
	protected void initContainerFilter() {
		
	}
	
	@Override
	public void onDamage(DamageType type, Entity source, float amount) {
		super.onDamage(type, source, amount);

		if (this.isDead() && world.isServer) {
			for (int slot = 0; slot < cargoItems.getSlots(); slot++) {
				ItemStack itemstack = cargoItems.getStackInSlot(slot);
				if (itemstack.getCount() != 0) {
					world.dropItem(new cam72cam.mod.item.ItemStack(itemstack.copy()), self.getPosition());
					itemstack.setCount(0);
				}
			}
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