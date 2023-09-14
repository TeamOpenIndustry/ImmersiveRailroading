package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.CarPassenger;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.resource.Identifier;

public class CarPassengerDefinition extends CarFreightDefinition {

    public CarPassengerDefinition(String defID, DataBlock data) throws Exception {
        super(CarPassenger.class, defID, data);
    }

    @Override
    protected Identifier defaultDataLocation() {
        return new Identifier(ImmersiveRailroading.MODID, "rolling_stock/default/passenger.caml");
    }

    @Override
    public boolean acceptsPassengers() {
        return true;
    }

    @Override
    public boolean acceptsLivestock() {
        return false;
    }
}
