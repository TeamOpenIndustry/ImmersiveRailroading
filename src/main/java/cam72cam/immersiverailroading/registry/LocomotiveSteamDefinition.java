package cam72cam.immersiverailroading.registry;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LocomotiveSteamDefinition extends LocomotiveDefinition {
	private int waterConsumption;
	private int tankCapacity;
	private int fuelCapacity;

	public LocomotiveSteamDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
		JsonObject properties = data.get("properties").getAsJsonObject();
		waterConsumption = properties.get("fuel_consumption").getAsInt();
		tankCapacity = properties.get("water_capacity").getAsInt();
		fuelCapacity = properties.get("fuel_capacity").getAsInt();
	}

	@Override
	public EntityRollingStock spawn(World world, BlockPos pos, EnumFacing facing) {
		LocomotiveSteam loco = new LocomotiveSteam(world, defID);

		loco.setPosition(pos.getX(), pos.getY(), pos.getZ());
		loco.prevRotationYaw = facing.getHorizontalAngle();
		loco.rotationYaw = facing.getHorizontalAngle();
		world.spawnEntity(loco);

		return loco;
	}

	public int getWaterConsumption() {
		return this.waterConsumption;
	}

	public int getTankCapacity() {
		return this.tankCapacity;
	}
	
	public int getFuelCapacity() {
		return this.fuelCapacity;
	}
}
