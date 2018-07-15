package cam72cam.immersiverailroading.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.library.ValveGearType;
import cam72cam.immersiverailroading.model.RenderComponent;
import cam72cam.immersiverailroading.util.FluidQuantity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class LocomotiveSteamDefinition extends LocomotiveDefinition {
	private FluidQuantity tankCapacity;
	private int maxPSI;
	private ValveGearType valveGear;
	private int numSlots;
	private int width;
	
	public Quilling quill;
	public ResourceLocation whistle;
	public ResourceLocation idle;
	public ResourceLocation chuff;
	public ResourceLocation pressure;
	
	public LocomotiveSteamDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
		
		// Handle null data
		if (tankCapacity == null) {
			tankCapacity = FluidQuantity.ZERO;
		}
	}
	
	@Override
	public void parseJson(JsonObject data) throws Exception {
		super.parseJson(data);
		JsonObject properties = data.get("properties").getAsJsonObject();
		tankCapacity = FluidQuantity.FromLiters((int) Math.ceil(properties.get("water_capacity_l").getAsInt() * internal_inv_scale));
		maxPSI = (int) Math.ceil(properties.get("max_psi").getAsInt() * internal_inv_scale);
		valveGear = ValveGearType.valueOf(properties.get("valve_gear").getAsString().toUpperCase());
		JsonObject firebox = data.get("firebox").getAsJsonObject();
		this.numSlots = (int) Math.ceil(firebox.get("slots").getAsInt() * internal_inv_scale);
		this.width = (int) Math.ceil(firebox.get("width").getAsInt() * internal_inv_scale);
		
		JsonObject sounds = data.has("sounds") ? data.get("sounds").getAsJsonObject() : null;
		
		whistle = new ResourceLocation(ImmersiveRailroading.MODID, "sounds/steam/default/whistle.ogg");
		idle = new ResourceLocation(ImmersiveRailroading.MODID, "sounds/steam/default/idle.ogg");
		chuff = new ResourceLocation(ImmersiveRailroading.MODID, "sounds/steam/default/chuff.ogg");
		pressure = new ResourceLocation(ImmersiveRailroading.MODID, "sounds/steam/default/pressure.ogg");

		boolean whistleSet = false;
		
		
		if (sounds != null) {
			if (sounds.has("whistle")) {
				whistle = new ResourceLocation(ImmersiveRailroading.MODID, sounds.get("whistle").getAsString());
				whistleSet = true;
			}
			
			if (sounds.has("idle")) {
				idle = new ResourceLocation(ImmersiveRailroading.MODID, sounds.get("idle").getAsString());
			}
			
			if (sounds.has("chuff")) {
				chuff = new ResourceLocation(ImmersiveRailroading.MODID, sounds.get("chuff").getAsString());
			}
			
			if (sounds.has("pressure")) {
				pressure = new ResourceLocation(ImmersiveRailroading.MODID, sounds.get("pressure").getAsString());
			}
			
			if (sounds.has("quilling")) {
				quill = new Quilling(sounds.get("quilling").getAsJsonArray());
				whistleSet = true;
			}
		}
		if (!whistleSet) {
			quill = new Quilling(new ResourceLocation(ImmersiveRailroading.MODID, "sounds/steam/default/quill.ogg"));
		}
	}

	@Override
	public EntityRollingStock instance(World world) {
		return new LocomotiveSteam(world, defID);
	}
	
	@Override
	protected boolean unifiedBogies() {
		return false;
	}

	@Override
	protected Set<String> parseComponents() {
		Set<String> groups = super.parseComponents();
		
		switch (this.valveGear) {
		case STEPHENSON:
		case WALSCHAERTS:
		case HIDDEN:
			for (int i = 0; i < 10; i++) {
				addComponentIfExists(RenderComponent.parseID(RenderComponentType.WHEEL_DRIVER_X, this, groups, i), true);
			}
			break;
		case T1:
		case MALLET_WALSCHAERTS:
			for (int i = 0; i < 10; i++) {
				addComponentIfExists(RenderComponent.parseID(RenderComponentType.WHEEL_DRIVER_FRONT_X, this, groups, i), true);
				addComponentIfExists(RenderComponent.parseID(RenderComponentType.WHEEL_DRIVER_REAR_X, this, groups, i), true);
			};
			addComponentIfExists(RenderComponent.parse(RenderComponentType.FRONT_LOCOMOTIVE, this, groups), true);
			break;
		case CLIMAX:
			break;
		case SHAY:
			break;
		}
		

		for (int i = 0; i < 20; i++) {
			addComponentIfExists(RenderComponent.parseID(RenderComponentType.BOILER_SEGMENT_X, this, groups, i), true);
		}
		
		for (int i = 0; i < 20; i++) {
			addComponentIfExists(RenderComponent.parseID(RenderComponentType.PARTICLE_CHIMNEY_X, this, groups, i), false);
			addComponentIfExists(RenderComponent.parseID(RenderComponentType.PRESSURE_VALVE_X, this, groups, i), false);
		}
		
		addComponentIfExists(RenderComponent.parse(RenderComponentType.FIREBOX, this, groups), true);
		addComponentIfExists(RenderComponent.parse(RenderComponentType.SMOKEBOX, this, groups), true);
		addComponentIfExists(RenderComponent.parse(RenderComponentType.STEAM_CHEST_FRONT, this, groups), true);
		addComponentIfExists(RenderComponent.parse(RenderComponentType.STEAM_CHEST_REAR, this, groups), true);
		addComponentIfExists(RenderComponent.parse(RenderComponentType.STEAM_CHEST, this, groups), true);
		addComponentIfExists(RenderComponent.parse(RenderComponentType.PIPING, this, groups), true);
		
		
		List<String> sides = new ArrayList<String>();
		
		switch (this.valveGear) {
		case STEPHENSON:
		case WALSCHAERTS:
			sides.add("RIGHT");
			sides.add("LEFT");
		case T1:
		case MALLET_WALSCHAERTS:
			if (sides.size() == 0) {
				sides.add("LEFT_FRONT");
				sides.add("RIGHT_FRONT");
				sides.add("LEFT_REAR");
				sides.add("RIGHT_REAR");
			}
			
			RenderComponentType[] components = new RenderComponentType[] {
				RenderComponentType.SIDE_ROD_SIDE,
				RenderComponentType.MAIN_ROD_SIDE,
				RenderComponentType.PISTON_ROD_SIDE,
				RenderComponentType.CYLINDER_SIDE,
				
				RenderComponentType.UNION_LINK_SIDE,
				RenderComponentType.COMBINATION_LEVER_SIDE,
				RenderComponentType.VALVE_STEM_SIDE,
				RenderComponentType.RADIUS_BAR_SIDE,
				RenderComponentType.EXPANSION_LINK_SIDE,
				RenderComponentType.ECCENTRIC_ROD_SIDE,
				RenderComponentType.ECCENTRIC_CRANK_SIDE,
				RenderComponentType.REVERSING_ARM_SIDE,
				RenderComponentType.LIFTING_LINK_SIDE,
				RenderComponentType.REACH_ROD_SIDE,
			};
			
			for (String side : sides) {
				for (RenderComponentType name : components) {
					addComponentIfExists(RenderComponent.parseSide(name, this, groups, side), true);
				}
			}
		case CLIMAX:
			break;
		case SHAY:
			break;
		case HIDDEN:
			break;
		}
		
		return groups;
	}

	public FluidQuantity getTankCapacity(Gauge gauge) {
		return this.tankCapacity.scale(gauge.scale()).min(FluidQuantity.FromBuckets(1)).roundBuckets();
	}
	
	public int getMaxPSI(Gauge gauge) {
		return (int) Math.ceil(this.maxPSI * gauge.scale());
	}
	public ValveGearType getValveGear() {
		return valveGear;
	}
	
	public int getInventorySize(Gauge gauge) {
		return MathHelper.ceil(numSlots * gauge.scale());
	}

	public int getInventoryWidth(Gauge gauge) {
		return Math.max(3, MathHelper.ceil(width * gauge.scale()));
	}
}
