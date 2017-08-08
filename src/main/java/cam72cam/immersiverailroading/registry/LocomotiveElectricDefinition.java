package cam72cam.immersiverailroading.registry;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.LocomotiveElectric;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LocomotiveElectricDefinition extends LocomotiveDefinition {
	private int fuelCapacity;

	public LocomotiveElectricDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
		JsonObject properties = data.get("properties").getAsJsonObject();
		fuelCapacity = properties.get("fuel_capacity").getAsInt();
	}

	@Override
	public EntityRollingStock spawn(World world, BlockPos pos, EnumFacing facing) {
		LocomotiveElectric loco = new LocomotiveElectric(world, defID);

		loco.setPosition(pos.getX(), pos.getY(), pos.getZ());
		loco.prevRotationYaw = facing.getHorizontalAngle();
		loco.rotationYaw = facing.getHorizontalAngle();
		world.spawnEntity(loco);

		return loco;
	}
	
	public int getFuelCapacity() {
		return this.fuelCapacity;
	}
}
