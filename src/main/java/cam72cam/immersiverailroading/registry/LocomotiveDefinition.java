package cam72cam.immersiverailroading.registry;

import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.model.RenderComponent;
import cam72cam.immersiverailroading.util.Speed;

public abstract class LocomotiveDefinition extends FreightDefinition  {
	private String works;
	private int power;
	private int traction;
	private Speed maxSpeed;
	
	public LocomotiveDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
		
		// Handle null data
		if (works == null) {
			works = "Unknown";
		}
		if (maxSpeed == null) {
			maxSpeed = Speed.ZERO;
		}
	}
	
	@Override
	public void parseJson(JsonObject data) throws Exception {
		super.parseJson(data);
		
		works = data.get("works").getAsString();
		
		JsonObject properties = data.get("properties").getAsJsonObject();
		
		power = (int)Math.ceil(properties.get("horsepower").getAsInt() * internal_inv_scale);
		traction = (int)Math.ceil(properties.get("tractive_effort_lbf").getAsInt() * internal_inv_scale);
		maxSpeed = Speed.fromMetric(properties.get("max_speed_kmh").getAsDouble() * internal_inv_scale);
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
	public List<String> getTooltip(Gauge gauge) {
		List<String> tips = super.getTooltip(gauge);
		tips.add(GuiText.LOCO_WORKS.toString(this.works));
		tips.add(GuiText.LOCO_HORSE_POWER.toString(this.getHorsePower(gauge)));
		tips.add(GuiText.LOCO_TRACTION.toString(this.getStartingTractionNewtons(gauge)));
		tips.add(GuiText.LOCO_MAX_SPEED.toString(this.getMaxSpeed(gauge).metricString()));
		return tips;
	}
	
	public int getHorsePower(Gauge gauge) {
		return (int)Math.ceil(gauge.scale() * this.power);
	}
	
	/**
	 * @return tractive effort in newtons
	 */
	public int getStartingTractionNewtons(Gauge gauge) {
		return (int)Math.ceil(gauge.scale() * this.traction * 4.44822);
	}

	public Speed getMaxSpeed(Gauge gauge) {
		return Speed.fromMinecraft(gauge.scale() * this.maxSpeed.minecraft());
	}
	
	public double getBrakePower() {
		return 1;
	}
}
