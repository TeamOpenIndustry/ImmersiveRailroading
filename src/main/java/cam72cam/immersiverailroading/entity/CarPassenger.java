package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.registry.CarPassengerDefinition;

public class CarPassenger extends EntityCoupleableRollingStock {
    private boolean hasLocomotivePower;
    private int gotLocomotiveTick = -1;

    @Override
    public CarPassengerDefinition getDefinition() {
        return super.getDefinition(CarPassengerDefinition.class);
    }

    @Override
    public void onTick() {
        super.onTick();
        if (getWorld().isClient) {
            boolean hadLocomotive = hasLocomotivePower;
            hasLocomotivePower = false;
            this.mapTrain(this, false, stock -> {
                if (stock instanceof Locomotive && stock.internalLightsEnabled()) {
                    hasLocomotivePower = true;
                    if (!hadLocomotive) {
                        gotLocomotiveTick = getTickCount();
                    }
                }
            });
        }
    }

    @Override
    public boolean internalLightsEnabled() {
        return getDefinition().hasInternalLighting() && hasLocomotivePower && (
                        gotLocomotiveTick == -1 ||
                        getTickCount() - gotLocomotiveTick > 15 ||
                        ((getTickCount() - gotLocomotiveTick)/(int)((Math.random()+2) * 4)) % 2 == 0
                );
    }
}
