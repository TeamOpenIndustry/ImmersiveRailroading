package cam72cam.immersiverailroading.model;

import java.util.List;

import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.mod.math.Vec3d;

public class MultiRenderComponent {
	private final Vec3d min;
	private final Vec3d max;

	public MultiRenderComponent(List<RenderComponent> subComponents) {
		double minX = subComponents.get(0).min.x;
		double minY = subComponents.get(0).min.y;
		double minZ = subComponents.get(0).min.z;
		double maxX = subComponents.get(0).max.x;
		double maxY = subComponents.get(0).max.y;
		double maxZ = subComponents.get(0).max.z;
		
		for (RenderComponent rc : subComponents) {
			minX = Math.min(minX, rc.min.x);
			minY = Math.min(minY, rc.min.y);
			minZ = Math.min(minZ, rc.min.z);
			maxX = Math.max(maxX, rc.max.x);
			maxY = Math.max(maxY, rc.max.y);
			maxZ = Math.max(maxZ, rc.max.z);
		}
		min = new Vec3d(minX, minY, minZ);
		max = new Vec3d(maxX, maxY, maxZ);
	}

	private MultiRenderComponent(Vec3d min, Vec3d max) {
		this.min = min;
		this.max = max;
	}

	public Vec3d center() {
		return new Vec3d((min.x + max.x)/2, (min.y + max.y)/2, (min.z + max.z)/2);
	}

	public double height() {
		return max.y - min.y;
	}
}
