package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.registry.ControlCarDefinition;
import cam72cam.immersiverailroading.util.BurnUtil;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.fluid.Fluid;

import java.util.List;

public class ControlCar extends ControllableStock {

    @Override
    public int getInventoryWidth() {
        return 2;
    }

    @Override
    public ControlCarDefinition getDefinition() {
        return super.getDefinition(ControlCarDefinition.class);
    }

    /*
     * Sets the throttle or brake on all connected diesel locomotives if the throttle or brake has been changed
     */

    @Override
    public void setThrottle(float newThrottle) {
        realSetThrottle(newThrottle);
        if (this.getDefinition().multiUnitCapable) {
            this.mapTrain(this, true, false, this::setThrottleMap);
        }
    }

    @Override
    public void setAirBrake(float newAirBrake) {
        realAirBrake(newAirBrake);
        this.mapTrain(this, true, false, this::setThrottleMap);
    }

    @Override
    public List<Fluid> getFluidFilter() {
        return BurnUtil.burnableFluids();
    }

    @Override
    public FluidQuantity getTankCapacity() {
        return FluidQuantity.ZERO;
    }
}
