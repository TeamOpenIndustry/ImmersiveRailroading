package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.entity.CarPassenger;
import com.google.gson.JsonObject;

public class CarPassengerDefinition extends EntityRollingStockDefinition {

    private boolean hasInternalLighting;

    public CarPassengerDefinition(String defID, JsonObject data) throws Exception {
        super(CarPassenger.class, defID, data);
    }

    @Override
    public void parseJson(JsonObject data) throws Exception {
        super.parseJson(data);
        JsonObject properties = data.get("properties").getAsJsonObject();
        hasInternalLighting = !properties.has("internalLighting") || properties.get("internalLighting").getAsBoolean();
    }

    @Override
    public boolean acceptsPassengers() {
        return true;
    }

    public boolean hasInternalLighting() {
        return hasInternalLighting;
    }
}
