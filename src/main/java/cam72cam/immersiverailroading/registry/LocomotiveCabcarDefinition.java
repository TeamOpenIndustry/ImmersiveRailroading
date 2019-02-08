package cam72cam.immersiverailroading.registry;

import java.util.Set;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.LocomotiveCabcar;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.util.FluidQuantity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class LocomotiveCabcarDefinition extends LocomotiveDieselDefinition {
	private FluidQuantity fuelCapacity;
	private int fuelEfficiency;
	public ResourceLocation idle;
	public ResourceLocation horn;
	public boolean muliUnitCapable;

	public LocomotiveCabcarDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
		
		// Handle null data
		if (fuelCapacity == null) {
			fuelCapacity = FluidQuantity.ZERO;
		}
	}

	@Override
	protected Set<String> parseComponents() {
		Set<String> groups = super.parseComponents();
		return groups;
	}
	
	@Override
	public void parseJson(JsonObject data) throws Exception {
		super.parseJson(data);
		
		fuelCapacity = FluidQuantity.FromLiters(0);
		fuelEfficiency = 100;
		muliUnitCapable = true;
		
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
		return new LocomotiveCabcar(world, defID);
	}
	
	public FluidQuantity getFuelCapacity(Gauge gauge) {
		return this.fuelCapacity.scale(gauge.scale()).min(FluidQuantity.FromBuckets(1)).roundBuckets();
	}

	public int getFuelEfficiency() {
		return this.fuelEfficiency;
	}
}
