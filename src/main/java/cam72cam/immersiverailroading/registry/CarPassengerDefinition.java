package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.entity.CarPassenger;
import com.google.gson.JsonObject;

public class CarPassengerDefinition extends EntityRollingStockDefinition {

    CarPassengerDefinition(String defID, JsonObject data) throws Exception {
        super(CarPassenger.class, defID, data);
    }

    @Override
    public boolean acceptsPassengers() {
        return true;
    }
}
