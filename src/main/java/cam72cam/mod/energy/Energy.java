package cam72cam.mod.energy;

public class Energy implements IEnergy {
    private int stored;
    private final int max;

    public Energy(int maxStorage) {
        this.stored = 0;
        this.max = maxStorage;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int delta = Math.min(maxReceive, max - stored);
        if (!simulate) {
            this.stored += delta;
        }
        return delta;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int delta = Math.min(maxExtract, stored);
        if (!simulate) {
            this.stored -= delta;
        }
        return delta;
    }

    @Override
    public int getEnergyStored() {
        return stored;
    }

    @Override
    public int getMaxEnergyStored() {
        return max;
    }
}
