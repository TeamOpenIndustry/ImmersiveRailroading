package cam72cam.immersiverailroading.model;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import net.minecraft.util.math.Vec3d;

public class RenderComponent {
	public final RenderComponentType type;
	public final EntityRollingStockDefinition def;
	public final int id;
	public final String side;
	public final Set<String> modelIDs;
	public final String pos;
	public final double scale;
	private boolean wooden;
	
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
		RenderComponent comp = new RenderComponent(name, def, id, side, pos);
		
		String idStr = id != -1 ? "" + id : "";
		
		comp.wooden = true;
		
		for (String group : groups) {
			if (Pattern.matches(name.regex.replace("#SIDE#", side).replaceAll("#ID#", idStr).replaceAll("#POS#", pos), group)) {
				comp.modelIDs.add(group);
				if (!group.contains("WOOD")) {
					comp.wooden = false;
				}
			}
		}
		if (comp.modelIDs.size() == 0) {
			return null;
		}
		groups.removeAll(comp.modelIDs);
		
		return comp;
	} 

	private RenderComponent(RenderComponentType name, EntityRollingStockDefinition def, int wheel, String side, String pos) {
		this.modelIDs = new HashSet<String>();
		this.type = name;
		this.def = def;
		this.id = wheel;
		this.side = side;
		this.pos = pos;
		this.scale = 1;
	}
	
	protected RenderComponent(EntityRollingStockDefinition def) {
		this(null, def, 0, null, null);
	}

	private RenderComponent(Set<String> modelIDs, RenderComponentType type, EntityRollingStockDefinition def, int id, String side, String pos, double scale, boolean wooden) {
		this.modelIDs = modelIDs;
		this.type = type;
		this.def = def;
		this.id = id;
		this.side = side;
		this.pos = pos;
		this.scale = scale;
		this.wooden = wooden;
	}

	public Vec3d min() {
		return def.getModel().minOfGroup(modelIDs).scale(scale);
	}
	public Vec3d max() {
		return def.getModel().maxOfGroup(modelIDs).scale(scale);
	}
	public Vec3d center() {
		Vec3d min = min();
		Vec3d max = max();
		return new Vec3d((min.x + max.x)/2, (min.y + max.y)/2, (min.z + max.z)/2);
	}
	public double height() {
		Vec3d min = min();
		Vec3d max = max();
		return max.y - min.y;
	}
	public double length() {
		Vec3d min = min();
		Vec3d max = max();
		return max.x - min.x;
	}

	public double width() {
		Vec3d min = min();
		Vec3d max = max();
		return max.z - min.z;
	}
	
	public boolean isWooden() {
		return wooden;
	}

	public RenderComponent scale(Gauge gauge) {
		return new RenderComponent(modelIDs, type, def, id, side, pos, gauge.scale(), wooden);
	}
	
	@Override
	public String toString() {
		return String.format("%s%s%s%s%s", this.type, this.id, this.side, this.pos, this.scale);
	}
}
