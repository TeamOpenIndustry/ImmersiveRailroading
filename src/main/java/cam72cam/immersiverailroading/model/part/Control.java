package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.util.Axis;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.opengl.GL11;

import java.util.Optional;

public class Control {
    public final ModelComponent part;
    private Vec3d rotationPoint = null;
    private int rotationDegrees = 0;
    private Axis rotationAxis = null;

    public static Control get(OBJModel model, ComponentProvider provider, ModelComponentType type) {
        ModelComponent part = provider.parse(type);
        Optional<OBJGroup> point = model.groups.values().stream().filter(g -> g.name.contains(type.regex.replaceAll("\\.\\*", "") + "_ROT")).findFirst();

        return part != null ? new Control(part, point) : null;
    }

    public Control(ModelComponent part, Optional<OBJGroup> point) {
        this.part = part;
        if (point.isPresent()) {
            OBJGroup group = point.get();
            this.rotationPoint = group.max.add(group.min).scale(0.5);
            String[] split = group.name.split("_");
            int idx = ArrayUtils.indexOf(split, "ROT");
            if (idx != ArrayUtils.INDEX_NOT_FOUND) {
                String degrees = split[idx + 1];
                try {
                    rotationDegrees = Integer.parseInt(degrees);
                } catch (NumberFormatException e) {
                    ModCore.error("Unable to parse rotation point '%s': %s", group.name, e);
                }
            }
            Vec3d delta = group.min.subtract(group.max);
            if (Math.abs(delta.x) > Math.abs(delta.y) && Math.abs(delta.x) > Math.abs(delta.z)) {
                rotationAxis = Axis.X;
            } else {
                rotationAxis = Math.abs(delta.y) > Math.abs(delta.z) ? Axis.Y : Axis.Z;
            }
        }
    }

    public void render(float valuePercent, ComponentRenderer draw) {
        if (rotationPoint == null) {
            draw.render(part);
            return;
        }
        try (ComponentRenderer matrix = draw.push()) {
            GL11.glTranslated(rotationPoint.x, rotationPoint.y, rotationPoint.z);
            GL11.glRotated(
                    valuePercent * rotationDegrees,
                    rotationAxis == Axis.X ? 1 : 0,
                    rotationAxis == Axis.Y ? 1 : 0,
                    rotationAxis == Axis.Z ? 1 : 0
            );
            GL11.glTranslated(-rotationPoint.x, -rotationPoint.y, -rotationPoint.z);
            matrix.render(part);
        }
    }
}
