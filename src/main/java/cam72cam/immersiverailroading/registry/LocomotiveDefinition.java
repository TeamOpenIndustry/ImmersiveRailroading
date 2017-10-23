package cam72cam.immersiverailroading.registry;

import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.model.RenderComponent;
import cam72cam.immersiverailroading.util.Speed;

public class LocomotiveDefinition extends EntityRollingStockDefinition  {
	private String works = "Unknown";
	private int power;
	private int traction;
	private Speed maxSpeed = Speed.ZERO;
	
	public LocomotiveDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
	}
	
	@Override
	public void parseJson(JsonObject data) throws Exception {
		super.parseJson(data);
		
		works = data.get("works").getAsString();
		
		JsonObject properties = data.get("properties").getAsJsonObject();
		
		power = properties.get("horsepower").getAsInt();
		traction = properties.get("tractive_effort_lbf").getAsInt();
		maxSpeed = Speed.fromMetric(properties.get("max_speed_kmh").getAsDouble());
	}
	
	@Override
	protected Set<String> parseComponents() {
		Set<String> groups = super.parseComponents();

		addComponentIfExists(RenderComponent.parse(RenderComponentType.CAB, this, groups), true);
		addComponentIfExists(RenderComponent.parse(RenderComponentType.BELL, this, groups), true);
		addComponentIfExists(RenderComponent.parse(RenderComponentType.WHISTLE, this, groups), true);
		addComponentIfExists(RenderComponent.parse(RenderComponentType.HORN, this, groups), true);
		
		return groups;
	}
	
	@Override
	public List<String> getTooltip() {
		List<String> tips = super.getTooltip();
		tips.add("Works: " + this.works);
		tips.add("Horse Power: " + this.getHorsePower());
		tips.add("Max Speed: " + this.getMaxSpeed().metricString());
		return tips;
	}
	
	public int getHorsePower() {
		return this.power;
	}
	
	/**
	 * @return tractive effort in newtons
	 */
	public int getStartingTractionNewtons() {
		return (int) (this.traction * 4.44822);
	}

	public Speed getMaxSpeed() {
		return this.maxSpeed;
	}
}
