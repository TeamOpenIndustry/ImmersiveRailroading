package cam72cam.immersiverailroading.model.part;

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

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Control {
    public final ModelComponent part;
    private Vec3d rotationPoint = null;
    private int rotationDegrees = 0;
    private Axis rotationAxis = null;

    public static List<Control> get(OBJModel model, ComponentProvider provider, ModelComponentType type) {
        return provider.parseAll(type).stream().map(part -> {
            OBJGroup point = model.groups.values().stream().filter(g -> Pattern.matches(type.regex.replaceAll("#ID#",  part.id + "_ROT"), g.name)).findFirst().orElse(null);
            return new Control(part, point);
        }).collect(Collectors.toList());
    }

    public Control(ModelComponent part, OBJGroup point) {
        this.part = part;
        if (point != null) {
            this.rotationPoint = point.max.add(point.min).scale(0.5);
            String[] split = point.name.split("_");
            int idx = ArrayUtils.indexOf(split, "ROT");
            if (idx != ArrayUtils.INDEX_NOT_FOUND) {
                String degrees = split[idx + 1];
                try {
                    rotationDegrees = Integer.parseInt(degrees);
                } catch (NumberFormatException e) {
                    ModCore.error("Unable to parse rotation point '%s': %s", point.name, e);
                }
            }
            Vec3d delta = point.min.subtract(point.max);
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
