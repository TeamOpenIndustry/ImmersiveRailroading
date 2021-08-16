package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.registry.CarPassengerDefinition;

public class CarPassenger extends EntityCoupleableRollingStock {
    private boolean hasLocomotive;
    private int gotLocomotiveTick = -1;

    @Override
    public CarPassengerDefinition getDefinition() {
        return super.getDefinition(CarPassengerDefinition.class);
    }

    @Override
    public void onTick() {
        super.onTick();
        if (getWorld().isClient) {
            boolean hadLocomotive = hasLocomotive;
            hasLocomotive = false;
            this.mapTrain(this, false, stock -> {
                if (stock instanceof Locomotive) {
                    hasLocomotive = true;
                    if (!hadLocomotive) {
                        gotLocomotiveTick = getTickCount();
                    }
                }
            });
        }
    }

    @Override
    public boolean internalLightsEnabled() {
        return getDefinition().hasInternalLighting() && hasLocomotive && (
                        gotLocomotiveTick == -1 ||
                        getTickCount() - gotLocomotiveTick > 15 ||
                        ((getTickCount() - gotLocomotiveTick)/(int)((Math.random()+2) * 4)) % 2 == 0
                );
    }
}
