package cam72cam.immersiverailroading.entity.registry;

import java.util.List;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.Speed;
import cam72cam.immersiverailroading.entity.SteamLocomotive;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RegisteredSteamLocomotive extends DefinitionRollingStock {
	private int waterConsumption;
	private int fuelConsumption;
	private int tankCapacity;
	private int fuelCapacity;
	private int power;
	private int traction;
	private double accel;
	private double brake;
	private Speed maxSpeed;

	public RegisteredSteamLocomotive(String defID) throws Exception {
		super(defID);
		JsonObject properties = super.getJsonData().get("properties").getAsJsonObject();
		
		waterConsumption = properties.get("fuel_consumption").getAsInt();
		fuelConsumption = properties.get("fuel_consumption").getAsInt();
		
		tankCapacity = properties.get("water_capacity").getAsInt();
		fuelCapacity = properties.get("fuel_capacity").getAsInt();
		
		power = properties.get("horsepower").getAsInt();
		traction = properties.get("tractive_effort").getAsInt();
		accel = properties.get("deceleration").getAsDouble();
		brake = properties.get("deceleration").getAsDouble();
		maxSpeed = Speed.fromMetric(properties.get("max_speed").getAsDouble());
	}

	@Override
	public EntityRollingStock spawn(World world, BlockPos pos, EnumFacing facing) {
		SteamLocomotive loco = new SteamLocomotive(world, defID);

		loco.setPosition(pos.getX(), pos.getY(), pos.getZ());
		loco.prevRotationYaw = facing.getHorizontalAngle();
		loco.rotationYaw = facing.getHorizontalAngle();
		world.spawnEntity(loco);

		return loco;
	}
	
	@Override
	public List<String> getTooltip() {
		List<String> tips = super.getTooltip();
		tips.add("Horse Power: " + this.getPower());
		tips.add("Max Speed: " + this.getMaxSpeed().metricString());
		return tips;
	}

	public int getWaterConsumption() {
		return this.waterConsumption;
	}

	public int getFuelConsumption() {
		return this.fuelConsumption;
	}

	public int getTankCapacity() {
		return this.tankCapacity;
	}
	
	public int getFuelCapacity() {
		return this.fuelCapacity;
	}
	
	public int getPower() {
		return this.power;
	}
	
	public int getTraction() {
		return this.traction;
	}

	public double getAccel() {
		return this.accel;
	}

	public double getBrake() {
		return this.brake;
	}

	public Speed getMaxSpeed() {
		return this.maxSpeed;
	}
}
