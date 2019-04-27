package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.mod.entity.Entity;
import net.minecraftforge.fluids.Fluid;

public class HandCar extends Locomotive {
	
	public HandCar(net.minecraft.world.World world) {
		super(world);
	}

	@Override
	protected int getAvailableHP() {
		int passengers = 0;
		for (Entity passenger : this.getPassengers()) {
			if (passenger instanceof EntityPlayer) {
				EntityPlayer player = (EntityPlayer) passenger;
				if (!player.isCreative()) {
					if (player.getFoodStats().getFoodLevel() > 0) {
						passengers++;
					}
				} else {
					passengers++;
				}
			}
		}
		return this.getDefinition().getHorsePower(gauge) * passengers;
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		
		if (world.isClient) {
			return;
		}
		
		if (this.getThrottle() != 0 && this.ticksExisted % (int)(200 * (1.1-Math.abs(this.getThrottle()))) == 0) {
			for (Entity passenger : this.getPassengers()) {
				if (passenger instanceof EntityPlayer) {
					EntityPlayer player = (EntityPlayer) passenger;
					if (!player.isCreative()) {
						if (player.getFoodStats().getFoodLevel() > 0) {
							player.getFoodStats().setFoodLevel(player.getFoodStats().getFoodLevel() - 1);
						}
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
