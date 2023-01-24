package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config.ConfigBalance;
import cam72cam.immersiverailroading.inventory.FilteredStackHandler;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.registry.FreightDefinition;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Living;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.serialization.TagField;

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

		if (getWorld().isServer) {
			for (int i = 0; i < cargoItems.getSlotCount(); i++) {
				ItemStack stack = cargoItems.get(i);
				if (!stack.isEmpty()) {
					getWorld().dropItem(stack.copy(), getPosition());
					stack.setCount(0);
				}
			}
		}
	}

	@Override
	public ClickResult onClick(Player player, Player.Hand hand) {
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
			if (getWorld().isClient && !leashed.isEmpty()) {
				return ClickResult.ACCEPTED;
			}
			if (player.hasPermission(Permissions.BOARD_WITH_LEAD)) {
				for (Living entity : leashed) {
					if (canFitPassenger(entity)) {
						entity.unleash(player);
						this.addPassenger(entity);
						return ClickResult.ACCEPTED;
					}
				}
			}

			if (player.getHeldItem(hand).is(Fuzzy.LEAD)) {
				for (Entity passenger : this.getPassengers()) {
					if (passenger instanceof Living && !passenger.isVillager()) {
						if (getWorld().isServer) {
							Living living = (Living) passenger;
							if (living.canBeLeashedTo(player)) {
								this.removePassenger(living);
								living.setLeashHolder(player);
								player.getHeldItem(hand).shrink(1);
							}
						}
						return ClickResult.ACCEPTED;
					}
				}
			}
		}

		if (player.getHeldItem(hand).isEmpty() || player.getRiding() != this) {
			if (getWorld().isClient || openGui(player)) {
				return ClickResult.ACCEPTED;
			}
		}
		return ClickResult.PASS;
	}

	protected boolean openGui(Player player) {
		if (getInventorySize() == 0) {
			return false;
		}
		if (player.hasPermission(Permissions.FREIGHT_INVENTORY)) {
			GuiTypes.FREIGHT.open(player, this);
		}
		return true;
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
		percentFull = this.getInventorySize() > 0 ? stacksWithStuff * 100 / this.getInventorySize() : 100;
	}
	
	public int getPercentCargoFull() {
		return percentFull;
	}

	protected void initContainerFilter() {
		
	}

	@Override
	public double getWeight() {
		double fLoad = ConfigBalance.blockWeight * itemCount;
		fLoad = fLoad + super.getWeight();
		return fLoad;
	}
}