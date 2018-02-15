package cam72cam.immersiverailroading.registry;

import java.util.Set;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.model.RenderComponent;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class LocomotiveDieselDefinition extends LocomotiveDefinition {
	private FluidQuantity fuelCapacity;
	private int fuelEfficiency;
	public ResourceLocation idle;
	public ResourceLocation horn;

	public LocomotiveDieselDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
		
		// Handle null data
		if (fuelCapacity == null) {
			fuelCapacity = FluidQuantity.ZERO;
		}
	}
	
	@Override
	protected Set<String> parseComponents() {
		Set<String> groups = super.parseComponents();

		addComponentIfExists(RenderComponent.parse(RenderComponentType.FUEL_TANK, this, groups), true);
		addComponentIfExists(RenderComponent.parse(RenderComponentType.ALTERNATOR, this, groups), true);
		addComponentIfExists(RenderComponent.parse(RenderComponentType.ENGINE_BLOCK, this, groups), true);
		addComponentIfExists(RenderComponent.parse(RenderComponentType.CRANKSHAFT, this, groups), true);
		for (int i = 0; i < 20; i++) {
			addComponentIfExists(RenderComponent.parseID(RenderComponentType.PISTON_X, this, groups, i), true);
			addComponentIfExists(RenderComponent.parseID(RenderComponentType.DIESEL_EXHAUST_X, this, groups, i), false);
		}
		
		return groups;
	}
	
	@Override
	public void parseJson(JsonObject data) throws Exception {
		super.parseJson(data);
		
		JsonObject properties = data.get("properties").getAsJsonObject();
		fuelCapacity = FluidQuantity.FromLiters((int)Math.ceil(properties.get("fuel_capacity_l").getAsInt() * internal_scale));
		fuelEfficiency = properties.get("fuel_efficiency_%").getAsInt();
		
		JsonObject sounds = data.has("sounds") ? data.get("sounds").getAsJsonObject() : null;
		
		if (sounds != null && sounds.has("idle")) {
			idle = new ResourceLocation(ImmersiveRailroading.MODID, sounds.get("idle").getAsString());
		} else {
			idle = new ResourceLocation(ImmersiveRailroading.MODID, "sounds/diesel/default/idle.ogg");
		}
		
		if (sounds != null && sounds.has("horn")) {
			horn = new ResourceLocation(ImmersiveRailroading.MODID, sounds.get("horn").getAsString());
		} else {
			horn = new ResourceLocation(ImmersiveRailroading.MODID, "sounds/diesel/default/horn.ogg");
		}
	}

	@Override
	public EntityRollingStock instance(World world) {
		return new LocomotiveDiesel(world, defID);
	}
	
	public FluidQuantity getFuelCapacity(Gauge gauge) {
		return this.fuelCapacity.scale(gauge.scale()).min(FluidQuantity.FromBuckets(1)).roundBuckets();
	}

	public int getFuelEfficiency() {
		return this.fuelEfficiency;
	}
}
