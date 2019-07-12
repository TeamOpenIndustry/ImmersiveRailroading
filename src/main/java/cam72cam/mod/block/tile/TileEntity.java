package cam72cam.mod.block.tile;

import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.energy.IEnergy;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.fluid.ITank;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Identifier;
import cam72cam.mod.util.TagCompound;
import cam72cam.mod.world.World;
import com.google.common.collect.HashBiMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Function;

public class TileEntity extends net.minecraft.tileentity.TileEntity {
    public World world;
    public Vec3i pos;
    public boolean hasTileData;
    private String instanceId;
    private BlockEntity instance;

    /*
    Tile registration
    */

    private static final Map<String, Function<TileEntity, BlockEntity>> registry = HashBiMap.create();
    private TagCompound deferredLoad;

    public TileEntity() {
        // Forge reflection
        super();
    }

    public TileEntity(Identifier id) {
        this();
        instanceId = id.toString();
    }

    public static void register(Function<TileEntity, BlockEntity> instance, Identifier id) {
        registry.put(id.toString(), instance);
    }

    public final void register() {
        ResourceLocation currentName = TileEntity.getKey(this.getClass());
        if (currentName != null) {
            if (!currentName.toString().equals(getName().toString())) {
                throw new RuntimeException("Duplicate TileEntity registration with different name: " + currentName + " : " + getName());
            }
            // TODO log a warning here?
            return;
        }
        net.minecraft.tileentity.TileEntity.register(this.getName().internal.toString(), this.getClass());
    }

    public Identifier getName() {
        //TODO use MODID
        return new Identifier("notreallyamodthismightbreak", "hack");
    }


    /*
    Standard Tile function overrides
    */

    @Override
    public void setWorld(net.minecraft.world.World world) {
        super.setWorld(world);
        this.world = World.get(world);
    }
    @Override
    protected void setWorldCreate(net.minecraft.world.World worldIn) {
        super.setWorld(worldIn);
        this.world = World.get(worldIn);
    }
    @Override
    public void setPos(BlockPos pos) {
        super.setPos(pos);
        this.pos = new Vec3i(pos);
    }


    @Override
    public final void readFromNBT(NBTTagCompound compound) {
        hasTileData = true;
        load(new TagCompound(compound));
    }
    @Override
    public final NBTTagCompound writeToNBT(NBTTagCompound compound) {
        save(new TagCompound(compound));
        return compound;
    }

