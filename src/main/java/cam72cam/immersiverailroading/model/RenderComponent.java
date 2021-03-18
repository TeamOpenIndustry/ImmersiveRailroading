package cam72cam.immersiverailroading.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.math.Vec3d;

public class RenderComponent {
	public final RenderComponentType type;
	public final int id;
	public final String side;
	public final Set<String> modelIDs;
	public final String pos;
	public final Vec3d min;
	public final Vec3d max;

	private final boolean wooden;
	private final Vec3d center;
	private final Map<Double, RenderComponent> scaleCache = new HashMap<>();

	public static RenderComponent parse(RenderComponentType name, EntityRollingStockDefinition def, Set<String> groups) {
		return parse(name, def, groups, -1, "", "");
	}
	
	public static RenderComponent parseID(RenderComponentType name, EntityRollingStockDefinition def, Set<String> groups, int id) {
		return parse(name, def, groups, id, "", "");
	}
	
	public static RenderComponent parseSide(RenderComponentType name, EntityRollingStockDefinition def, Set<String> groups, String side) {
		return parse(name, def, groups, -1, side, "");
	}

	public static RenderComponent parsePos(RenderComponentType name, EntityRollingStockDefinition def, Set<String> groups, String pos) {
		return parse(name, def, groups, -1, "", pos);
	}

	public static RenderComponent parsePosID(RenderComponentType name, EntityRollingStockDefinition def, Set<String> groups, String pos, int id) {
		return parse(name, def, groups, id, "", pos);
	}
	
	private static RenderComponent parse(RenderComponentType name, EntityRollingStockDefinition def, Set<String> groups, int id, String side, String pos) {
		Set<String> modelIDs = new HashSet<String>();
		
		String idStr = id != -1 ? "" + id : "";
		
		boolean wooden = true;
		
		for (String group : groups) {
			if (Pattern.matches(name.regex.replace("#SIDE#", side).replaceAll("#ID#", idStr).replaceAll("#POS#", pos), group)) {
				modelIDs.add(group);
				if (!group.contains("WOOD")) {
					wooden = false;
				}
			}
		}
		if (modelIDs.size() == 0) {
			return null;
		}
		

		Vec3d min = def.getModel().minOfGroup(modelIDs);
		Vec3d max = def.getModel().maxOfGroup(modelIDs);
		
		groups.removeAll(modelIDs);
		
		return new RenderComponent(modelIDs, name, id, side, pos, wooden, min, max);
	}

	private RenderComponent(Set<String> modelIDs, RenderComponentType type, int id, String side, String pos, boolean wooden, Vec3d min, Vec3d max) {
		this.modelIDs = modelIDs;
		this.type = type;
		this.id = id;
		this.side = side;
		this.pos = pos;
		this.wooden = wooden;
		this.min = min;
		this.max = max;
		this.center = new Vec3d((min.x + max.x)/2, (min.y + max.y)/2, (min.z + max.z)/2);
	}

	public Vec3d min() {
		return min;
	}
	public Vec3d max() {
		return max;
	}
	public Vec3d center() {
		return center;
	}
	public double height() {
		return max.y - min.y;
	}
	public double length() {
		return max.x - min.x;
	}

	public double width() {
		return max.z - min.z;
	}
	
	public boolean isWooden() {
		return wooden;
	}

	@Override
	public String toString() {
		return String.format("%s%s%s%s", this.type, this.id, this.side, this.pos);
	}
}
