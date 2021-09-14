package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.registry.CarPassengerDefinition;

public class CarPassenger extends EntityCoupleableRollingStock {
    private boolean hadElectricalPower = false;
    private int gotElectricalPowerTick = -1;

    @Override
    public CarPassengerDefinition getDefinition() {
        return super.getDefinition(CarPassengerDefinition.class);
    }

    @Override
    public void onTick() {
        super.onTick();
        if (getWorld().isClient) {
            if (!hadElectricalPower && hasElectricalPower()) {
                gotElectricalPowerTick = getTickCount();
            }
        }
        hadElectricalPower = hasElectricalPower();
    }

    @Override
    public boolean internalLightsEnabled() {
        return getDefinition().hasInternalLighting() && hasElectricalPower() && (
                        gotElectricalPowerTick == -1 ||
                        getTickCount() - gotElectricalPowerTick > 15 ||
                        ((getTickCount() - gotElectricalPowerTick)/(int)((Math.random()+2) * 4)) % 2 == 0
                );
    }
}
