package cam72cam.immersiverailroading.registry;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.CarPassenger;
import net.minecraft.world.World;

public class CarPassengerDefinition extends EntityRollingStockDefinition {

	public CarPassengerDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
	}

	@Override
	public EntityRollingStock instance(World world) {
		return new CarPassenger(world, defID);
	}

	@Override
	public boolean acceptsPassengers() {
		return true;
	}
}
