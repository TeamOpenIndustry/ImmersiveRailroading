package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.registry.BrakeVanDefinition;
import cam72cam.immersiverailroading.util.BurnUtil;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.fluid.Fluid;

import java.util.List;

public class BrakeVan extends ControllableStock {

    @Override
    public int getInventoryWidth() {
        return 2;
    }

    @Override
    public BrakeVanDefinition getDefinition() {
        return super.getDefinition(BrakeVanDefinition.class);
    }

    @Override
    public void onTick() {
        super.onTick();
    }

    /*
     * Sets the throttle or brake on all connected diesel locomotives if the throttle or brake has been changed
     */
    @Override
    public void handleKeyPress(Player source, KeyTypes key) {
        super.handleKeyPress(source, key);
    }

    @Override
    protected void realSetThrottle(float newThrottle) { }

    @Override
    protected void realAirBrake(float newAirBrake) {
        super.setAirBrake(newAirBrake);
    }

    @Override
    public void setThrottle(float newThrottle) {
        realSetThrottle(newThrottle);
    }

    @Override
    public void setAirBrake(float newAirBrake) {
        realAirBrake(newAirBrake);
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
