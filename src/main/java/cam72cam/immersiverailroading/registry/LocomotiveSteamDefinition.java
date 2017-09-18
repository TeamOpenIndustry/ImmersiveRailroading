package cam72cam.immersiverailroading.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.library.ComponentName;
import cam72cam.immersiverailroading.library.ValveGearType;
import cam72cam.immersiverailroading.model.Component;
import cam72cam.immersiverailroading.util.FluidQuantity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LocomotiveSteamDefinition extends LocomotiveDefinition {
	private FluidQuantity tankCapacity;
	private int maxPSI;
	private ValveGearType valveGear;
	private int numSlots;
	private int width;
	
	private Map<String, Map<ComponentName, Component>> valveGearComponents;
	
	public LocomotiveSteamDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
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
	public EntityRollingStock spawn(World world, Vec3d pos, EnumFacing facing) {
		LocomotiveSteam loco = new LocomotiveSteam(world, defID);

		loco.setPosition(pos.x, pos.y, pos.z);
		loco.prevRotationYaw = facing.getHorizontalAngle();
		loco.rotationYaw = facing.getHorizontalAngle();
		world.spawnEntity(loco);

		return loco;
	}
	
	@Override
	protected Set<String> parseComponents() {
		Set<String> groups = super.parseComponents();
		
		valveGearComponents = new HashMap<String,Map<ComponentName, Component>>();
		
		switch (this.valveGear) {
		case WALSCHAERTS:
			for (int i = 0; i < 10; i++) {
				addComponentIfExists(Component.parseWheel(ComponentName.WHEEL_DRIVER_X, this, groups, i));
			}
			break;
		case MALLET_WALSCHAERTS:
			for (int i = 0; i < 10; i++) {
				addComponentIfExists(Component.parseWheel(ComponentName.WHEEL_DRIVER_FRONT_X, this, groups, i));
				addComponentIfExists(Component.parseWheel(ComponentName.WHEEL_DRIVER_REAR_X, this, groups, i));
			};
			addComponentIfExists(Component.parse(ComponentName.FRONT_LOCOMOTIVE, this, groups));
			break;
		case CLIMAX:
			break;
		case SHAY:
			break;
		}
		
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
			
			ComponentName[] components = new ComponentName[] {
					ComponentName.SIDE_ROD_SIDE,
					ComponentName.MAIN_ROD_SIDE,
					ComponentName.PISTON_ROD_SIDE,
					ComponentName.UNION_LINK_SIDE,
					ComponentName.COMBINATION_LEVER_SIDE,
					ComponentName.VALVE_STEM_SIDE,
					ComponentName.RADIUS_BAR_SIDE,
					ComponentName.EXPANSION_LINK_SIDE,
					ComponentName.ECCENTRIC_ROD_SIDE,
					ComponentName.ECCENTRIC_CRANK_SIDE,
					ComponentName.REVERSING_ARM_SIDE,
					ComponentName.LIFTING_LINK_SIDE,
					ComponentName.REACH_ROD_SIDE,
			};
			
			for (String side : sides) {
				for (ComponentName name : components) {
					Component found = Component.parseSide(name, this, groups, side);
					if (found == null) {
						continue;
					}
					
					addComponentIfExists(found);
					
					if (!valveGearComponents.containsKey(side)) {
						valveGearComponents.put(side, new HashMap<ComponentName, Component>());
					}
					valveGearComponents.get(side).put(name, found);
				}
			}
		case CLIMAX:
			break;
		case SHAY:
			break;
		}
		
		return groups;
	}
	
	public Component getComponent(ComponentName name, String side) {
		return valveGearComponents.containsKey(side) ? valveGearComponents.get(side).get(name) : null;
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
