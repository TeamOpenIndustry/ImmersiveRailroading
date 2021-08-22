package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.util.MathUtil;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.util.Axis;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.opengl.GL11;
import util.Matrix4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Control {
    public final ModelComponent part;
    private Vec3d rotationPoint = null;
    private int rotationDegrees = 0;
    private Axis rotationAxis = null;
    private Map<Axis, Float> translations = new HashMap<>();

    public static List<Control> get(OBJModel model, ComponentProvider provider, ModelComponentType type) {
        return provider.parseAll(type).stream().map(part -> {
            OBJGroup rot = model.groups.values().stream().filter(g -> Pattern.matches(type.regex.replaceAll("#ID#",  part.id + "_ROT"), g.name)).findFirst().orElse(null);
            return new Control(part, rot);
        }).collect(Collectors.toList());
    }

    public Control(ModelComponent part, OBJGroup rot) {
        this.part = part;
        if (rot != null) {
            this.rotationPoint = rot.max.add(rot.min).scale(0.5);
            String[] split = rot.name.split("_");
            int idx = ArrayUtils.indexOf(split, "ROT");
            if (idx != ArrayUtils.INDEX_NOT_FOUND) {
                String degrees = split[idx + 1];
                try {
                    rotationDegrees = Integer.parseInt(degrees);
                } catch (NumberFormatException e) {
                    ModCore.error("Unable to parse rotation point '%s': %s", rot.name, e);
                }
            }
            Vec3d delta = rot.min.subtract(rot.max);
            if (Math.abs(delta.x) > Math.abs(delta.y) && Math.abs(delta.x) > Math.abs(delta.z)) {
                rotationAxis = Axis.X;
            } else {
                rotationAxis = Math.abs(delta.y) > Math.abs(delta.z) ? Axis.Y : Axis.Z;
            }
        }

        Pattern pattern = Pattern.compile("TL_([^_]*)_([^_]*)");
        for (String modelID : part.modelIDs) {
            Matcher matcher = pattern.matcher(modelID);
            while (matcher.find()) {
                translations.put(Axis.valueOf(matcher.group(2)), Float.parseFloat(matcher.group(1)));
            }
        }
    }

    public void render(float valuePercent, ComponentRenderer draw) {
        if (rotationPoint == null && translations.isEmpty()) {
            draw.render(part);
            return;
        }
        try (ComponentRenderer matrix = draw.push()) {
            translations.forEach((axis, val) -> {
                GL11.glTranslated(
                        axis == Axis.X ? val * valuePercent : 0,
                        axis == Axis.Y ? val * valuePercent : 0,
                        axis == Axis.Z ? val * valuePercent : 0
                );
            });
            if (rotationPoint != null) {
                GL11.glTranslated(rotationPoint.x, rotationPoint.y, rotationPoint.z);
                GL11.glRotated(
                        valuePercent * rotationDegrees,
                        rotationAxis == Axis.X ? 1 : 0,
                        rotationAxis == Axis.Y ? 1 : 0,
                        rotationAxis == Axis.Z ? 1 : 0
                );
                GL11.glTranslated(-rotationPoint.x, -rotationPoint.y, -rotationPoint.z);
            }
            matrix.render(part);
        }
    }

    private Vec3d transform(Vec3d point, float valuePercent) {
        Matrix4 m = new Matrix4();
        for (Map.Entry<Axis, Float> entry : translations.entrySet()) {
            Axis axis = entry.getKey();
            Float val = entry.getValue();
            m = m.translate(
                    axis == Axis.X ? val * valuePercent : 0,
                    axis == Axis.Y ? val * valuePercent : 0,
                    axis == Axis.Z ? val * valuePercent : 0
            );
        }

        if (rotationPoint != null) {
            m = m.translate(rotationPoint.x, rotationPoint.y, rotationPoint.z);
            m = m.rotate(
                    Math.toRadians(valuePercent * rotationDegrees),
                    rotationAxis == Axis.X ? 1 : 0,
                    rotationAxis == Axis.Y ? 1 : 0,
                    rotationAxis == Axis.Z ? 1 : 0
            );
            m = m.translate(-rotationPoint.x, -rotationPoint.y, -rotationPoint.z);
        }
        return m.apply(point);
    }

    public IBoundingBox getBoundingBox(float controlPosition) {
        return IBoundingBox.from(transform(part.min, controlPosition), transform(part.max, controlPosition));
    }

    public float movementDelta(double x, double y, EntityRollingStock stock) {
        /*
          -X
        -Z * +Z
          +X
         */
        Vec3d movement = new Vec3d(x * 2, y * 2, 0);
        movement = new Matrix4()
                .rotate(
                        Math.toRadians(MinecraftClient.getPlayer().getRotationPitch()),
                        1, 0, 0
                )
                .rotate(
                        Math.toRadians(MinecraftClient.getPlayer().getRotationYawHead() - stock.getRotationYaw() - 90),
                        0, 1, 0
                )
                .apply(movement);

        float delta = 0;
        for (Map.Entry<Axis, Float> entry : translations.entrySet()) {
            Axis axis = entry.getKey();
            Float val = entry.getValue();

            switch (axis) {
                case X:
                    delta += movement.x / val;
                    break;
                case Y:
                    delta += movement.y / val;
                    break;
                case Z:
                    delta += movement.z / val;
                    break;
            }
        }

        if (rotationPoint != null) {
            switch (rotationAxis) {
                case X:
                    movement = new Vec3d(0, movement.y, movement.z);
                    break;
                case Y:
                    movement = new Vec3d(movement.x, 0, movement.z);
                    break;
                case Z:
                    movement = new Vec3d(movement.x, movement.y, 0);
                    break;
            }
            Vec3d grabComponent = transform(part.center, stock.getControlPosition(this)).subtract(rotationPoint).add(movement);
            Vec3d grabComponentNext = transform(part.center, stock.getControlPosition(this) + 0.1f).subtract(rotationPoint);
            Vec3d grabComponentPrev = transform(part.center, stock.getControlPosition(this) - 0.1f).subtract(rotationPoint);
            if (grabComponent.distanceTo(grabComponentNext) < grabComponent.distanceTo(grabComponentPrev)) {
                delta += movement.length();
            } else {
                delta -= movement.length();
            }
        }

        return delta;
    }
}
