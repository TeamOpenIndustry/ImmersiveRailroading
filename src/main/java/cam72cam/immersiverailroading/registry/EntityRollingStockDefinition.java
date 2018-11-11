package cam72cam.immersiverailroading.registry;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityBuildableRollingStock;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.model.RenderComponent;
import cam72cam.immersiverailroading.model.obj.Material;
import cam72cam.immersiverailroading.model.obj.OBJModel;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.util.RealBB;
import cam72cam.immersiverailroading.util.TextUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class EntityRollingStockDefinition {
	
	public abstract EntityRollingStock instance(World world);
	
	public final EntityRollingStock spawn(World world, Vec3d pos, EnumFacing facing, Gauge gauge, String texture) {
		EntityRollingStock stock = instance(world);
		stock.setPosition(pos.x, pos.y, pos.z);
		stock.prevRotationYaw = facing.getHorizontalAngle();
		stock.rotationYaw = facing.getHorizontalAngle();
		stock.gauge = gauge;
		stock.texture = texture;
		world.spawnEntity(stock);

		return stock;
	}

	public final String defID;
	private String name = "Unknown";
	private OBJModel model;
	public Map<String, String> textureNames = null;
	private Vec3d passengerCenter = new Vec3d(0, 0, 0);
	private float bogeyFront;
	private float bogeyRear;
	private float couplerOffsetFront;
	private float couplerOffsetRear;
	
	public float dampeningAmount;
	private boolean scalePitch;
	public  double frontBounds;
	public  double rearBounds;
	private double heightBounds;
	private double widthBounds;
	private double passengerCompartmentLength;
	private double passengerCompartmentWidth;
	private int weight;
	private int maxPassengers;
	protected double internal_model_scale;
	protected double internal_inv_scale;
	public Gauge recommended_gauge;
	public Boolean shouldSit;
	public ResourceLocation wheel_sound;
	
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
	}
	
	public boolean shouldScalePitch() {
		return scalePitch;
	}

	public void parseJson(JsonObject data) throws Exception  {
		name = data.get("name").getAsString();
		float darken = 0;
		if (data.has("darken_model")) {
			darken = data.get("darken_model").getAsFloat();
		}
		this.internal_model_scale = 1;
		this.internal_inv_scale = 1;
		// TODO Gauge.from(Gauge.STANDARD).value() what happens when != Gauge.STANDARD
		this.recommended_gauge = Gauge.from(Gauge.STANDARD);
		if (data.has("model_gauge_m")) { 
			this.recommended_gauge = Gauge.from(data.get("model_gauge_m").getAsDouble());
			this.internal_model_scale = Gauge.STANDARD / data.get("model_gauge_m").getAsDouble();
		}
		if (data.has("recommended_gauge_m")) {
			this.recommended_gauge = Gauge.from(data.get("recommended_gauge_m").getAsDouble());
		}
		if (this.recommended_gauge != Gauge.from(Gauge.STANDARD)) {
			this.internal_inv_scale = Gauge.STANDARD / recommended_gauge.value();
		}
		
		model = new OBJModel(new ResourceLocation(data.get("model").getAsString()), darken, internal_model_scale);
		textureNames = new LinkedHashMap<String, String>();
		textureNames.put(null, "Default");
		if (data.has("tex_variants")) {
			JsonElement variants = data.get("tex_variants");
			for (Entry<String, JsonElement> variant : variants.getAsJsonObject().entrySet()) {
				textureNames.put(variant.getValue().getAsString(), variant.getKey());
			}
		}
		
		ResourceLocation alt_textures = new ResourceLocation(ImmersiveRailroading.MODID, defID.replace(".json", "_variants.json"));
		try {
			List<InputStream> alts = ImmersiveRailroading.proxy.getResourceStreamAll(alt_textures);
			for (InputStream input : alts) {
				JsonParser parser = new JsonParser();
				JsonElement variants = parser.parse(new InputStreamReader(input)).getAsJsonArray();
				for (Entry<String, JsonElement> variant : variants.getAsJsonObject().entrySet()) {
					textureNames.put(variant.getValue().getAsString(), variant.getKey());
				}
			}
		} catch (java.io.FileNotFoundException ex) {
			//ignore
		}
		
		JsonObject passenger = data.get("passenger").getAsJsonObject();
		passengerCenter = new Vec3d(passenger.get("center_x").getAsDouble(), passenger.get("center_y").getAsDouble()-0.35, 0).scale(internal_model_scale);
		passengerCompartmentLength = passenger.get("length").getAsDouble() * internal_model_scale;
		passengerCompartmentWidth = passenger.get("width").getAsDouble() * internal_model_scale;
		maxPassengers = passenger.get("slots").getAsInt();
		if (passenger.has("should_sit")) {
			shouldSit = passenger.get("should_sit").getAsBoolean();
		}

		bogeyFront = (float) (data.get("trucks").getAsJsonObject().get("front").getAsFloat() * internal_model_scale);
		bogeyRear = (float) (data.get("trucks").getAsJsonObject().get("rear").getAsFloat() * internal_model_scale);
		
		dampeningAmount = 0.75f;
		if (data.has("sound_dampening_percentage")) {
			if (data.get("sound_dampening_percentage").getAsFloat() >= 0.0f && data.get("sound_dampening_percentage").getAsFloat() <= 1.0f) {
				dampeningAmount = data.get("sound_dampening_percentage").getAsFloat();
			}
		}
		
		scalePitch = true;
		if (data.has("scale_pitch")) {
			scalePitch = data.get("scale_pitch").getAsBoolean();
		}
		
		if (data.has("couplers")) {
			couplerOffsetFront = (float) (data.get("couplers").getAsJsonObject().get("front_offset").getAsFloat() * internal_model_scale);
			couplerOffsetRear = (float) (data.get("couplers").getAsJsonObject().get("rear_offset").getAsFloat() * internal_model_scale);
		}
		
		frontBounds = -model.minOfGroup(model.groups()).x + couplerOffsetFront;
		rearBounds = model.maxOfGroup(model.groups()).x + couplerOffsetRear;
		widthBounds = model.widthOfGroups(model.groups());
		
		// Bad hack for height bounds
		ArrayList<String> heightGroups = new ArrayList<String>();
		for (String group : model.groups()) {
			boolean ignore = false;
			for (RenderComponentType rct : RenderComponentType.values()) {
				if (rct.collisionsEnabled) {
					continue;
				}
				for (int i = 0; i < 10; i++) {
					if (Pattern.matches(rct.regex.replace("#ID#", "" + i), group)) {
						ignore = true;
						break;
					}
				}
				if (ignore) {
					break;
				}
			}
			if (!ignore) {
				heightGroups.add(group);
			}
		}
		heightBounds = model.heightOfGroups(heightGroups);
		
		weight = (int)Math.ceil(data.get("properties").getAsJsonObject().get("weight_kg").getAsInt() * internal_inv_scale);
		
		JsonObject sounds = data.has("sounds") ? data.get("sounds").getAsJsonObject() : null;
		if (sounds != null && sounds.has("wheels")) {
			wheel_sound = new ResourceLocation(ImmersiveRailroading.MODID, sounds.get("wheels").getAsString());
		} else {
			wheel_sound = new ResourceLocation(ImmersiveRailroading.MODID, "sounds/default/track_wheels.ogg");
		}
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
		for (int i = 100; i >= 1; i--) {
			addComponentIfExists(RenderComponent.parseID(RenderComponentType.CARGO_FILL_X, this, groups, i), false);
		}
		
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
			return gauge.scale() * (this.frontBounds);
		case BACK:
			return gauge.scale() * (this.rearBounds);
		default:
			return 0;
		}
	}
	
	public void initHeightMap() {
		ImmersiveRailroading.info("Generating heightmap %s", defID);
		
		double ratio = 8;
		xRes = (int) Math.ceil((this.frontBounds + this.rearBounds) * ratio);
		zRes = (int) Math.ceil(this.widthBounds * ratio);
		
		int precision = (int) Math.ceil(this.heightBounds * 4); 
		
		for (List<RenderComponent> rcl : this.renderComponents.values()) {
			for (RenderComponent rc : rcl) {
				if (!rc.type.collisionsEnabled) {
					continue;
				}
				double[][] heightMap = new double[xRes][zRes];
				for (String group : rc.modelIDs) {
					int[] faces = model.groups.get(group);
					for (int face : faces) {
						Path2D path = new Path2D.Double();
						double fheight = 0;
						boolean first = true;
						for (int[] point : model.points(face)) {
							Vec3d vert = model.vertices(point[0]);
							vert = vert.addVector(this.frontBounds, 0, this.widthBounds/2);
							if (first) {
								path.moveTo(vert.x * ratio, vert.z * ratio);
								first = false;
							} else {
								path.lineTo(vert.x * ratio, vert.z * ratio);
							}
							fheight += vert.y / 3; // We know we are using tris
						}
						Rectangle2D bounds = path.getBounds2D();
						if (bounds.getWidth() * bounds.getHeight() < 1) {
							continue;
						}
						for (int x = 0; x < xRes; x++) {
							for (int z = 0; z < zRes; z++) {
								double relX = ((xRes-1)-x);
								double relZ = z;
								if (bounds.contains(relX, relZ) && path.contains(relX, relZ)) {
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
	
	public String[][] getIcon(int i) {
		
		ImmersiveRailroading.info("Generating model icon map %s...", this.defID);
		
		String[][] map = new String[i][i];
		
		// Distance per pixel
		double nx = Math.max(this.heightBounds, this.widthBounds) / map.length;
		double xoff = 0;
		if (this.heightBounds > this.widthBounds) {
			xoff = (this.heightBounds - this.widthBounds) / 2;
		}
		
		List<Integer> faces = new ArrayList<Integer>();
		for (List<RenderComponent> rcl : this.renderComponents.values()) {
			for (RenderComponent rc : rcl) {
				if (!rc.type.collisionsEnabled) {
					continue;
				}
				for (String group : rc.modelIDs) {
					for (int face : model.groups.get(group)) {
						faces.add(face);
					}
				}
			}
		}
		float[] depthCache = new float[model.faceVerts.length/9];
		for (int f : faces) {
			float sum = 0;
			for (int[] point : model.points(f)) {
				Vec3d pt = model.vertices(point[0]);
				sum += pt.x;
			}
			depthCache[f] = sum / 3; //We know it's a tri
		}
		
		faces.sort(new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return Float.compare(depthCache[o1], depthCache[o2]);
			}
		});
		
		for (int f : faces) {
			Material mtl = model.materials.get(model.faceMTLs[f]);
			Path2D path = new Path2D.Double();
			boolean first = true;
			for (int[] point : model.points(f)) {
				Vec3d vert = model.vertices(point[0]);
				vert = vert.addVector(0, 0, this.widthBounds/2);
				if (first) {
					path.moveTo(vert.z / nx, vert.y / nx);
				} else {
					path.lineTo(vert.z / nx, vert.y / nx);
				}
				first = false;
			}
			Rectangle2D bounds = path.getBounds2D();
			if (bounds.getWidth() * bounds.getHeight() < 1) {
				continue;
			}
			for (int z = 0; z < map.length; z++) {
				for (int y = 0; y < map[z].length; y++) {
					if (map[z][y] != null) {
						continue;
					}
					double relZ = z - xoff / nx;
					double relY = y ;
					if (bounds.contains(relZ, relY) && path.contains(relZ, relY)) {
						map[z][y] = mtl.name;
					}
				}
			}
		}
		return map;
	}
	
	public double[][] createHeightMap(EntityBuildableRollingStock stock) {
		double[][] heightMap = new double[xRes][zRes];
		

		List<RenderComponentType> availComponents = new ArrayList<RenderComponentType>();
		for (ItemComponentType item : stock.getItemComponents()) {
			availComponents.addAll(item.render);
		}
		
		for (List<RenderComponent> rcl : this.renderComponents.values()) {
			for (RenderComponent rc : rcl) {
				if (!rc.type.collisionsEnabled) {
					continue;
				}
				
				if (availComponents.contains(rc.type)) {
					availComponents.remove(rc.type);
				} else if (rc.type == RenderComponentType.REMAINING && stock.isBuilt()) {
					//pass
				} else {
					continue;
				}
				double[][] pm = partMapCache.get(rc);
				for (int x = 0; x < xRes; x++) {
					for (int z = 0; z < zRes; z++) {
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
			for (double x = minX; x <= maxX+1; x++) {
				for (double y = minY; y <= maxY+1; y++) {
					for (double z = minZ; z <= maxZ+1; z++) {
						blocksInBounds.add(new Vec3d(x,y,z));
					}
				}
			}
		}
		return blocksInBounds;
	}
	
	public String name() {
		String[] sp = this.defID.replaceAll(".json", "").split("/");
		String localStr = String.format("%s:entity.%s.%s", ImmersiveRailroading.MODID, sp[sp.length-2], sp[sp.length-1]); 
		String transStr = TextUtil.translate(localStr);
		return localStr != transStr ? transStr : name;
	}

	public List<String> getTooltip(Gauge gauge) {
		List<String> tips = new ArrayList<String>();
		tips.add(GuiText.WEIGHT_TOOLTIP.toString(this.getWeight(gauge)));
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

	public boolean acceptsPassengers() {
		return false;
	}
	
	public boolean acceptsLivestock() {
		return false;
	}

	@SideOnly(Side.SERVER)
	public void clearModel() {
		this.model = null;
	}
}
