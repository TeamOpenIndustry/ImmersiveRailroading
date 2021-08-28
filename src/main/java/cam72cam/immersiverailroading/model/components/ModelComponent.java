package cam72cam.immersiverailroading.model.components;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.part.Wheel;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJModel;
import util.Matrix4;

import java.util.List;
import java.util.Set;

public class ModelComponent {
    public final ModelComponentType type;
    public final String pos;
    public final Integer id;
    public final String key;

    public final Set<String> modelIDs;
    public final Vec3d min;
    public final Vec3d max;
    public final Vec3d center;
    public final boolean wooden;

    public ModelComponent(ModelComponentType type, String pos, Integer id, OBJModel model, Set<String> modelIDs) {
        this.type = type;
        this.pos = pos;
        this.id = id;
        this.modelIDs = modelIDs;
        this.key = String.join(" ", modelIDs);
        min = model.minOfGroup(this.modelIDs);
        max = model.maxOfGroup(this.modelIDs);
        center = new Vec3d((min.x + max.x)/2, (min.y + max.y)/2, (min.z + max.z)/2);
        wooden = modelIDs.stream().anyMatch(g -> g.contains("WOOD"));
    }

    public double length() {
        return max.x - min.x;
    }
    public double height() {
        return max.y - min.y;
    }
    public double width() {
        return max.z - min.z;
    }

    public Vec3d min(EntityRollingStock stock) {
        return worldPosition(min, stock);
    }

    public Vec3d center(EntityRollingStock stock) {
        return worldPosition(center, stock);
    }

    public Vec3d max(EntityRollingStock stock) {
        return worldPosition(max, stock);
    }

    public static Vec3d center(List<ModelComponent> components) {
        double minX = components.get(0).min.x;
        double minY = components.get(0).min.y;
        double minZ = components.get(0).min.z;
        double maxX = components.get(0).max.x;
        double maxY = components.get(0).max.y;
        double maxZ = components.get(0).max.z;

        for (ModelComponent rc : components) {
            minX = Math.min(minX, rc.min.x);
            minY = Math.min(minY, rc.min.y);
            minZ = Math.min(minZ, rc.min.z);
            maxX = Math.max(maxX, rc.max.x);
            maxY = Math.max(maxY, rc.max.y);
            maxZ = Math.max(maxZ, rc.max.z);
        }
        return new Vec3d((minX + maxX)/2, (minY + maxY)/2, (minZ + maxZ)/2);
    }

    public static Vec3d worldPosition(Vec3d pos, EntityRollingStock stock) {
        return worldMatrix(stock).apply(pos);
    }

    public static Matrix4 worldMatrix(EntityRollingStock stock) {
        // TODO put this matrix in stock and update every tick
        return new Matrix4()
                .scale(stock.gauge.scale(), stock.gauge.scale(), stock.gauge.scale())
                .translate(stock.getPosition().x, stock.getPosition().y, stock.getPosition().z)
                .rotate(Math.toRadians(180 - stock.getRotationYaw()), 0, 1, 0)
                .rotate(Math.toRadians(stock.getRotationPitch()), 1, 0, 0)
                .rotate(Math.toRadians(-90), 0, 1, 0);
    }
}
