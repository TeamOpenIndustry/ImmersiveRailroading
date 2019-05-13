package cam72cam.mod.capability;

import net.minecraftforge.energy.IEnergyStorage;

public abstract class Energy {
    IEnergyStorage internal() {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                return this.receiveEnergy(maxReceive, simulate);
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return this.extractEnergy(maxExtract, simulate);
            }

            @Override
            public int getEnergyStored() {
                return this.getEnergyStored();
            }

            @Override
            public int getMaxEnergyStored() {
                return this.getMaxEnergyStored();
            }

            @Override
            public boolean canExtract() {
                return this.canExtract();
            }

            @Override
            public boolean canReceive() {
                return this.canReceive();
            }
        };
    }

    public abstract int receiveEnergy(int maxReceive, boolean simulate);
    public abstract int extractEnergy(int maxExtract, boolean simulate);
    public abstract int getEnergyStored();
    public abstract int getMaxEnergyStored();
    public abstract boolean canExtract();
    public abstract boolean canReceive();
}
