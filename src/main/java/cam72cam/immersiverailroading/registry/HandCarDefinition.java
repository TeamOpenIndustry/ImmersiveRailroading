package cam72cam.immersiverailroading.registry;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.HandCar;
import net.minecraft.world.World;

public class HandCarDefinition extends LocomotiveDefinition {
	public HandCarDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
	}

	@Override
	public EntityRollingStock instance(World world) {
		return new HandCar(world, defID);
	}
	
	@Override
	public double getBrakePower() {
		return 0.1;
	}
}
