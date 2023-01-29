package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.entity.CarPassenger;
import cam72cam.immersiverailroading.gui.overlay.DataBlock;

public class CarPassengerDefinition extends CarFreightDefinition {

    public CarPassengerDefinition(String defID, DataBlock data) throws Exception {
        super(CarPassenger.class, defID, data);
    }

    @Override
    public void parseJson(DataBlock data) throws Exception {
        super.parseJson(data);
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
