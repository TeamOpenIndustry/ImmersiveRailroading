package cam72cam.immersiverailroading.model;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import net.minecraft.util.math.Vec3d;

public class RenderComponent {
	public final RenderComponentType type;
	public final EntityRollingStockDefinition def;
	public final int wheel;
	public final String side;
	public final Set<String> modelIDs;
	
	public static RenderComponent parse(RenderComponentType name, EntityRollingStockDefinition def, Set<String> groups) {
		return parse(name, def, groups, -1, "");
	}
	
	public static RenderComponent parseWheel(RenderComponentType name, EntityRollingStockDefinition def, Set<String> groups, int i) {
		return parse(name, def, groups, i, "");
	}
	
	public static RenderComponent parseSide(RenderComponentType name, EntityRollingStockDefinition def, Set<String> groups, String side) {
		return parse(name, def, groups, -1, side);
	}
	
	private static RenderComponent parse(RenderComponentType name, EntityRollingStockDefinition def, Set<String> groups, int wheel, String side) {
		RenderComponent comp = new RenderComponent(name, def, wheel, side);
		
		String wheelStr = wheel != -1 ? "" + wheel : "";
		
		for (String group : groups) {
			if (Pattern.matches(name.regex.replace("#SIDE#", side).replaceAll("#WHEEL#", wheelStr), group)) {
				comp.modelIDs.add(group);
			}
		}
		if (comp.modelIDs.size() == 0) {
			return null;
		}
		groups.removeAll(comp.modelIDs);
		
		return comp;
	} 

	private RenderComponent(RenderComponentType name, EntityRollingStockDefinition def, int wheel, String side) {
		this.modelIDs = new HashSet<String>();
		this.type = name;
		this.def = def;
		this.wheel = wheel;
		this.side = side;
	}
	
	protected RenderComponent(EntityRollingStockDefinition def) {
		this(null, def, 0, null);
	}

	public Vec3d min() {
		return def.getModel().minOfGroup(modelIDs);
	}
	public Vec3d max() {
		return def.getModel().maxOfGroup(modelIDs);
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
}
