package cam72cam.immersiverailroading.entity;
import cam72cam.immersiverailroading.Config.ConfigBalance;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.inventory.FilteredStackHandler;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.FreightDefinition;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.entity.DamageType;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Hand;
import cam72cam.mod.util.TagCompound;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.items.ItemStackHandler;

public abstract class Freight extends EntityCoupleableRollingStock {
	public FilteredStackHandler cargoItems = new FilteredStackHandler(0) {
        @Override
        protected void onContentsChanged(int slot) {
            // We need to tell the tile entity that something has changed so
            // that the chest getContents is persisted
        	Freight.this.onInventoryChanged();
        }
    };
    
	protected final static String CARGO_ITEMS = "CARGO_ITEMS";
	protected final static String PERCENT_FULL = "PERCENT_FULL";

	public Freight(ModdedEntity entity) {
		super(entity);

		this.sync.setInteger(CARGO_ITEMS, 0);
		this.sync.setInteger(PERCENT_FULL, 0);
	}
	
	protected void onInventoryChanged() {
		if (getWorld().isServer) {
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
		
		if (getWorld().isServer) {
			for (int slot = 0; slot < cargoItems.getSlots(); slot++) {
				ItemStack itemstack = cargoItems.getStackInSlot(slot);
				if (itemstack.getCount() != 0) {
					Vec3d pos = getPosition().add(VecUtil.fromWrongYaw(4, this.getRotationYaw()+90));
					getWorld().dropItem(new cam72cam.mod.item.ItemStack(itemstack.copy()), new Vec3i(pos));
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

		for (EntityLiving entityliving : getWorld().internal.getEntitiesWithinAABB(EntityLiving.class, new AxisAlignedBB((double) i - dist, (double) j - 7.0D,
				(double) k - dist, (double) i + dist, (double) j + dist, (double) k + dist))) {
			if (entityliving.getLeashed() && entityliving.getLeashHolder() == player.internal) {
				if (canFitPassenger(new Entity(entityliving)) && this.getDefinition().acceptsLivestock()) {
					entityliving.clearLeashed(true, !player.isCreative());
					this.addPassenger(new Entity(entityliving));
					return ClickResult.ACCEPTED;
				}
			}
		}

		if (player.getHeldItem(Hand.PRIMARY).item == Items.LEAD) {
			Entity passenger = this.removePassenger((ModdedEntity.StaticPassenger sp) -> !sp.isVillager);
			if (passenger != null) {
				EntityLiving living = passenger.asInternal(EntityLiving.class);
				if (living.canBeLeashedTo(player.internal)) {
					living.setLeashHolder(player.internal, true);
					player.getHeldItem(Hand.PRIMARY).shrink(1);
				}
				return ClickResult.ACCEPTED;
			}
		}
		
		// I don't believe the positions are used
		if (guiType() != null) {
			player.internal.openGui(ImmersiveRailroading.instance, guiType().ordinal(), getWorld().internal, this.internal.getEntityId(), 0, 0);
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
		this.sync.setInteger(CARGO_ITEMS, itemInsideCount);
		this.sync.setInteger(PERCENT_FULL, stacksWithStuff * 100 / this.getInventorySize());
		this.sync.send();
	}
	
	public int getPercentCargoFull() {
		return this.sync.getInteger(PERCENT_FULL);
	}

	@Override
	public void save(TagCompound data) {
		super.save(data);
		data.set("items", new TagCompound(cargoItems.serializeNBT()));
	}

	@Override
	public void load(TagCompound data) {
		super.load(data);
		ItemStackHandler temp = new ItemStackHandler();
		temp.deserializeNBT(data.get("items").internal);
		cargoItems.setSize(this.getInventorySize());
		for (int slot = 0; slot < temp.getSlots(); slot ++) {
			if (slot < cargoItems.getSlots()) {
				cargoItems.setStackInSlot(slot, temp.getStackInSlot(slot));
			} else {
				getWorld().dropItem(new cam72cam.mod.item.ItemStack(temp.getStackInSlot(slot)), getPosition());
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

		if (this.isDead() && getWorld().isServer) {
			for (int slot = 0; slot < cargoItems.getSlots(); slot++) {
				ItemStack itemstack = cargoItems.getStackInSlot(slot);
				if (itemstack.getCount() != 0) {
					getWorld().dropItem(new cam72cam.mod.item.ItemStack(itemstack.copy()), getPosition());
					itemstack.setCount(0);
				}
			}
		}
	}
	
	@Override
	public double getWeight() {
		double fLoad = ConfigBalance.blockWeight * this.sync.getInteger(CARGO_ITEMS);
		fLoad = fLoad + super.getWeight();
		return fLoad;
	}
}