package cam72cam.immersiverailroading.registry;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
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
import cam72cam.immersiverailroading.entity.EntityBuildableRollingStock;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.model.RenderComponent;
import cam72cam.immersiverailroading.model.obj.Face;
import cam72cam.immersiverailroading.model.obj.OBJModel;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.util.RealBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
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
	private float couplerOffsetFront;
	private float couplerOffsetRear;

	public  double frontBounds;
	public  double rearBounds;
	private double heightBounds;
	private double widthBounds;
	private double passengerCompartmentLength;
	private double passengerCompartmentWidth;
	private int weight;
	private int maxPassengers;
	protected double internal_scale;
	
	private Map<RenderComponentType, List<RenderComponent>> renderComponents;
	ArrayList<ItemComponentType> itemComponents;
	
	private Map<RenderComponent, double[][]> partMapCache = new HashMap<RenderComponent, double[][]>();
	private int xRes;
	private int zRes;

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
		
		initHeightMap();
	}

	public void parseJson(JsonObject data) throws Exception  {
		name = data.get("name").getAsString();
		float darken = 0;
		if (data.has("darken_model")) {
			darken = data.get("darken_model").getAsFloat();
		}
		this.internal_scale = 1;
		if (data.has("model_gauge_m")) { 
			internal_scale = Gauge.STANDARD.value() / data.get("model_gauge_m").getAsDouble();
		}
		model = new OBJModel(new ResourceLocation(data.get("model").getAsString()), darken, internal_scale);
		JsonObject passenger = data.get("passenger").getAsJsonObject();
		passengerCenter = new Vec3d(passenger.get("center_x").getAsDouble(), passenger.get("center_y").getAsDouble(), 0).scale(internal_scale);
		passengerCompartmentLength = passenger.get("length").getAsDouble() * internal_scale;
		passengerCompartmentWidth = passenger.get("width").getAsDouble() * internal_scale;
		maxPassengers = passenger.get("slots").getAsInt();

		bogeyFront = (float) (data.get("trucks").getAsJsonObject().get("front").getAsFloat() * internal_scale);
		bogeyRear = (float) (data.get("trucks").getAsJsonObject().get("rear").getAsFloat() * internal_scale);
		
		if (data.has("couplers")) {
			couplerOffsetFront = (float) (data.get("couplers").getAsJsonObject().get("front_offset").getAsFloat() * internal_scale);
			couplerOffsetRear = (float) (data.get("couplers").getAsJsonObject().get("rear_offset").getAsFloat() * internal_scale);
		}
		
		frontBounds = -model.minOfGroup(model.groups()).x;
		rearBounds = model.maxOfGroup(model.groups()).x;
		widthBounds = model.widthOfGroups(model.groups());
		heightBounds = model.heightOfGroups(model.groups());
		
		weight = (int)Math.ceil(data.get("properties").getAsJsonObject().get("weight_kg").getAsInt() * internal_scale);
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
			return gauge.scale() * (this.frontBounds + couplerOffsetFront);
		case BACK:
			return gauge.scale() * (this.rearBounds + couplerOffsetRear);
		default:
			return 0;
		}
	}
	
	private void initHeightMap() {
		double ratio = 8;
		xRes = (int) Math.ceil((this.frontBounds + this.rearBounds) * ratio);
		zRes = (int) Math.ceil(this.widthBounds * ratio);
		
		int precision = (int) Math.ceil(this.heightBounds * 4); 
		
		for (List<RenderComponent> rcl : this.renderComponents.values()) {
			for (RenderComponent rc : rcl) {
				double[][] heightMap = new double[xRes][zRes];
				for (String group : rc.modelIDs) {
					List<Face> faces = model.groups.get(group);
					for (Face face : faces) {
						Path2D path = new Path2D.Double();
						double fheight = 0;
						boolean first = true;
						for (int[] point : face.points) {
							Vec3d vert = model.vertices.get(point[0]);
							vert = vert.addVector(this.frontBounds, 0, this.widthBounds/2);
							if (first) {
								path.moveTo(vert.x, vert.z);
							} else {
								path.lineTo(vert.x, vert.z);
							}
							fheight += vert.y / face.points.length;
							first = false;
						}
						Area a = new Area(path);
						Rectangle2D bounds = a.getBounds2D();
						for (int x = 0; x < xRes; x++) {
							for (int z = 1; z < zRes; z++) {
								double relX = ((xRes-1)-x) / ratio;
								double relZ = z / ratio;
								if (bounds.contains(relX, relZ) && a.contains(relX, relZ)) {
									double relHeight = fheight / heightBounds;
									relHeight = ((int)Math.ceil(relHeight * precision))/(double)precision;
									heightMap[x][z] = Math.max(heightMap[x][z], relHeight);
								}
							}
						}
					}
				}
				
				partMapCache.put(rc, heightMap);
			}
		}
	}
	
	public double[][] createHeightMap(EntityBuildableRollingStock stock) {
		double[][] heightMap = new double[xRes][zRes];
		

		List<RenderComponentType> availComponents = new ArrayList<RenderComponentType>();
		for (ItemComponentType item : stock.getItemComponents()) {
			availComponents.addAll(item.render);
		}
		
		for (List<RenderComponent> rcl : this.renderComponents.values()) {
			for (RenderComponent rc : rcl) {
				if (availComponents.contains(rc.type)) {
					availComponents.remove(rc.type);
				} else if (rc.type == RenderComponentType.REMAINING && stock.isBuilt()) {
					//pass
				} else {
					continue;
				}
				double[][] pm = partMapCache.get(rc);
				for (int x = 0; x < xRes; x++) {
					for (int z = 1; z < zRes; z++) {
						heightMap[x][z] = Math.max(heightMap[x][z], pm[x][z]);
					}
				}
			}
		}
		
		return heightMap;
	}

	public RealBB getBounds(EntityMoveableRollingStock stock, Gauge gauge) {
		return (RealBB) new RealBB(gauge.scale() * frontBounds, gauge.scale() * -rearBounds, gauge.scale() * widthBounds,
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
		return (int)Math.ceil(gauge.scale() * this.weight);
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
