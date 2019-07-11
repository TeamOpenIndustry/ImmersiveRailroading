package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.CraftingMachineMode;
import cam72cam.immersiverailroading.multiblock.Multiblock.MultiblockInstance;
import cam72cam.immersiverailroading.net.MultiblockSelectCraftPacket;

import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.mod.block.BlockEntityTickable;
import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.util.TagCompound;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.ItemStackHandler;

public class TileMultiblock extends BlockEntityTickable {
	
	private IBlockState replaced;
	private Vec3i offset;
	private Rotation rotation;
	private String name;
	private CraftingMachineMode craftMode = CraftingMachineMode.STOPPED;
	private long ticks;
	private MultiblockInstance mb;
	
	//Crafting
	private int craftProgress = 0;
	private ItemStack craftItem = ItemStack.EMPTY;
	private ItemStackHandler container = new ItemStackHandler(0) {
        @Override
        protected void onContentsChanged(int slot) {
        	markDirty();
        }

		@Override
		public int getSlotLimit(int slot) {
			if (isLoaded()) {
				return Math.min(super.getSlotLimit(slot), getMultiblock().getSlotLimit(offset.internal, slot));
			}
			return 0;
		}
    };
    
    private EnergyStorage energy = new EnergyStorage(1000) {
    	@Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
    		int val = super.receiveEnergy(maxReceive, simulate);
    		if (!simulate && val != 0 && isLoaded()) {
    			markDirty();
    		}
    		return val;
    	}
    	
    	@Override
        public int extractEnergy(int maxExtract, boolean simulate) {
    		int val = super.extractEnergy(maxExtract, simulate);
    		if (!simulate && val != 0 && isLoaded()) {
    			markDirty();
    		}
    		return val;
    	}
    };

	public TileMultiblock(TileEntity internal) {
		super(internal);
	}

	public boolean isLoaded() {
    	return this.name != null;
    }

	public void configure(String name, Rotation rot, Vec3i offset, IBlockState replaced) {
		this.name = name;
		this.rotation = rot;
		this.offset = offset;
		this.replaced = replaced;
		
		container.setSize(this.getMultiblock().getInvSize(offset.internal));
		
		markDirty();
	}

	@Override
	public void save(TagCompound nbt) {
		if (name != null) {
			// Probably in some weird block break path

            nbt.setString("name", name);
            nbt.setInteger("rotation", rotation.ordinal());
            nbt.setVec3i("offset", offset);
            nbt.set("replaced", new TagCompound(NBTUtil.writeBlockState(new NBTTagCompound(), replaced)));

            nbt.set("inventory", new TagCompound(container.serializeNBT()));
            nbt.set("craftItem", craftItem.toTag());
            nbt.setInteger("craftProgress", craftProgress);
            nbt.setInteger("craftMode", craftMode.ordinal());

            nbt.setInteger("energy", energy.getEnergyStored());
		}
	}

	@Override
	public void load(TagCompound nbt) {
		name = nbt.getString("name");
		rotation = Rotation.values()[nbt.getInteger("rotation")];
		offset = nbt.getVec3i("offset");
		replaced = NBTUtil.readBlockState(nbt.get("replaced").internal);
		
		container.deserializeNBT(nbt.get("inventory").internal);
		craftItem = new ItemStack(nbt.get("craftItem"));
		craftProgress = nbt.getInteger("craftProgress");
		
		craftMode = CraftingMachineMode.STOPPED;
		if (nbt.hasKey("craftMode")) {
			craftMode = CraftingMachineMode.values()[nbt.getInteger("craftMode")];
		}
		
		// Empty and then refill energy storage
		energy.extractEnergy(energy.getEnergyStored(), false);
		energy.receiveEnergy(nbt.getInteger("energy"), false);
	}

	@Override
	public void writeUpdate(TagCompound nbt) {

	}

	@Override
	public void readUpdate(TagCompound nbt) {

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
		this.getMultiblock().tick(offset.internal);
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
			this.mb = MultiblockRegistry.get(name).instance(world.internal, getOrigin().internal, rotation.internal);
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
		return getMultiblock().onBlockActivated(player.internal, hand.internal, offset.internal);
	}
	
	/*
	 * Event Handlers
	 */
	
	public void onBreakEvent() {
		for (int slot = 0; slot < container.getSlots(); slot ++) {
			net.minecraft.item.ItemStack item = container.extractItem(slot, Integer.MAX_VALUE, false);
			if (!item.isEmpty()) {
				world.dropItem(new ItemStack(item), pos);
			}
		}
		world.internal.removeTileEntity(pos.internal);
		world.internal.setBlockState(pos.internal, replaced, 3);
	}

	public boolean isRender() {
		return getMultiblock().isRender(offset.internal);
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
			ImmersiveRailroading.net.sendToServer(new MultiblockSelectCraftPacket(pos, craftItem, mode));
		}
	}
	
	public ItemStack getCraftItem() {
		return craftItem;
	}

	public void setCraftItem(ItemStack selected) {
		if (world.isServer) {
			if (craftItem == null || selected == null || !net.minecraft.item.ItemStack.areItemStacksEqualUsingNBTShareTag(selected.internal, craftItem.internal)) {
				this.craftItem = selected.copy();
				this.craftProgress = 0;
				this.markDirty();
			}
		} else {
			ImmersiveRailroading.net.sendToServer(new MultiblockSelectCraftPacket(pos, selected, craftMode));
		}
	}
	
	/*
	 * Capabilities
	 */

	/* TODO CAPABILITIES
	@Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if (this.isLoaded()) {
	        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
	            return this.getMultiblock().getInvSize(offset.internal) != 0;
	        }
	        if (capability == CapabilityEnergy.ENERGY) {
	        	return this.getMultiblock().canRecievePower(offset.internal);
	        }
		}
        return super.hasCapability(capability, facing);
    }

	@Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (this.isLoaded()) {
	        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
	        	if (this.getMultiblock().getInvSize(offset.internal) != 0) {
	        		return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new IItemHandlerModifiable()  {
						@Override
						public int getSlots() {
							return container.getSlots();
						}
						@Override
						public net.minecraft.item.ItemStack getStackInSlot(int slot) {
							return container.getStackInSlot(slot);
						}
						@Override
	        	        public net.minecraft.item.ItemStack insertItem(int slot, @Nonnull net.minecraft.item.ItemStack stack, boolean simulate) {
	        	        	if (getMultiblock().canInsertItem(offset.internal, slot, stack)) {
	        	        		return container.insertItem(slot, stack, simulate);
	        	        	}
	        	        	return stack;
	        	        }
	        	        @Override
	        	        public net.minecraft.item.ItemStack extractItem(int slot, int amount, boolean simulate) {
	        	        	if (getMultiblock().isOutputSlot(offset.internal, slot)) {
	        	        		return container.extractItem(slot, amount, simulate);
	        	        	}
	        	        	return ItemStack.EMPTY.internal;
	        	        }
						@Override
						public int getSlotLimit(int slot) {
							return container.getSlotLimit(slot);
						}
						
						@Override
						public void setStackInSlot(int slot, net.minecraft.item.ItemStack stack) {
							container.setStackInSlot(slot, stack);
						}
	        		});
	        	}
	        }
	        if (capability == CapabilityEnergy.ENERGY) {
	        	if (this.getMultiblock().canRecievePower(offset.internal)) {
	        		return CapabilityEnergy.ENERGY.cast(this.energy);
	        	}
	        }
		}
        return super.getCapability(capability, facing);
    }
    */

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

	@Override
	public void onNeighborChange(Vec3i neighbor) {
	}
}
