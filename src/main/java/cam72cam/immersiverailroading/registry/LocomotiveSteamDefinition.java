package cam72cam.immersiverailroading.registry;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.util.FluidQuantity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LocomotiveSteamDefinition extends LocomotiveDefinition {
	private int waterConsumption;
	private FluidQuantity tankCapacity;
	private int fuelCapacity;
	private ValveGearType valveGear;
	private int numSlots;
	private int width;
	
	public enum ValveGearType {
		WALSCHAERTS,
		MALLET_WALSCHAERTS,
		SHAY,
		CLIMAX,
	}

	public LocomotiveSteamDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
		JsonObject properties = data.get("properties").getAsJsonObject();
		waterConsumption = properties.get("fuel_consumption").getAsInt();
		tankCapacity = FluidQuantity.FromLiters(properties.get("water_capacity_l").getAsInt());
		fuelCapacity = properties.get("fuel_capacity").getAsInt();
		valveGear = ValveGearType.valueOf(properties.get("valve_gear").getAsString().toUpperCase());
		JsonObject tender = data.get("firebox").getAsJsonObject();
		this.numSlots = tender.get("slots").getAsInt();
		this.width = tender.get("width").getAsInt();
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

	public FluidQuantity getTankCapacity() {
		return this.tankCapacity;
	}
	
	public int getFuelCapacity() {
		return this.fuelCapacity;
	}
	public ValveGearType getValveGear() {
		return valveGear;
	}
	
	public int getInventorySize() {
		return numSlots;
	}

	public int getInventoryWidth() {
		return width;
	}
}
