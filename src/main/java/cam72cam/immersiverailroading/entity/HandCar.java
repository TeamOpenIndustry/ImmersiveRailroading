package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.util.FluidQuantity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;

public class HandCar extends Locomotive {
	
	public HandCar(World world) {
		super(world, null);
	}
	public HandCar(World world, String defID) {
		super(world, defID);
	}

	@Override
	protected int getAvailableHP() {
		return this.getDefinition().getHorsePower(gauge) * this.getPassengers().size();
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		
		if (world.isRemote) {
			return;
		}
		
		if (this.getThrottle() != 0 && this.ticksExisted % (int)(200 * (1.1-Math.abs(this.getThrottle()))) == 0) {
			for (Entity passenger : this.getPassengers()) {
				if (passenger instanceof EntityPlayer) {
					EntityPlayer player = (EntityPlayer) passenger;
					if (!player.isCreative()) {
						player.getFoodStats().setFoodLevel(player.getFoodStats().getFoodLevel() - 1);
					}
				}
			}
		}
	}

	@Override
	public FluidQuantity getTankCapacity() {
		return FluidQuantity.ZERO;
	}

	@Override
	public List<Fluid> getFluidFilter() {
		return new ArrayList<Fluid>();
	}
}