    @Override
    public final SPacketUpdateTileEntity getUpdatePacket() {
        TagCompound nbt = new TagCompound();
        this.writeToNBT(nbt.internal);
        this.writeUpdate(nbt);

        return new SPacketUpdateTileEntity(this.getPos(), 1, nbt.internal);
    }
    @Override
    public final void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        hasTileData = true;
        this.readFromNBT(pkt.getNbtCompound());
        this.readUpdate(new TagCompound(pkt.getNbtCompound()));
        super.onDataPacket(net, pkt);
        if (updateRerender()) {
            world.internal.markBlockRangeForRenderUpdate(getPos(), getPos());
        }
    }

    @Override
    public final NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        if (this.isLoaded()) {
            this.writeToNBT(tag);
            this.writeUpdate(new TagCompound(tag));
        }
        return tag;
    }
    @Override
    public final void handleUpdateTag(NBTTagCompound tag) {
        hasTileData = true;
        this.readFromNBT(tag);
        this.readUpdate(new TagCompound(tag));
        super.handleUpdateTag(tag);
        if (updateRerender()) {
            world.internal.markBlockRangeForRenderUpdate(getPos(), getPos());
        }
    }



    @Override
    public void markDirty() {
        super.markDirty();
        if (world.isServer) {
            world.internal.notifyBlockUpdate(getPos(), world.internal.getBlockState(getPos()), world.internal.getBlockState(getPos()), 1 + 2 + 8);
            world.internal.notifyNeighborsOfStateChange(pos.internal, this.getBlockType(), true);
        }
    }

    /* Forge Overrides */

    @Override
    public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, @Nullable net.minecraft.util.EnumFacing facing) {
        //TODO more efficient
        return getCapability(capability, facing) != null;
    }

    @Override
    @Nullable
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable net.minecraft.util.EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            ITank target = getTank(Facing.from(facing));
            if (target == null) {
                return null;
            }
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(new IFluidHandler() {
                @Override
                public IFluidTankProperties[] getTankProperties() {
                    return new IFluidTankProperties[] {
                        new IFluidTankProperties() {
                            @Nullable
                            @Override
                            public FluidStack getContents() {
                                return target.getContents().internal;
                            }

                            @Override
                            public int getCapacity() {
                                return target.getCapacity();
                            }

                            @Override
                            public boolean canFill() {
                                return true;
                            }

                            @Override
                            public boolean canDrain() {
                                return true;
                            }

                            @Override
                            public boolean canFillFluidType(FluidStack fluidStack) {
                                return target.allows(Fluid.getFluid(fluidStack.getFluid()));
                            }

                            @Override
                            public boolean canDrainFluidType(FluidStack fluidStack) {
                                return target.allows(Fluid.getFluid(fluidStack.getFluid()));
                            }
                        }
                    };
                }

                @Override
                public int fill(FluidStack resource, boolean doFill) {
                    int res = target.fill(new cam72cam.mod.fluid.FluidStack(resource), !doFill);
                    return res;
                }

                @Nullable
                @Override
                public FluidStack drain(FluidStack resource, boolean doDrain) {
                    return target.drain(new cam72cam.mod.fluid.FluidStack(resource), !doDrain).internal;
                }

                @Nullable
                @Override
                public FluidStack drain(int maxDrain, boolean doDrain) {
                    if (target.getContents().internal == null) {
                        return null;
                    }
                    return target.drain(new cam72cam.mod.fluid.FluidStack(new FluidStack(target.getContents().internal, maxDrain)), doDrain).internal;
                }
            });
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            IInventory target = getInventory(Facing.from(facing));
            if (target == null) {
                return null;
            }
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new IItemHandlerModifiable() {
                @Override
                public int getSlots() {
                    return target.getSlotCount();
                }

                @Override
                public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
                    target.set(slot, new cam72cam.mod.item.ItemStack(stack));
                }

                @Nonnull
                @Override
                public ItemStack getStackInSlot(int slot) {
                    return target.get(slot).internal;
                }

                @Nonnull
                @Override
                public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                    return target.insert(slot, new cam72cam.mod.item.ItemStack(stack), simulate).internal;
                }

                @Nonnull
                @Override
                public ItemStack extractItem(int slot, int amount, boolean simulate) {
                    return target.extract(slot, amount, simulate).internal;
                }

                @Override
                public int getSlotLimit(int slot) {
                    return target.getLimit(slot);
                }
            });
        }
        if (capability == CapabilityEnergy.ENERGY) {
            IEnergy target = getEnergy(Facing.from(facing));
            return CapabilityEnergy.ENERGY.cast(new IEnergyStorage() {
                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    return target.receiveEnergy(maxReceive, simulate);
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    return target.extractEnergy(maxExtract, simulate);
                }

                @Override
                public int getEnergyStored() {
                    return target.getEnergyStored();
                }

                @Override
                public int getMaxEnergyStored() {
                    return target.getMaxEnergyStored();
                }

                @Override
                public boolean canExtract() {
                    return true;
                }

                @Override
                public boolean canReceive() {
                    return true;
                }
            });
        }
        return null;
    }

    /*
    Wrapped functionality
    */

    public void setWorld(World world) {
        super.setWorld(world.internal);
    }
    public void setPos(Vec3i pos) {
        super.setPos(pos.internal);
    }

    public void load(TagCompound data) {
        super.readFromNBT(data.internal);
        pos = new Vec3i(super.pos);

        if (instanceId == null) {
            // If this fails something is really wrong
            instanceId = data.getString("instanceId");
            if (instanceId == null) {
                throw new RuntimeException("Unable to load instanceid with " + data.toString());
            }
        }

        if (instance() != null) {
            instance().load(data);
        } else {
            deferredLoad = data;
        }
    }
    public void save(TagCompound data) {
        super.writeToNBT(data.internal);
        data.setString("instanceId", instanceId);
        if (instance() != null) {
            instance().save(data);
        }
    }

    public void writeUpdate(TagCompound nbt) {
        if (instance() != null) {
            instance().writeUpdate(nbt);
        }
    }
    public void readUpdate(TagCompound nbt) {
        if (instance() != null) {
            instance().readUpdate(nbt);
        }
    }

    /*
    New Functionality
    */

    public boolean isLoaded() {
        return this.hasWorld() && (world.isServer || hasTileData);
    }

    // TODO render system?
    public boolean updateRerender() {
        return false;
    }

    public BlockEntity instance() {
        if (this.instance == null) {
            if (isLoaded()) {
                if (this.instanceId == null) {
                    System.out.println("WAT NULL");
                }
                if (!registry.containsKey(instanceId)) {
                    System.out.println("WAT " + instanceId);
                }
                this.instance = registry.get(this.instanceId).apply(this);
                if (deferredLoad != null) {
                    this.instance.load(deferredLoad);
                }
                this.deferredLoad = null;
            }
        }
        return this.instance;
    }

    /* Capabilities */

    public IInventory getInventory(Facing side) {
        return instance() != null ? instance().getInventory(side) : null;
    }

    public ITank getTank(Facing side) {
        return instance() != null ? instance().getTank(side) : null;
    }

    public IEnergy getEnergy(Facing side) {
        return instance() != null ? instance().getEnergy(side) : null;
    }
}
