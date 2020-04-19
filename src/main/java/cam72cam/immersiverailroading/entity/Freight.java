package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config.ConfigBalance;
import cam72cam.immersiverailroading.inventory.FilteredStackHandler;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.FreightDefinition;
import cam72cam.mod.entity.DamageType;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Living;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.gui.GuiRegistry;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.util.Hand;

import java.util.List;

public abstract class Freight extends EntityCoupleableRollingStock {
	@TagField("items")
	public FilteredStackHandler cargoItems = new FilteredStackHandler(0);

	@TagSync
	@TagField("CARGO_ITEMS")
	private int itemCount = 0;

	@TagSync
	@TagField("PERCENT_FULL")
	private int percentFull = 0;

	public abstract int getInventorySize();
	public abstract int getInventoryWidth();

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
		List<ItemStack> extras = cargoItems.setSize(this.getInventorySize());
		if (getWorld().isServer) {
			extras.forEach(stack -> getWorld().dropItem(stack, getPosition()));
			cargoItems.onChanged(slot -> handleMass());
			handleMass();
		}
		initContainerFilter();
	}
	
	@Override
	public void onDissassemble() {
		super.onDissassemble();

		List<ItemStack> extras = cargoItems.setSize(0);
		if (getWorld().isServer) {
			extras.forEach(stack -> getWorld().dropItem(stack, getPosition()));
		}
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
		if (this.getDefinition().acceptsLivestock()) {
			List<Living> leashed = getWorld().getEntities((Living e) -> e.getPosition().distanceTo(player.getPosition()) < 16 && e.isLeashedTo(player), Living.class);
			for (Living entity : leashed) {
				if (canFitPassenger(entity)) {
					entity.unleash(player);
					this.addPassenger(entity);
					return ClickResult.ACCEPTED;
				}
			}
		}

		if (player.getHeldItem(Hand.PRIMARY).is(Fuzzy.LEAD)) {
			for (Entity passenger : this.getPassengers()) {
				if (passenger instanceof Living && !passenger.isVillager()) {
					Living living = (Living) passenger;
					if (living.canBeLeashedTo(player)) {
						this.removePassenger(living);
						living.setLeashHolder(player);
						player.getHeldItem(Hand.PRIMARY).shrink(1);
					}
					return ClickResult.ACCEPTED;
				}
			}
		}
		
		if (guiType() != null) {
			guiType().open(player, this);
			return ClickResult.ACCEPTED;
		}
		return ClickResult.PASS;
	}

	protected GuiRegistry.EntityGUI guiType() {
		return GuiTypes.FREIGHT;
	}

	/**
	 * Handle mass depending on item count
	 */
	protected void handleMass() {
		int itemInsideCount = 0;
		int stacksWithStuff = 0;
		for (int slot = 0; slot < cargoItems.getSlotCount(); slot++) {
			itemInsideCount += cargoItems.get(slot).getCount();
			if (cargoItems.get(slot).getCount() != 0) {
				stacksWithStuff += 1;
			}
		}
		itemCount = itemInsideCount;
		percentFull = stacksWithStuff * 100 / this.getInventorySize();
	}
	
	public int getPercentCargoFull() {
		return percentFull;
	}

	protected void initContainerFilter() {
		
	}
	
	@Override
	public void onDamage(DamageType type, Entity source, float amount) {
		super.onDamage(type, source, amount);

		if (this.isDead() && getWorld().isServer) {
			for (int slot = 0; slot < cargoItems.getSlotCount(); slot++) {
				ItemStack itemstack = cargoItems.get(slot);
				if (itemstack.getCount() != 0) {
					getWorld().dropItem(itemstack.copy(), getPosition());
					itemstack.setCount(0);
				}
			}
		}
	}
	
	@Override
	public double getWeight() {
		double fLoad = ConfigBalance.blockWeight * itemCount;
		fLoad = fLoad + super.getWeight();
		return fLoad;
	}
}