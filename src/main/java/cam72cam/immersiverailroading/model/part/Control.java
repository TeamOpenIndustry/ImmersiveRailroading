package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition.ControlSoundsDefinition;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.text.TextColor;
import cam72cam.mod.text.TextUtil;
import cam72cam.mod.util.Axis;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.text.WordUtils;
import util.Matrix4;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Control<T extends EntityMoveableRollingStock> extends Interactable<T> {
    public final String controlGroup;
    public final String label;
    public final boolean toggle;
    public final boolean press;
    public final boolean global;
    protected final boolean invert;
    protected final float offset;
    private final boolean hide;
    private final Vec3d center;
    protected final ModelState state;
    private final String modelId;
    private final boolean noInteract;
    private Vec3d rotationPoint = null;
    private float rotationDegrees = 0;
    private final Map<Axis, Float> rotations = new HashMap<>();
    private final Map<Axis, Float> translations = new HashMap<>();
    private final Map<Axis, Float> scales = new HashMap<>();
    private final Map<Axis, Float> scaleRot = new HashMap<>();

    public static <T extends EntityMoveableRollingStock> List<Control<T>> get(ComponentProvider provider, ModelState state, ModelComponentType type, ModelPosition pos) {
        return provider.parseAll(type, pos).stream().map(part1 -> new Control<T>(part1, state, provider.internal_model_scale, provider.widgetConfig)).collect(Collectors.toList());
    }

    public static <T extends EntityMoveableRollingStock> List<Control<T>> get(ComponentProvider provider, ModelState state, ModelComponentType type) {
        return provider.parseAll(type).stream().map(part1 -> new Control<T>(part1, state, provider.internal_model_scale, provider.widgetConfig)).collect(Collectors.toList());
    }

    public Control(ModelComponent part, ModelState state, double internal_model_scale, Map<String, DataBlock> widgetConfig) {
        super(part);

        // This is terrible
        String rotpat = part.pos != null && !part.type.regex.contains("#POS#") ?
                part.type.regex.replaceAll("#ID#", part.pos + "_" + part.id + "_ROT") :
                part.pos != null ?
                        part.type.regex.replaceAll("#POS#", part.pos.toString()).replaceAll("#ID#", part.id + "_ROT") :
                        part.type.regex.replaceAll("#ID#", part.id + "_ROT");
        // This is worse...
        String name = rotpat.replace("_ROT", "").replaceAll("\\.\\*", "");

        DataBlock config = widgetConfig.containsKey(name) ? widgetConfig.get(name) : new DataBlock() {
            @Override
            public Map<String, Value> getValueMap() { return Collections.emptyMap(); }
            @Override
            public Map<String, List<Value>> getValuesMap() { return Collections.emptyMap(); }
            @Override
            public Map<String, DataBlock> getBlockMap() { return Collections.emptyMap(); }
            @Override
            public Map<String, List<DataBlock>> getBlocksMap() { return Collections.emptyMap(); }
        };

        this.controlGroup = config.getValue("CG").asString(part.modelIDs.stream().map(group -> {
            Matcher matcher = Pattern.compile("_CG_([^_]+)").matcher(group);
            return matcher.find() ? matcher.group(1) : null;
        }).filter(Objects::nonNull).findFirst().orElse(part.key));
        this.label = config.getValue("LABEL").asString(part.modelIDs.stream().map(group -> {
            Matcher matcher = Pattern.compile("_LABEL_([^_]+)").matcher(group);
            return matcher.find() ? matcher.group(1).replaceAll("\\^", " ") : null;
        }).filter(Objects::nonNull).findFirst().orElse(null));

        Predicate<String> hasKey = s -> config.getValue(s).asBoolean(part.modelIDs.stream().anyMatch(g -> g.contains("_" + s + "_") || g.startsWith(s + "_") || g.endsWith("_" + s)));
        this.toggle = hasKey.test("TOGGLE");
        this.press = hasKey.test("PRESS");
        this.global = hasKey.test("GLOBAL");
        this.invert = hasKey.test("INVERT");
        this.hide = hasKey.test("HIDE");
        this.noInteract = hasKey.test("NOTOUCH") || hasKey.test("NOINTERACT");

        this.offset = config.getValue("OFFSET").asFloat(part.modelIDs.stream().map(group -> {
            Matcher matcher = Pattern.compile("_OFFSET_([^_]+)").matcher(group);
            return matcher.find() ? Float.parseFloat(matcher.group(1)) : null;
        }).filter(Objects::nonNull).findFirst().orElse(0f));

        DataBlock rotBlock = config.getBlock("ROT");
        if (rotBlock != null) {
            this.rotationDegrees = rotBlock.getValue("DEGREES").asFloat();
            DataBlock point = rotBlock.getBlock("POINT");
            this.rotationPoint = new Vec3d(
                    point.getValue("X").asDouble(),
                    point.getValue("Y").asDouble(),
                    point.getValue("Z").asDouble()
            );
            DataBlock axis = rotBlock.getBlock("AXIS");
            this.rotations.put(Axis.X, axis.getValue("X").asFloat());
            this.rotations.put(Axis.Y, axis.getValue("Y").asFloat());
            this.rotations.put(Axis.Z, axis.getValue("Z").asFloat());
            this.center = part.center;
        } else {
            OBJGroup rot = part.groups().stream()
                    .filter(g -> Pattern.matches(rotpat, g.name))
                    .findFirst().orElse(null);
            if (rot != null && rot.normal != null) {
                this.rotationPoint = rot.max.add(rot.min).scale(0.5);
                String[] split = rot.name.split("_");
                int idx = ArrayUtils.indexOf(split, "ROT");
                if (idx != ArrayUtils.INDEX_NOT_FOUND) {
                    String degrees = split[idx + 1];
                    try {
                        rotationDegrees = Float.parseFloat(degrees);
                    } catch (NumberFormatException e) {
                        ModCore.error("Unable to parse rotation point '%s': %s", rot.name, e);
                    }
                }

                rotations.put(Axis.X, (float) rot.normal.x);
                rotations.put(Axis.Y, (float) rot.normal.y);
                rotations.put(Axis.Z, (float) rot.normal.z);

                List<Vec3d> nonRotGroups = part.groups().stream().filter(g -> !g.name.contains("_ROT")).map(g -> g.max.add(g.min).scale(0.5)).collect(Collectors.toList());
                this.center = nonRotGroups.isEmpty() ? part.center : nonRotGroups.stream().reduce(Vec3d.ZERO, Vec3d::add).scale(1.0 / nonRotGroups.size());
            } else {
                this.center = part.center;
            }
        }

        DataBlock tl = config.getBlock("TL");
        if (tl != null) {
            tl.getValueMap().forEach((k, v) -> translations.put(Axis.valueOf(k), v.asFloat()));
        } else {
            Pattern pattern = Pattern.compile("TL_([^_]*)_([^_])");
            for (String modelID : part.modelIDs) {
                Matcher matcher = pattern.matcher(modelID);
                while (matcher.find()) {
                    translations.put(Axis.valueOf(matcher.group(2)), Float.parseFloat(matcher.group(1)) * (float) internal_model_scale);
                }
            }
        }
        DataBlock scale = config.getBlock("SCALE");
        if (scale != null) {
            scale.getValueMap().forEach((k, v) -> scales.put(Axis.valueOf(k), v.asFloat()));
            DataBlock r = scale.getBlock("R");
            if (r != null) {
                r.getValueMap().forEach((k, v) -> scaleRot.put(Axis.valueOf(k), v.asFloat()));
            }
        } else {
            Pattern pattern = Pattern.compile("SCALE_([^_]*)_([^_]+)");
            for (String modelID : part.modelIDs) {
                Matcher matcher = pattern.matcher(modelID);
                while (matcher.find()) {
                    if (matcher.group(2).startsWith("R")) {
                        scaleRot.put(Axis.valueOf(matcher.group(2).substring(1)), Float.parseFloat(matcher.group(1)));
                    } else {
                        scales.put(Axis.valueOf(matcher.group(2)), Float.parseFloat(matcher.group(1)));
                    }
                }
            }
        }

        if (hide) {
            state = state.push(builder ->
                    builder.add((ModelState.GroupVisibility) (stock, group) -> getValue(stock) != 1)
            );
        }

        if (!(rotationPoint == null && translations.isEmpty() && scales.isEmpty())) {
            state = state.push(builder -> {
                builder.add((ModelState.GroupAnimator) (stock, group, partialTicks) -> {
                    float valuePercent = getValue(stock);

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
                                rotations.getOrDefault(Axis.X, 0f),
                                rotations.getOrDefault(Axis.Y, 0f),
                                rotations.getOrDefault(Axis.Z, 0f)
                        );
                        m = m.translate(-rotationPoint.x, -rotationPoint.y, -rotationPoint.z);
                    }
                    if (!scales.isEmpty()) {
                        m = m.translate(part.center.x, part.center.y, part.center.z);
                        for (Map.Entry<Axis, Float> entry : scaleRot.entrySet()) {
                            Axis axis = entry.getKey();
                            m = m.rotate(Math.toRadians(
                                            entry.getValue()),
                                    axis == Axis.X ? 1 : 0,
                                    axis == Axis.Y ? 1 : 0,
                                    axis == Axis.Z ? 1 : 0
                            );
                        }
                        m = m.scale(
                                scales.containsKey(Axis.X) ? (1 - scales.get(Axis.X)) + (scales.get(Axis.X) * valuePercent) : 1,
                                scales.containsKey(Axis.Y) ? (1 - scales.get(Axis.Y)) + (scales.get(Axis.Y) * valuePercent) : 1,
                                scales.containsKey(Axis.Z) ? (1 - scales.get(Axis.Z)) + (scales.get(Axis.Z) * valuePercent) : 1
                        );
                        for (Map.Entry<Axis, Float> entry : scaleRot.entrySet()) {
                            Axis axis = entry.getKey();
                            m = m.rotate(Math.toRadians(
                                            -entry.getValue()),
                                    axis == Axis.X ? 1 : 0,
                                    axis == Axis.Y ? 1 : 0,
                                    axis == Axis.Z ? 1 : 0
                            );
                        }
                        m = m.translate(-part.center.x, -part.center.y, -part.center.z);
                    }
                    return m;
                });
            });
        }
        this.state = state;
        this.state.include(part);
        this.modelId = part.modelIDs.stream().findFirst().get();
    }

    @Override
    public boolean disabled() {
        return noInteract;
    }

    private static String formatLabel(ModelComponentType label) {
        return WordUtils.capitalizeFully(label.name().replace("_X", "").replaceAll("_CONTROL", "").replaceAll("_", " ").toLowerCase(Locale.ROOT));
    }

    public void postRender(T stock, RenderState state, float partialTicks) {
        if (!ConfigGraphics.interactiveComponentsOverlay) {
            return;
        }

        if (!stock.playerCanDrag(MinecraftClient.getPlayer(), this)) {
            return;
        }

        if (MinecraftClient.getPlayer().getPosition().distanceTo(stock.getPosition()) > stock.getDefinition().getLength(stock.gauge)) {
            return;
        }

        boolean isPressing = stock.getControlPressed(this);
        if (!isPressing && Math.abs(lookedAt - stock.getWorld().getTicks()) > 2) {
            return;
        }

        Matrix4 m = this.state.getGroupMatrix(stock, modelId, partialTicks);
        Vec3d pos = m == null ? center : m.apply(center);
        String labelstate = "";
        float percent = getValue(stock) - offset;
        switch (part.type) {
            case REVERSER_X:
                percent *= -2;
                // Fallthrough
            case TRAIN_BRAKE_X:
            case INDEPENDENT_BRAKE_X:
                if (!stock.getDefinition().isLinearBrakeControl()) {
                     break;
                }
                // Fallthrough
            case THROTTLE_X:
            case THROTTLE_BRAKE_X:
            case BELL_CONTROL_X:
            case WHISTLE_CONTROL_X:
            case HORN_CONTROL_X:
            case ENGINE_START_X:
            case CYLINDER_DRAIN_CONTROL_X:
                if (toggle || press) {
                    labelstate = " (" + TextUtil.translate(percent == 1 ? GuiText.ON.getRaw() : GuiText.OFF.getRaw()) + ")";
                } else {
                    labelstate = String.format(" (%d%%)", (int)(percent * 100));
                }
                break;
            default:
                if (label == null || label.trim().isEmpty()) {
                    return;
                }
                if (toggle || press) {
                    labelstate = " (" + TextUtil.translate(percent == 1 ? GuiText.ON.getRaw() : GuiText.OFF.getRaw()) + ")";
                } else {
                    labelstate = String.format(" (%d%%)", (int)(percent * 100));
                }
        }
        System.out.println(this.modelId);
        String str = (label != null ? label : formatLabel(part.type)) + labelstate;
        if (isPressing) {
            str = TextColor.BOLD.wrap(str);
        }
        GlobalRender.drawText(str, state, pos, 0.2f, 180 - stock.getRotationYaw() - 90);
    }

    public float getValue(EntityMoveableRollingStock stock) {
        float pos = stock.getControlPosition(this) + offset;
        return (invert ? 1 - pos : pos) - (part.type == ModelComponentType.REVERSER_X || part.type == ModelComponentType.THROTTLE_BRAKE_X ? 0.5f : 0);
    }

    public Vec3d transform(Vec3d point, T stock) {
        float partialTicks = 0;
        Matrix4 m = state.getGroupMatrix(stock, modelId, partialTicks);
        if (m == null) {
            m = stock.getModelMatrix();
        } else {
            m = stock.getModelMatrix().multiply(m);
        }
        return m.apply(point);
    }

    @Override
    public Vec3d center(EntityRollingStock stock) {
        return transform(part.center, (T)stock);
    }

    @Override
    public IBoundingBox getBoundingBox(EntityRollingStock stock) {
        return IBoundingBox.from(
                transform(part.min, (T)stock),
                transform(part.max, (T)stock)
        );
    }

    /** Client only! */
    private Vec3d lastClientLook = null;
    public float clientMovementDelta(Player player, EntityRollingStock stockRaw) {
        /*
          -X
        -Z * +Z
          +X
         */

        if (press) {
            return 1;
        }

        T stock = (T)stockRaw;

        float delta = 0;

        Vec3d partPos = transform(center, stock).subtract(stock.getPosition());
        Vec3d current = player.getPositionEyes().subtract(stock.getPosition());
        Vec3d look = player.getLookVector();
        // Rescale along look vector
        double len = 1 + current.add(look).distanceTo(partPos);
        current = current.add(look.scale(len));
        current = current.rotateYaw(stock.getRotationYaw());

        if (lastClientLook != null) {
            Vec3d movement = current.subtract(lastClientLook);
            movement = movement.rotateYaw(-stock.getRotationYaw());
            float applied = Math.min(0.1f, (float) (movement.length()*1));
            if (rotationDegrees <= 180) {
                float value = stock.getControlPosition(this);//getValue(stock);  // Does this work with invert???
                Vec3d grabComponent = transform(center, stock).add(movement);

                stock.setControlPosition(this, value + applied);
                Vec3d grabComponentNext = transform(center, stock);
                stock.setControlPosition(this, value - applied);
                Vec3d grabComponentPrev = transform(center, stock);

                stock.setControlPosition(this, value);

                if (grabComponent.distanceTo(grabComponentNext) < grabComponent.distanceTo(grabComponentPrev)) {
                    delta += applied * movement.length() / grabComponent.distanceTo(grabComponentNext);
                } else {
                    delta -= applied * movement.length() / grabComponent.distanceTo(grabComponentPrev);
                }
            } else {
                // hack spinning wheels
                if (movement.x * (1-rotationPoint.x) + movement.y * (1-rotationPoint.y) + movement.z * (1-rotationPoint.z) > 0) {
                    delta += applied;
                } else {
                    delta -= applied;
                }
            }
        }
        lastClientLook = current;

        return Float.isNaN(delta) ? 0 : delta;
    }

    public void stopClientDragging() {
        lastClientLook = null;
    }

    public void effects(T stock) {
        ControlSoundsDefinition sounds = stock.getDefinition().getControlSound(part.type.name().replace("_X", "_" + part.id));
        if (sounds != null) {
            sounds.effects(stock, stock.getControlPressed(this), stock.getControlPosition(this), center(stock));
        }
    }
    public void removed(T stock) {
        ControlSoundsDefinition sounds = stock.getDefinition().getControlSound(part.type.name().replace("_X", "_" + part.id));
        if (sounds != null) {
            sounds.removed(stock);
        }
    }
}
