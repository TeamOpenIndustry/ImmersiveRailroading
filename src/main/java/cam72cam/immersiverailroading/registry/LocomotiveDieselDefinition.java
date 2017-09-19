package cam72cam.immersiverailroading.registry;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import net.minecraft.world.World;

public class LocomotiveDieselDefinition extends LocomotiveDefinition {
	private FluidQuantity fuelCapacity;
	private int fuelEfficiency;

	public LocomotiveDieselDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
	}
	
	@Override
	public void parseJson(JsonObject data) throws Exception {
		super.parseJson(data);
		
		JsonObject properties = data.get("properties").getAsJsonObject();
		fuelCapacity = FluidQuantity.FromLiters(properties.get("fuel_capacity_l").getAsInt());
		fuelEfficiency = properties.get("fuel_efficiency_%").getAsInt();
	}

	@Override
	public EntityRollingStock instance(World world) {
		return new LocomotiveDiesel(world, defID);
	}
	
	public FluidQuantity getFuelCapacity() {
		return this.fuelCapacity;
	}

	public int getFuelEfficiency() {
		return this.fuelEfficiency;
	}
}
