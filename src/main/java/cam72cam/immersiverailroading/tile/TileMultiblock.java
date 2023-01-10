package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.CraftingMachineMode;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.multiblock.Multiblock.MultiblockInstance;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.immersiverailroading.net.MultiblockSelectCraftPacket;
import cam72cam.mod.block.BlockEntityTickable;
import cam72cam.mod.energy.Energy;
import cam72cam.mod.energy.IEnergy;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.ItemStackHandler;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.util.Facing;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.world.BlockInfo;

public class TileMultiblock extends BlockEntityTickable {

	@TagField("replaced")
	private BlockInfo replaced;
	@TagField("offset")
	private Vec3i offset;
	@TagField("rotation")
	private Rotation rotation;
	@TagField("name")
	private String name;
	@TagField("craftMode")
	private CraftingMachineMode craftMode = CraftingMachineMode.STOPPED;
	private long ticks;
	private MultiblockInstance mb;
	
	//Crafting
	@TagField("craftProgress")
	private int craftProgress = 0;
	@TagField("craftItem")
	private ItemStack craftItem = ItemStack.EMPTY;
	@TagField
	private ItemStackHandler container = new ItemStackHandler(0);
	@TagField("energyStorage")
    private Energy energy = new Energy(0, 1000);

	public boolean isLoaded() {
			//TODO FIX ME bad init
    	return this.name != null && this.name.length() != 0;
    }

	public void configure(String name, Rotation rot, Vec3i offset, BlockInfo replaced) {
		this.name = name;
		this.rotation = rot;
		this.offset = offset;
		this.replaced = replaced;
		
		container.setSize(this.getMultiblock().getInvSize(offset));
		
		markDirty();
	}

	@Override
	public void load(TagCompound nbt) {
		container.onChanged(slot -> this.markDirty());
		container.setSlotLimit(slot -> getMultiblock().getSlotLimit(offset, slot));
		energy.onChanged(this::markDirty);
	}

	@Override
	public void update() {
		this.ticks += 1;

		if (offset != null && getMultiblock() != null) {
			this.getMultiblock().tick(offset);
		} else if (ticks > 20) {
			System.out.println("Error in multiblock, reverting");
			getWorld().breakBlock(getPos());
		}
	}

	@Override
	public IBoundingBox getRenderBoundingBox() {
		return IBoundingBox.INFINITE;
	}

	public Vec3i getOrigin() {
		return getPos().subtract(offset.rotate(rotation));
	}
	
	public MultiblockInstance getMultiblock() {
		if (this.mb == null && this.isLoaded()) {
			this.mb = MultiblockRegistry.get(name).instance(getWorld(), getOrigin(), rotation);
		}
		return this.mb;
	}
	
	public String getName() {
		return name;
	}
	
	public long getRenderTicks() {
		return this.ticks;
	}
	
	public ItemStackHandler getContainer() {
		if (container.getSlotCount() != getMultiblock().getInvSize(offset)) {
			container.setSize(getMultiblock().getInvSize(offset));
		}
		return this.container;
	}

	/*
	 * BlockType Functions to pass on to the multiblock
	 */
	public void breakBlock() {
		if (getMultiblock() != null) {
			getMultiblock().onBreak();
		}
	}

	public boolean onBlockActivated(Player player, Player.Hand hand) {
		return getMultiblock().onBlockActivated(player, hand, offset);
	}
	
	/*
	 * Event Handlers
	 */
	
	public void onBreakEvent() {
		for (int slot = 0; slot < container.getSlotCount(); slot ++) {
			ItemStack item = container.get(slot);
			if (!item.isEmpty()) {
				getWorld().dropItem(item, getPos());
			}
		}

		if (replaced != null) {
			getWorld().setBlock(getPos(), replaced);
		}
	}

	public boolean isRender() {
		return getMultiblock().isRender(offset);
	}

	public double getRotation() {
		return 180 - Facing.EAST.rotate(rotation).getAngle();
	}
	
	/*
	 * Crafting
	 */
	public int getCraftProgress() {
		return craftProgress;
	}
	
	public void setCraftProgress(int progress) {
		if (craftProgress != progress) {
			craftProgress = progress;
			this.markDirty();
		}
	}
	
	public CraftingMachineMode getCraftMode() {
		return craftMode;
	}
	
	public void setCraftMode(CraftingMachineMode mode) {
		if (getWorld().isServer) {
			if (craftMode != mode) {
				craftMode = mode;
				this.markDirty();
			}
		} else {
			new MultiblockSelectCraftPacket(getPos(), craftItem, mode).sendToServer();
		}
	}
	
	public ItemStack getCraftItem() {
		return craftItem;
	}

	public void setCraftItem(ItemStack selected) {
		if (getWorld().isServer) {
			if (selected == null || !selected.equals(craftItem)) {
				this.craftItem = selected == null ? null : selected.copy();
				this.craftProgress = 0;
				this.markDirty();
			}
		} else {
			new MultiblockSelectCraftPacket(getPos(), selected, craftMode).sendToServer();
		}
	}
	
	/*
	 * Capabilities
	 */

	@Override
	public IInventory getInventory(Facing facing) {
		if (this.getMultiblock() == null || this.getMultiblock().getInvSize(offset) == 0) {
			return null;
		}

		if (container.getSlotCount() != getMultiblock().getInvSize(offset)) {
			container.setSize(getMultiblock().getInvSize(offset));
		}

		return new IInventory() {
			@Override
			public int getSlotCount() {
				return container.getSlotCount();
			}

			@Override
			public ItemStack get(int slot) {
				return container.get(slot);
			}

			@Override
			public void set(int slot, ItemStack stack) {
				container.set(slot, stack);
			}

			@Override
			public ItemStack insert(int slot, ItemStack stack, boolean simulate) {
				if (getMultiblock().canInsertItem(offset, slot, stack)) {
					return container.insert(slot, stack, simulate);
				}
				return stack;
			}

			@Override
			public ItemStack extract(int slot, int amount, boolean simulate) {
				if (getMultiblock().isOutputSlot(offset, slot)) {
					return container.extract(slot, amount, simulate);
				}
				return ItemStack.EMPTY;
			}

			@Override
			public int getLimit(int slot) {
				return container.getLimit(slot);
			}
		};
	}

	@Override
	public IEnergy getEnergy(Facing facing) {
		return this.isLoaded() && this.getMultiblock().canRecievePower(offset) ? energy : null;
	}

	@Override
	public void onBreak() {
		try {
			// Multiblock break
			this.breakBlock();
		} catch (Exception ex) {
			ImmersiveRailroading.catching(ex);
			// Something broke
			// TODO figure out why
			getWorld().setToAir(getPos());
		}
	}

	@Override
	public boolean onClick(Player player, Player.Hand hand, Facing facing, Vec3d hit) {
		if (!player.hasPermission(Permissions.MACHINIST)) {
			return false;
		}
		return onBlockActivated(player, hand);
	}

	@Override
	public ItemStack onPick() {
		return ItemStack.EMPTY;
	}
}
