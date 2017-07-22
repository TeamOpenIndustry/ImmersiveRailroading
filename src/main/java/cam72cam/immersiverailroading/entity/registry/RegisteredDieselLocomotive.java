package cam72cam.immersiverailroading.entity.registry;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.DieselLocomotive;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RegisteredDieselLocomotive extends RegisteredLocomotive {
	private int fuelCapacity;

	public RegisteredDieselLocomotive(String defID, JsonObject data) throws Exception {
		super(defID, data);
		JsonObject properties = data.get("properties").getAsJsonObject();
		fuelCapacity = properties.get("fuel_capacity").getAsInt();
	}

	@Override
	public EntityRollingStock spawn(World world, BlockPos pos, EnumFacing facing) {
		DieselLocomotive loco = new DieselLocomotive(world, defID);

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
