package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.util.Axis;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.opengl.GL11;
import util.Matrix4;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Control {
    public final ModelComponent part;
    public final String controlGroup;
    public final String label;
    public final boolean toggle;
    public final boolean press;
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
        this.controlGroup = part.modelIDs.stream().map(group -> {
            Matcher matcher = Pattern.compile("_CG_([^_]+)").matcher(group);
            return matcher.find() ? matcher.group(1) : null;
        }).filter(Objects::nonNull).findFirst().orElse(part.key);
        this.label = part.modelIDs.stream().map(group -> {
            Matcher matcher = Pattern.compile("_LABEL_([^_]+)").matcher(group);
            return matcher.find() ? matcher.group(1) : null;
        }).filter(Objects::nonNull).findFirst().orElse(part.type.name().replace("_X", ""));
        this.toggle = part.modelIDs.stream().anyMatch(g -> g.contains("_TOGGLE_") || g.startsWith("TOGGLE_") || g.endsWith("_TOGGLE"));
        this.press = part.modelIDs.stream().anyMatch(g -> g.contains("_PRESS_") || g.startsWith("PRESS_") || g.endsWith("_PRESS"));

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

    public void render(EntityRollingStock stock, ComponentRenderer draw) {
        if (rotationPoint == null && translations.isEmpty()) {
            draw.render(part);
            return;
        }

        float valuePercent = getValue(stock);

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

    public void postRender(EntityRollingStock stock) {
        if (!ConfigGraphics.interactiveComponentsOverlay) {
            return;
        }

        if (MinecraftClient.getPlayer().getPosition().distanceTo(stock.getPosition()) > stock.getDefinition().getLength(stock.gauge)) {
            return;
        }

        Vec3d pos = transform(part.center, getValue(stock), stock);
        Vec3d playerPos = MinecraftClient.getPlayer().getPositionEyes().add(MinecraftClient.getPlayer().getLookVector());
        if (playerPos.distanceTo(pos) > 0.5) {
            return;
        }

        pos = transform(part.center, getValue(stock), new Matrix4().scale(stock.gauge.scale(), stock.gauge.scale(), stock.gauge.scale()));
        GlobalRender.drawText(label, pos, 0.2f, 180 - stock.getRotationYaw() - 90);
    }

    public float getValue(EntityRollingStock stock) {
        return stock.getControlPosition(this) - (part.type == ModelComponentType.REVERSER_X ? 0.5f : 0);
    }

    public Vec3d transform(Vec3d point, EntityRollingStock stock) {
        return transform(point, getValue(stock), stock);
    }

    protected Vec3d transform(Vec3d point, float valuePercent, EntityRollingStock stock) {
        return transform(point, valuePercent, stock.getModelMatrix());
    }

    protected Vec3d transform(Vec3d point, float valuePercent, Matrix4 m) {
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

    public Vec3d center(EntityRollingStock stock) {
        return transform(part.center, stock);
    }

    public IBoundingBox getBoundingBox(EntityRollingStock stock) {
        return IBoundingBox.from(
                transform(part.min, stock),
                transform(part.max, stock)
        );
    }

    /** Client only! */
    private Vec3d lastClientLook = null;
    public float clientMovementDelta(Player player, EntityRollingStock stock) {
        /*
          -X
        -Z * +Z
          +X
         */

        if (press) {
            return 1;
        }

        float delta = 0;

        Vec3d partPos = transform(part.center, stock).subtract(stock.getPosition());
        Vec3d current = player.getPositionEyes().subtract(stock.getPosition());
        Vec3d look = player.getLookVector();
        // Rescale along look vector
        double len = 1 + current.add(look).distanceTo(partPos);
        current = current.add(look.scale(len));
        current = current.rotateYaw(stock.getRotationYaw());

        if (lastClientLook != null) {
            Vec3d movement = current.subtract(lastClientLook);
            movement = movement.rotateYaw(-stock.getRotationYaw());
            float applied = (float) (movement.length());
            float value = getValue(stock);
            Vec3d grabComponent = transform(part.center, value, stock).add(movement);
            Vec3d grabComponentNext = transform(part.center, value + applied, stock);
            Vec3d grabComponentPrev = transform(part.center, value - applied, stock);
            if (grabComponent.distanceTo(grabComponentNext) < grabComponent.distanceTo(grabComponentPrev)) {
                delta += applied;
            } else {
                delta -= applied;
            }
        }
        lastClientLook = current;

        return delta;
    }

    public void stopClientDragging() {
        lastClientLook = null;
    }
}
