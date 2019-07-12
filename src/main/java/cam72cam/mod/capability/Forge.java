package cam72cam.mod.capability;

import cam72cam.mod.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
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

public class Forge {
    /*
    public static <T> T getCapability(Object target, Capability<T> capability) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return target instanceof IInventory ? inventoryCapability((IInventory)target) : null;
        }
        if (capability == CapabilityEnergy.ENERGY) {
            return target instanceof IEnergy ? energyCapability((IEnergy)target) : null;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return target instanceof ITank ? fluidCapability((ITank)target) : null;
        }
        return null;
    }

    private static <T> T fluidCapability(ITank target) {
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
                return target.fill(new cam72cam.mod.fluid.FluidStack(resource), !doFill);
            }

            @Nullable
            @Override
            public FluidStack drain(FluidStack resource, boolean doDrain) {
                return target.drain(new cam72cam.mod.fluid.FluidStack(resource), !doDrain).internal;
            }

            @Nullable
            @Override
            public FluidStack drain(int maxDrain, boolean doDrain) {
                return target.drain(new cam72cam.mod.fluid.FluidStack(new FluidStack(target.getContents().internal, maxDrain)), doDrain).internal;
            }
        });
    }

    private static <T> T inventoryCapability(IInventory target) {
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

    private static <T> T energyCapability(IEnergy target) {
        return CapabilityEnergy.ENERGY.cast(new IEnergyStorage() {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                return target.recieveEnergy(maxReceive, simulate);
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
    */
}
