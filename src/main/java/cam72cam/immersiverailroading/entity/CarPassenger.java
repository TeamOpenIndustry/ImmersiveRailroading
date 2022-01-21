package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.registry.CarPassengerDefinition;

public class CarPassenger extends EntityCoupleableRollingStock {

    @Override
    public CarPassengerDefinition getDefinition() {
        return super.getDefinition(CarPassengerDefinition.class);
    }

    @Override
    public void onTick() {
        super.onTick();
    }
}
