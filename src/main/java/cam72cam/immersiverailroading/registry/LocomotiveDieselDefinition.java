package cam72cam.immersiverailroading.registry;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LocomotiveDieselDefinition extends LocomotiveDefinition {
	private FluidQuantity fuelCapacity;
	private int fuelEfficiency;

	public LocomotiveDieselDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
		JsonObject properties = data.get("properties").getAsJsonObject();
		fuelCapacity = FluidQuantity.FromLiters(properties.get("fuel_capacity_l").getAsInt());
		fuelEfficiency = properties.get("fuel_efficiency_%").getAsInt();
	}

	@Override
	public EntityRollingStock spawn(World world, Vec3d pos, EnumFacing facing) {
		LocomotiveDiesel loco = new LocomotiveDiesel(world, defID);

		loco.setPosition(pos.x, pos.y, pos.z);
		loco.prevRotationYaw = facing.getHorizontalAngle();
		loco.rotationYaw = facing.getHorizontalAngle();
		world.spawnEntity(loco);

		return loco;
	}
	
	public FluidQuantity getFuelCapacity() {
		return this.fuelCapacity;
	}

	public int getFuelEfficiency() {
		return this.fuelEfficiency;
	}
}
