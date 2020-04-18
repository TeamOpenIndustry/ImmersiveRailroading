package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.CraftingMachineMode;
import cam72cam.immersiverailroading.multiblock.Multiblock.MultiblockInstance;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.immersiverailroading.net.MultiblockSelectCraftPacket;
import cam72cam.mod.block.BlockEntityTickable;
import cam72cam.mod.energy.Energy;
import cam72cam.mod.energy.IEnergy;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.ItemStackHandler;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
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
	private ItemStackHandler container = new ItemStackHandler(0) {
        @Override
        protected void onContentsChanged(int slot) {
        	markDirty();
        }

		@Override
		public int getLimit(int slot) {
			if (isLoaded()) {
				return Math.min(super.getLimit(slot), getMultiblock().getSlotLimit(offset, slot));
			}
			return 0;
		}
    };
    
    private Energy energy = new Energy(1000) {
    	@Override
        public int receive(int maxReceive, boolean simulate) {
    		int val = super.receive(maxReceive, simulate);
    		if (!simulate && val != 0 && isLoaded()) {
    			markDirty();
    		}
    		return val;
    	}
    	
    	@Override
        public int extract(int maxExtract, boolean simulate) {
    		int val = super.extract(maxExtract, simulate);
    		if (!simulate && val != 0 && isLoaded()) {
    			markDirty();
    		}
    		return val;
    	}
    };

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
	public void save(TagCompound nbt) {
		if (container != null) {
			nbt.set("inventory", container.save());
		}
		if (energy != null) {
            nbt.setInteger("energy", energy.getCurrent());
		}
	}

	@Override
	public void load(TagCompound nbt) {
		container.load(nbt.get("inventory"));
		// Empty and then refill energy storage
		energy.extract(energy.getCurrent(), false);
		energy.receive(nbt.getInteger("energy"), false);
	}

	/* TODO RENDER
	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return Math.pow(ImmersiveRailroading.proxy.getRenderDistance()*16, 2);
	}
	*/

	@Override
	public void update() {
		if (offset == null) {
			// Not formed yet.  World may onTick before configure is called
			return;
		}
		this.ticks += 1;
		if (getMultiblock() != null) {
			this.getMultiblock().tick(offset);
		}
	}

	/* TODO RENDER
    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
    	return INFINITE_EXTENT_AABB;
    }
    */
	
	public Vec3i getOrigin() {
		return pos.subtract(offset.rotate(rotation));
	}
	
	public MultiblockInstance getMultiblock() {
		if (this.mb == null && this.isLoaded()) {
			this.mb = MultiblockRegistry.get(name).instance(world, getOrigin(), rotation);
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

	public boolean onBlockActivated(Player player, Hand hand) {
		return getMultiblock().onBlockActivated(player, hand, offset);
	}
	
	/*
	 * Event Handlers
	 */
	
	public void onBreakEvent() {
		for (int slot = 0; slot < container.getSlotCount(); slot ++) {
			ItemStack item = container.get(slot);
			if (!item.isEmpty()) {
				world.dropItem(item, pos);
			}
		}

		world.setBlock(pos, replaced);
	}

	public boolean isRender() {
		return getMultiblock().isRender(offset);
	}

	public double getRotation() {
		return 180 - Facing.EAST.rotate(rotation).getHorizontalAngle();
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
		if (world.isServer) {
			if (craftMode != mode) {
				craftMode = mode;
				this.markDirty();
			}
		} else {
			new MultiblockSelectCraftPacket(pos, craftItem, mode).sendToServer();
		}
	}
	
	public ItemStack getCraftItem() {
		return craftItem;
	}

	public void setCraftItem(ItemStack selected) {
		if (world.isServer) {
			if (craftItem == null || selected == null || !(selected.equals(craftItem) && selected.getTagCompound().equals(craftItem.getTagCompound()))) {
				this.craftItem = selected == null ? null : selected.copy();
				this.craftProgress = 0;
				this.markDirty();
			}
		} else {
			new MultiblockSelectCraftPacket(pos, selected, craftMode).sendToServer();
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
			world.setToAir(pos);
		}
	}

	@Override
	public boolean onClick(Player player, Hand hand, Facing facing, Vec3d hit) {
		return onBlockActivated(player, hand);
	}

	@Override
	public ItemStack onPick() {
		return ItemStack.EMPTY;
	}
}
