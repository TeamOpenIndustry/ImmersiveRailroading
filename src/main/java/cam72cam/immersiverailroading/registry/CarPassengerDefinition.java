package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.entity.CarPassenger;
import com.google.gson.JsonObject;

public class CarPassengerDefinition extends EntityRollingStockDefinition {

    public CarPassengerDefinition(String defID, JsonObject data) throws Exception {
        super(CarPassenger.class, defID, data);
    }

    @Override
    public void parseJson(JsonObject data) throws Exception {
        super.parseJson(data);
    }

    @Override
    public boolean acceptsPassengers() {
        return true;
    }
}
