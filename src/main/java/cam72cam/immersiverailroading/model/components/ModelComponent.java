package cam72cam.immersiverailroading.model.components;

import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJModel;

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
}
