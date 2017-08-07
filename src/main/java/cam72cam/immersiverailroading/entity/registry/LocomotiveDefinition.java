package cam72cam.immersiverailroading.entity.registry;

import java.util.List;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.util.Speed;

public abstract class LocomotiveDefinition extends EntityRollingStockDefinition  {
	private String works;
	private int fuelConsumption;
	private int power;
	private int traction;
	private double brake;
	private Speed maxSpeed;
	
	public LocomotiveDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
		works = data.get("works").getAsString();
		
		JsonObject properties = data.get("properties").getAsJsonObject();
		
		fuelConsumption = properties.get("fuel_consumption").getAsInt();
		power = properties.get("horsepower").getAsInt();
		traction = properties.get("tractive_effort").getAsInt();
		brake = properties.get("deceleration").getAsDouble();
		maxSpeed = Speed.fromMetric(properties.get("max_speed").getAsDouble());
	}
	
	@Override
	public List<String> getTooltip() {
		List<String> tips = super.getTooltip();
		tips.add("Works: " + this.works);
		tips.add("Horse Power: " + this.getHorsePower());
		tips.add("Max Speed: " + this.getMaxSpeed().metricString());
		return tips;
	}

	public int getFuelConsumption() {
		return this.fuelConsumption;
	}
	
	public int getHorsePower() {
		return this.power;
	}
	
	/**
	 * @return tractive effort in pounds
	 */
	public int getStartingTraction() {
		return this.traction;
	}

	public double getBrakePower() {
		return this.brake;
	}

	public Speed getMaxSpeed() {
		return this.maxSpeed;
	}
}
