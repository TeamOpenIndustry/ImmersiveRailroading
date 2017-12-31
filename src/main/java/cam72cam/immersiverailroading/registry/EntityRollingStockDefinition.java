package cam72cam.immersiverailroading.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.Gauge;
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

public class EntityRollingStockDefinition {
	
	public EntityRollingStock instance(World world) {
		return null;
	}
	
	public final EntityRollingStock spawn(World world, Vec3d pos, EnumFacing facing, Gauge gauge) {
		EntityRollingStock stock = instance(world);
		stock.setPosition(pos.x, pos.y, pos.z);
		stock.prevRotationYaw = facing.getHorizontalAngle();
		stock.rotationYaw = facing.getHorizontalAngle();
		stock.gauge = gauge;
		world.spawnEntity(stock);

		return stock;
	}

	protected String defID;
	public String name = "Unknown";
	private OBJModel model;
	private Vec3d passengerCenter = new Vec3d(0, 0, 0);
	private float bogeyFront;
	private float bogeyRear;

	public  double frontBounds;
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
		if (data == null) {
			// USED ONLY RIGHT BEFORE REMOVING UNKNOWN STOCK

			renderComponents = new HashMap<RenderComponentType, List<RenderComponent>>();
			itemComponents = new ArrayList<ItemComponentType>();
			
			return;
		}
		
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
	
	public RenderComponent getComponent(RenderComponentType name, Gauge gauge) {
		if (!renderComponents.containsKey(name)) {
			return null;
		}
		return renderComponents.get(name).get(0).scale(gauge);
	}
	
	public RenderComponent getComponent(RenderComponentType name, String pos, Gauge gauge) {
		if (!renderComponents.containsKey(name)) {
			return null;
		}
		for (RenderComponent c : renderComponents.get(name)) {
			if (c.pos.equals(pos)) {
				return c.scale(gauge);
			}
			if (c.side.equals(pos)) {
				return c.scale(gauge);
			}
		}
		return null;
	}

	public RenderComponent getComponent(RenderComponent comp, Gauge gauge) {
		if (!renderComponents.containsKey(comp.type)) {
			return null;
		}
		for (RenderComponent c : getComponents(comp.type, gauge)) {
			if (c.equals(comp)) {
				return c;
			}
		}
		return null;
	}
	
	public List<RenderComponent> getComponents(RenderComponentType name, Gauge gauge) {
		if (!renderComponents.containsKey(name)) {
			return null;
		}
		List<RenderComponent> components = new ArrayList<RenderComponent>();
		for (RenderComponent c : renderComponents.get(name)) {
			components.add(c.scale(gauge));
		}
		return components;
	}
	
	public List<RenderComponent> getComponents(RenderComponentType name, String pos, Gauge gauge) {
		if (!renderComponents.containsKey(name)) {
			return null;
		}
		List<RenderComponent> components = new ArrayList<RenderComponent>();
		for (RenderComponent c : renderComponents.get(name)) {
			if (c.pos.equals(pos)) {
				components.add(c.scale(gauge));
			}
		}
		return components;
	}

	public Vec3d getPassengerCenter(Gauge gauge) {
		return this.passengerCenter.scale(gauge.scale());
	}
	public Vec3d correctPassengerBounds(Gauge gauge, Vec3d pos) {
		double gs = gauge.scale();
		if (pos.x > this.passengerCompartmentLength * gs) {
			pos = new Vec3d(this.passengerCompartmentLength * gs, pos.y, pos.z);
		}
		
		if (pos.x < -this.passengerCompartmentLength * gs) {
			pos = new Vec3d(-this.passengerCompartmentLength * gs, pos.y, pos.z);
		}
		
		if (Math.abs(pos.z) > this.passengerCompartmentWidth/2 * gs) {
			pos = new Vec3d(pos.x, pos.y, Math.copySign(this.passengerCompartmentWidth/2 * gs, pos.z));
		}
		
		return pos;
	}

	public boolean isAtFront(Gauge gauge, Vec3d pos) {
		return pos.x >= this.passengerCompartmentLength * gauge.scale();
	}
	public boolean isAtRear(Gauge gauge, Vec3d pos) {
		return pos.x <= -this.passengerCompartmentLength * gauge.scale();
	}

	public List<ItemComponentType> getItemComponents() {
		return itemComponents;
	}

	public float getBogeyFront(Gauge gauge) {
		return (float)gauge.scale() * this.bogeyFront;
	}

	public float getBogeyRear(Gauge gauge) {
		return (float)gauge.scale() * this.bogeyRear;
	}
	
	public double getCouplerPosition(CouplerType coupler, Gauge gauge) {
		switch(coupler) {
		case FRONT:
			return gauge.scale() * (this.frontBounds + Config.couplerRange);
		case BACK:
			return gauge.scale() * (this.rearBounds + Config.couplerRange);
		default:
			return 0;
		}
	}

	public AxisAlignedBB getBounds(EntityMoveableRollingStock stock, Gauge gauge) {
		return new RealBB(gauge.scale() * frontBounds, gauge.scale() * -rearBounds, gauge.scale() * widthBounds,
				gauge.scale() * heightBounds, stock.rotationYaw).offset(stock.getPositionVector());
	}
	
	List<Vec3d> blocksInBounds = null;
	public List<Vec3d> getBlocksInBounds(Gauge gauge) {
		if (blocksInBounds == null) {
			blocksInBounds = new ArrayList<Vec3d>();
			double minX = gauge.scale() * -rearBounds;
			double maxX = gauge.scale() * frontBounds;
			double minY = gauge.scale() * 0;
			double maxY = gauge.scale() * heightBounds;
			double minZ = gauge.scale() * -widthBounds / 2;
			double maxZ = gauge.scale() * widthBounds / 2;
			for (double x = minX; x <= maxX; x++) {
				for (double y = minY; y <= maxY; y++) {
					for (double z = minZ; z <= maxZ; z++) {
						blocksInBounds.add(new Vec3d(x,y,z));
					}
				}
			}
		}
		return blocksInBounds;
	}

	public List<String> getTooltip(Gauge gauge) {
		List<String> tips = new ArrayList<String>();
		return tips;
	}

	public double getPassengerCompartmentWidth(Gauge gauge) {
		return gauge.scale() * this.passengerCompartmentWidth;
	}

	public OBJModel getModel() {
		return model;
	}

	/**
	 * @return Stock Weight in Kg
	 */
	public int getWeight(Gauge gauge) {
		return (int) (gauge.scale() * this.weight);
	}

	public double getHeight(Gauge gauge) {
		return gauge.scale() * this.heightBounds;
	}
	
	public double getLength(Gauge gauge) {
		return gauge.scale() * this.frontBounds + this.rearBounds;
	}

	public int getMaxPassengers() {
		return this.maxPassengers;
	}
}
