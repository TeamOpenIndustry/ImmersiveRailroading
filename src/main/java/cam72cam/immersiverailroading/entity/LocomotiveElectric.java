package cam72cam.immersiverailroading.entity;

import net.minecraft.item.Item;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.energy.IEnergyStorage;

public class LocomotiveElectric extends Locomotive {
	
	public LocomotiveElectric(World world) {
		this(world, null);
	}
	
	public LocomotiveElectric(World world, String defID) {
		super(world, defID);
	}
	
	@Override
	public int getInventorySize() {
		return 5 + 5 + 5 + 1;
	}

	public int[] getLocomotiveInventorySizes() {
		return new int[]{5,5,5};
	}
	
	public double getMaxFuel() {
		return 1000.0;
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		if (world.isRemote) {
			return;
		}
		this.setState("hot");
		
		/* if the loco has fuel */
		if (getFuel() < getMaxFuel() && cargoItems.getStackInSlot(0) != null)
		{
			Item item = cargoItems.getStackInSlot(0).getItem();
			if (item instanceof IEnergyStorage) {
				final double RFtoRE = 10; // redstoneEnergy conversion factor to RF e.g. RF = redstoneEnergy * REtoRF
				final double REtoRF = 1 / RFtoRE; // redstoneEnergy conversion factor to RF e.g. RF = redstoneEnergy * REtoRF
				final int maxDraw = 200; // maximum amount of redstoneEnergy to draw from the item per tick
				int draw = MathHelper.floor(Math.min(maxDraw, getMaxFuel() - getFuel()) * REtoRF); // amount of energy to attempt to draw this tick
				addFuel(((IEnergyStorage) item).extractEnergy(draw, false) * RFtoRE);
			}
		}
	}
	
	@Override
	public int getFuelDiv(int i) {
		return (int) ((this.getFuel() * (i)) / getMaxFuel());
	}

	@Override
	public int getTankCapacity() {
		return 0;
	}
}
