package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.fluid.Fluid;

public class HandCar extends Locomotive {

	@Override
	public double getAppliedTractiveEffort(Speed speed) {
		int passengers = 0;
		for (Entity passenger : this.getPassengers()) {
			if (passenger.isPlayer()) {
				Player player = passenger.asPlayer();
				if (!player.isCreative()) {
					if (player.getFoodLevel() > 0) {
						passengers++;
					}
				} else {
					passengers++;
				}
			}
		}
		// Same function as Diesel for now
		return this.getDefinition().getHorsePower(gauge) * (308 / speed.imperial()) * 4.4482 * passengers;
	}

	@Override
	protected boolean forceLinkThrottleReverser() {
		// Always linked
		return true;
	}

	@Override
	public void onTick() {
		super.onTick();

		if (getWorld().isClient) {
			return;
		}

		if (getTrainBrake() > 0) {
			this.setTrainBrake(0);
		}

		if (this.getThrottle() != 0 && this.getTickCount() % (int)(600 * (1.1-this.getThrottle())) == 0) {
			for (Entity passenger : this.getPassengers()) {
				if (passenger.isPlayer()) {
					Player player = passenger.asPlayer();
					if (!player.isCreative()) {
						if (player.getFoodLevel() > 0) {
							player.useFood(1);
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
		return new ArrayList<>();
	}

	@Override
	public int getInventoryWidth() {
		return 2;
	}
}
