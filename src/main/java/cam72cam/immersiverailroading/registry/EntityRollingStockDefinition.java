package cam72cam.immersiverailroading.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.ComponentName;
import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.model.Component;
import cam72cam.immersiverailroading.model.obj.OBJModel;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.util.RealBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class EntityRollingStockDefinition {
	public abstract EntityRollingStock spawn(World world, Vec3d pos, EnumFacing facing);

	protected String defID;
	public String name;
	private OBJModel model;
	private Vec3d passengerCenter;
	private float bogeyFront;
	private float bogeyRear;

	private double frontBounds;
	private double rearBounds;
	private double heightBounds;
	private double widthBounds;
	private double passengerCompartmentLength;
	private double passengerCompartmentWidth;
	private int weight;
	private int maxPassengers;
	
	private Map<ComponentName, List<Component>> components;

	public EntityRollingStockDefinition(String defID, JsonObject data) throws Exception {
		this.defID = defID;
		
		parseJson(data);
		
		addComponentIfExists(Component.parse(ComponentName.REMAINING, this, parseComponents()));
	}

	public void parseJson(JsonObject data) throws Exception  {
		name = data.get("name").getAsString();
		float darken = 0;
		if (data.has("darken_model")) {
			darken = data.get("darken_model").getAsFloat();
		}
		model = new OBJModel(new ResourceLocation(data.get("model").getAsString()), darken);
		JsonObject passenger = data.get("passenger").getAsJsonObject();
		passengerCenter = new Vec3d(passenger.get("center_x").getAsDouble(), passenger.get("center_y").getAsDouble(), 0);
		passengerCompartmentLength = passenger.get("length").getAsDouble();
		passengerCompartmentWidth = passenger.get("width").getAsDouble();
		maxPassengers = passenger.get("slots").getAsInt();

		bogeyFront = data.get("trucks").getAsJsonObject().get("front").getAsFloat();
		bogeyRear = data.get("trucks").getAsJsonObject().get("rear").getAsFloat();
		
		frontBounds = -model.minOfGroup(model.groups()).x;
		rearBounds = model.maxOfGroup(model.groups()).x;
		widthBounds = model.widthOfGroups(model.groups());
		heightBounds = model.heightOfGroups(model.groups());
		
		weight = data.get("properties").getAsJsonObject().get("weight_kg").getAsInt();
	}
	
	protected void addComponentIfExists(Component component) {
		if (component != null) {
			if (!components.containsKey(component.name)) {
				components.put(component.name, new ArrayList<Component>());
			}
			components.get(component.name).add(component);
		}
	}
	
	protected Set<String> parseComponents() {
		components = new HashMap<ComponentName, List<Component>>();
		
		Set<String> groups = new HashSet<String>();
		groups.addAll(model.groups());
		
		for (int i = 0; i < 10; i++) {
			addComponentIfExists(Component.parseWheel(ComponentName.BOGEY_FRONT_WHEEL_X, this, groups, i));
			addComponentIfExists(Component.parseWheel(ComponentName.BOGEY_REAR_WHEEL_X, this, groups, i));
		}
		
		addComponentIfExists(Component.parse(ComponentName.BOGEY_FRONT, this, groups));
		addComponentIfExists(Component.parse(ComponentName.BOGEY_REAR, this, groups));
		
		return groups;
	}
	
	public Component getComponent(ComponentName name) {
		if (!components.containsKey(name)) {
			return null;
		}
		return components.get(name).get(0);
	}
	
	public List<Component> getComponents(ComponentName name) {
		return components.get(name);
	}

	public Vec3d getPassengerCenter() {
		return this.passengerCenter;
	}
	public Vec3d correctPassengerBounds(Vec3d pos) {
		if (pos.x > this.passengerCompartmentLength) {
			pos = new Vec3d(this.passengerCompartmentLength, pos.y, pos.z);
		}
		
		if (pos.x < -this.passengerCompartmentLength) {
			pos = new Vec3d(-this.passengerCompartmentLength, pos.y, pos.z);
		}
		
		if (Math.abs(pos.z) > this.passengerCompartmentWidth/2) {
			pos = new Vec3d(pos.x, pos.y, Math.copySign(this.passengerCompartmentWidth/2, pos.z));
		}
		
		return pos;
	}

	public float getBogeyFront() {
		return this.bogeyFront;
	}

	public float getBogeyRear() {
		return this.bogeyRear;
	}
	
	public double getCouplerPosition(CouplerType coupler) {
		switch(coupler) {
		case FRONT:
			return this.frontBounds + Config.couplerRange;
		case BACK:
			return this.rearBounds + Config.couplerRange;
		default:
			return 0;
		}
	}

	public AxisAlignedBB getBounds(EntityMoveableRollingStock stock) {
		return new RealBB(frontBounds, -rearBounds, widthBounds, heightBounds, stock.rotationYaw).offset(stock.getPositionVector());
	}

	public List<String> getTooltip() {
		List<String> tips = new ArrayList<String>();
		return tips;
	}

	public double getPassengerCompartmentWidth() {
		return this.passengerCompartmentWidth;
	}

	public OBJModel getModel() {
		return model;
	}

	/**
	 * @return Stock Weight in Kg
	 */
	public int getWeight() {
		return this.weight;
	}

	public double getHeight() {
		return this.heightBounds;
	}

	public int getMaxPassengers() {
		return this.maxPassengers;
	}
}
