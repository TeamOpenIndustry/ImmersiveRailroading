package cam72cam.immersiverailroading.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.model.RenderComponent;
import cam72cam.immersiverailroading.model.obj.OBJModel;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.util.RealBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class EntityRollingStockDefinition {
	public abstract EntityRollingStock instance(World world);
	
	public final EntityRollingStock spawn(World world, Vec3d pos, EnumFacing facing) {
		EntityRollingStock stock = instance(world);
		stock.setPosition(pos.x, pos.y, pos.z);
		stock.prevRotationYaw = facing.getHorizontalAngle();
		stock.rotationYaw = facing.getHorizontalAngle();
		world.spawnEntity(stock);

		return stock;
	}

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
	
	private Map<RenderComponentType, List<RenderComponent>> renderComponents;
	ArrayList<ItemComponentType> itemComponents;

	public EntityRollingStockDefinition(String defID, JsonObject data) throws Exception {
		this.defID = defID;
		
		parseJson(data);
		
		addComponentIfExists(RenderComponent.parse(RenderComponentType.REMAINING, this, parseComponents()), true);
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
	
	protected void addComponentIfExists(RenderComponent renderComponent, boolean itemComponent) {
		if (renderComponent != null) {
			if (!renderComponents.containsKey(renderComponent.type)) {
				renderComponents.put(renderComponent.type, new ArrayList<RenderComponent>());
			}
			renderComponents.get(renderComponent.type).add(renderComponent);
			
			if (itemComponent && renderComponent.type != RenderComponentType.REMAINING) {
				itemComponents.add(ItemComponentType.from(renderComponent.type));
			}
		}
	}
	
	protected boolean unifiedBogies() {
		return true;
	}
	
	protected Set<String> parseComponents() {
		renderComponents = new HashMap<RenderComponentType, List<RenderComponent>>();
		itemComponents = new ArrayList<ItemComponentType>();
		
		Set<String> groups = new HashSet<String>();
		groups.addAll(model.groups());
		
		for (int i = 0; i < 10; i++) {
			if (unifiedBogies()) {
				addComponentIfExists(RenderComponent.parsePosID(RenderComponentType.BOGEY_POS_WHEEL_X, this, groups, "FRONT", i), true);
				addComponentIfExists(RenderComponent.parsePosID(RenderComponentType.BOGEY_POS_WHEEL_X, this, groups, "REAR", i), true);
			} else {
				addComponentIfExists(RenderComponent.parseID(RenderComponentType.BOGEY_FRONT_WHEEL_X, this, groups, i), true);
				addComponentIfExists(RenderComponent.parseID(RenderComponentType.BOGEY_REAR_WHEEL_X, this, groups, i), true);
			}
			addComponentIfExists(RenderComponent.parseID(RenderComponentType.FRAME_WHEEL_X, this, groups, i), true);
		}
		if (unifiedBogies()) {
			addComponentIfExists(RenderComponent.parsePos(RenderComponentType.BOGEY_POS, this, groups, "FRONT"), true);
			addComponentIfExists(RenderComponent.parsePos(RenderComponentType.BOGEY_POS, this, groups, "REAR"), true);
		} else {
			addComponentIfExists(RenderComponent.parse(RenderComponentType.BOGEY_FRONT, this, groups), true);
			addComponentIfExists(RenderComponent.parse(RenderComponentType.BOGEY_REAR, this, groups), true);
		}
		
		addComponentIfExists(RenderComponent.parse(RenderComponentType.FRAME, this, groups), true);
		addComponentIfExists(RenderComponent.parse(RenderComponentType.SHELL, this, groups), true);
		
		return groups;
	}
	
	public RenderComponent getComponent(RenderComponentType name) {
		if (!renderComponents.containsKey(name)) {
			return null;
		}
		return renderComponents.get(name).get(0);
	}
	
	public RenderComponent getComponent(RenderComponentType name, String pos) {
		if (!renderComponents.containsKey(name)) {
			return null;
		}
		for (RenderComponent c : renderComponents.get(name)) {
			if (c.pos.equals(pos)) {
				return c;
			}
		}
		return null;
	}

	public RenderComponent getComponent(RenderComponent comp) {
		if (!renderComponents.containsKey(comp.type)) {
			return null;
		}
		for (RenderComponent c : getComponents(comp.type)) {
			if (c.equals(comp)) {
				return c;
			}
		}
		return null;
	}
	
	public List<RenderComponent> getComponents(RenderComponentType name) {
		return renderComponents.get(name);
	}
	
	public List<RenderComponent> getComponents(RenderComponentType name, String pos) {
		if (!renderComponents.containsKey(name)) {
			return null;
		}
		List<RenderComponent> components = new ArrayList<RenderComponent>();
		for (RenderComponent c : renderComponents.get(name)) {
			if (c.pos.equals(pos)) {
				components.add(c);
			}
		}
		return components;
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

	public boolean isAtFront(Vec3d pos) {
		return pos.x >= this.passengerCompartmentLength;
	}
	public boolean isAtRear(Vec3d pos) {
		return pos.x <= -this.passengerCompartmentLength;
	}

	public List<ItemComponentType> getItemComponents() {
		return itemComponents;
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
	
	public double getLength() {
		return this.frontBounds + this.rearBounds;
	}

	public int getMaxPassengers() {
		return this.maxPassengers;
	}
}
