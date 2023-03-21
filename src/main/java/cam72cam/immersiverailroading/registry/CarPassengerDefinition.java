package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.entity.CarPassenger;
import cam72cam.immersiverailroading.util.DataBlock;

public class CarPassengerDefinition extends CarFreightDefinition {

    public CarPassengerDefinition(String defID, DataBlock data) throws Exception {
        super(CarPassenger.class, defID, data);
    }

    @Override
    public void loadData(DataBlock data) throws Exception {
        super.loadData(data);
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
