package cam72cam.immersiverailroading.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.library.ValveGearType;
import cam72cam.immersiverailroading.model.RenderComponent;
import cam72cam.immersiverailroading.util.FluidQuantity;
import net.minecraft.world.World;

public class LocomotiveSteamDefinition extends LocomotiveDefinition {
	private FluidQuantity tankCapacity;
	private int maxPSI;
	private ValveGearType valveGear;
	private int numSlots;
	private int width;
	
	private Map<String, Map<RenderComponentType, RenderComponent>> valveGearComponents;
	
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
		tankCapacity = FluidQuantity.FromLiters(properties.get("water_capacity_l").getAsInt());
		maxPSI = properties.get("max_psi").getAsInt();
		valveGear = ValveGearType.valueOf(properties.get("valve_gear").getAsString().toUpperCase());
		JsonObject firebox = data.get("firebox").getAsJsonObject();
		this.numSlots = firebox.get("slots").getAsInt();
		this.width = firebox.get("width").getAsInt();
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
		
		valveGearComponents = new HashMap<String,Map<RenderComponentType, RenderComponent>>();
		
		switch (this.valveGear) {
		case WALSCHAERTS:
			for (int i = 0; i < 10; i++) {
				addComponentIfExists(RenderComponent.parseID(RenderComponentType.WHEEL_DRIVER_X, this, groups, i), true);
			}
			break;
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
		
		addComponentIfExists(RenderComponent.parse(RenderComponentType.FIREBOX, this, groups), true);
		addComponentIfExists(RenderComponent.parse(RenderComponentType.STEAM_CHEST_FRONT, this, groups), true);
		addComponentIfExists(RenderComponent.parse(RenderComponentType.STEAM_CHEST_REAR, this, groups), true);
		addComponentIfExists(RenderComponent.parse(RenderComponentType.STEAM_CHEST, this, groups), true);
		addComponentIfExists(RenderComponent.parse(RenderComponentType.PIPING, this, groups), true);
		
		
		List<String> sides = new ArrayList<String>();
		
		switch (this.valveGear) {
		case WALSCHAERTS:
			sides.add("RIGHT");
			sides.add("LEFT");
		case MALLET_WALSCHAERTS:
			if (sides.size() == 0) {
				sides.add("LEFT_FRONT");
				sides.add("RIGHT_FRONT");
				sides.add("LEFT_REAR");
				sides.add("RIGHT_REAR");
			}
			
			//valveGearComponents
			
			RenderComponentType[] components = new RenderComponentType[] {
				RenderComponentType.SIDE_ROD_SIDE,
				RenderComponentType.MAIN_ROD_SIDE,
				RenderComponentType.PISTON_ROD_SIDE,
				RenderComponentType.CYLINDER_SIDE,
			};
			
			for (String side : sides) {
				for (RenderComponentType name : components) {
					RenderComponent found = RenderComponent.parseSide(name, this, groups, side);
					if (found == null) {
						continue;
					}
					
					addComponentIfExists(found, true);
					
					if (!valveGearComponents.containsKey(side)) {
						valveGearComponents.put(side, new HashMap<RenderComponentType, RenderComponent>());
					}
					valveGearComponents.get(side).put(name, found);
				}
			}
			
			components = new RenderComponentType[] {
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
				boolean hasRender = false;
				for (RenderComponentType name : components) {
					RenderComponent found = RenderComponent.parseSide(name, this, groups, side);
					if (found == null) {
						continue;
					}
					
					hasRender = true;
					
					addComponentIfExists(found, false);
					
					if (!valveGearComponents.containsKey(side)) {
						valveGearComponents.put(side, new HashMap<RenderComponentType, RenderComponent>());
					}
					valveGearComponents.get(side).put(name, found);
				}
				if (hasRender) {
					itemComponents.add(ItemComponentType.WALCHERTS_LINKAGE);
				}
			}
		case CLIMAX:
			break;
		case SHAY:
			break;
		}
		
		return groups;
	}
	
	public RenderComponent getComponent(RenderComponentType name, String side, Gauge gauge) {
		RenderComponent comp = valveGearComponents.containsKey(side) ? valveGearComponents.get(side).get(name) : null;
		return comp != null ? comp.scale(gauge) : null;
	}

	public FluidQuantity getTankCapacity() {
		return this.tankCapacity;
	}
	
	public int getMaxPSI() {
		return this.maxPSI;
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
