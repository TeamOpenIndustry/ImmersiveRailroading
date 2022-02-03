package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition.ControlSoundsDefinition;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.sound.ISound;
import cam72cam.mod.util.Axis;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.text.WordUtils;
import util.Matrix4;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Control<T extends EntityMoveableRollingStock>  {
    public final ModelComponent part;
    public final String controlGroup;
    public final String label;
    public final boolean toggle;
    public final boolean press;
    public final boolean global;
    protected final boolean invert;
    protected final float offset;
    private final boolean hide;
    private final Vec3d center;
    private Vec3d rotationPoint = null;
    private float rotationDegrees = 0;
    private final Map<Axis, Float> rotations = new HashMap<>();
    private final Map<Axis, Float> translations = new HashMap<>();
    private final Map<Axis, Float> scales = new HashMap<>();
    private final Map<Axis, Float> scaleRot = new HashMap<>();
    private final Map<UUID, Float> lastMoveSoundValue = new HashMap<>();
    private final Map<UUID, Boolean> wasSoundPressed = new HashMap<>();
    private final Map<UUID, List<ISound>> sounds = new HashMap<>();
    private final Function<T, Matrix4> location;

    public static <T extends EntityMoveableRollingStock> List<Control<T>> get(ComponentProvider provider, ModelComponentType type) {
        return provider.parseAll(type).stream().map(part1 -> new Control<T>(part1, null)).collect(Collectors.toList());
    }

    public static <T extends EntityMoveableRollingStock> List<Control<T>> get(ComponentProvider provider, ModelComponentType type, ModelPosition pos, Function<T, Matrix4> loc) {
        return provider.parseAll(type, pos).stream().map(part1 -> new Control<>(part1, loc)).collect(Collectors.toList());
    }

    public Control(ModelComponent part, Function<T, Matrix4> loc) {
        this.part = part;
        this.location = loc;
        this.controlGroup = part.modelIDs.stream().map(group -> {
            Matcher matcher = Pattern.compile("_CG_([^_]+)").matcher(group);
            return matcher.find() ? matcher.group(1) : null;
        }).filter(Objects::nonNull).findFirst().orElse(part.key);
        this.label = part.modelIDs.stream().map(group -> {
            Matcher matcher = Pattern.compile("_LABEL_([^_]+)").matcher(group);
            return matcher.find() ? matcher.group(1).replaceAll("\\^", " ") : null;
        }).filter(Objects::nonNull).findFirst().orElse(null);
        this.toggle = part.modelIDs.stream().anyMatch(g -> g.contains("_TOGGLE_") || g.startsWith("TOGGLE_") || g.endsWith("_TOGGLE"));
        this.press = part.modelIDs.stream().anyMatch(g -> g.contains("_PRESS_") || g.startsWith("PRESS_") || g.endsWith("_PRESS"));
        this.global = part.modelIDs.stream().anyMatch(g -> g.contains("_GLOBAL_") || g.startsWith("GLOBAL_") || g.endsWith("_GLOBAL"));
        this.invert = part.modelIDs.stream().anyMatch(g -> g.contains("_INVERT_") || g.startsWith("INVERT_") || g.endsWith("_INVERT"));
        this.hide = part.modelIDs.stream().anyMatch(g -> g.contains("_HIDE_") || g.startsWith("HIDE_") || g.endsWith("_HIDE"));
        this.offset = part.modelIDs.stream().map(group -> {
            Matcher matcher = Pattern.compile("_OFFSET_([^_]+)").matcher(group);
            return matcher.find() ? Float.parseFloat(matcher.group(1)) : null;
        }).filter(Objects::nonNull).findFirst().orElse(0f);

        // This is terrible
        String rotpat = part.pos != null && !part.type.regex.contains("#POS#") ?
                part.type.regex.replaceAll("#ID#", part.pos + "_" + part.id + "_ROT") :
                part.pos != null ?
                        part.type.regex.replaceAll("#POS#", part.pos.toString()).replaceAll("#ID#", part.id + "_ROT") :
                        part.type.regex.replaceAll("#ID#", part.id + "_ROT");
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
            this.center = nonRotGroups.isEmpty() ? part.center : nonRotGroups.stream().reduce(Vec3d.ZERO, Vec3d::add).scale(1.0/nonRotGroups.size());
        } else {
            this.center = part.center;
        }

        Pattern pattern = Pattern.compile("TL_([^_]*)_([^_])");
        for (String modelID : part.modelIDs) {
            Matcher matcher = pattern.matcher(modelID);
            while (matcher.find()) {
                translations.put(Axis.valueOf(matcher.group(2)), Float.parseFloat(matcher.group(1)));
            }
        }
        pattern = Pattern.compile("SCALE_([^_]*)_([^_]+)");
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

    private static String formatLabel(ModelComponentType label) {
        return WordUtils.capitalizeFully(label.name().replace("_X", "").replaceAll("_CONTROL", "").replaceAll("_", " ").toLowerCase(Locale.ROOT));
    }

    public void render(T stock, ComponentRenderer draw) {
        float valuePercent = getValue(stock);
        if (hide && valuePercent == 1) {
            return;
        }


        if (rotationPoint == null && translations.isEmpty() && scales.isEmpty()) {
            draw.render(part);
            return;
        }

        try (ComponentRenderer matrix = draw.push()) {
            matrix.mult(transform(getValue(stock), new Matrix4(), stock));
            matrix.render(part);
        }
    }

    public void postRender(T stock) {
        if (!ConfigGraphics.interactiveComponentsOverlay) {
            return;
        }

        if (!stock.playerCanDrag(MinecraftClient.getPlayer(), this)) {
            return;
        }

        if (MinecraftClient.getPlayer().getPosition().distanceTo(stock.getPosition()) > stock.getDefinition().getLength(stock.gauge)) {
            return;
        }

        Player player = MinecraftClient.getPlayer();

        if (transform(center, stock).distanceTo(player.getPositionEyes().add(stock.getVelocity())) > 4) {
            return;
        }


        IBoundingBox bb = IBoundingBox.from(
                transform(part.min, stock),
                transform(part.max, stock)
        ).grow(new Vec3d(0.05, 0.05, 0.05));
        // The added velocity is due to a bug where the player may tick before or after the stock.
        // Ideally we'd be able to fix this in UMC and have all UMC entities tick after the main entities
        // or at least expose a "tick order" function as crappy as that would be...
        boolean inRange = false;
        Vec3d delta = bb.max().subtract(bb.min());
        double step = Math.max(0.01, Math.min(delta.x, Math.min(delta.y, delta.z))/2);
        for (double i = 0; i < 2; i+=step) {
            inRange = inRange || bb.contains(player.getPositionEyes().add(player.getLookVector().scale(i)).add(stock.getVelocity()));
        }
        if (!inRange) {
            return;
        }
        Vec3d pos = transform(getValue(stock), new Matrix4().scale(stock.gauge.scale(), stock.gauge.scale(), stock.gauge.scale()), stock).apply(center);
        String state = "";
        float percent = getValue(stock) - offset;
        switch (part.type) {
            case TRAIN_BRAKE_X:
            case INDEPENDENT_BRAKE_X:
                if (!stock.getDefinition().isLinearBrakeControl()) {
                     break;
                }
                // Fallthrough
            case THROTTLE_X:
            case REVERSER_X:
            case THROTTLE_BRAKE_X:
            case BELL_CONTROL_X:
            case WHISTLE_CONTROL_X:
            case HORN_CONTROL_X:
            case ENGINE_START_X:
                if (part.type == ModelComponentType.REVERSER_X) {
                    percent *= -2;
                }
                if (toggle || press) {
                    state = percent == 1 ? " (On)" : " (Off)";
                } else {
                    state = String.format(" (%d%%)", (int)(percent * 100));
                }
                break;
            default:
                if (label == null || label.trim().isEmpty()) {
                    return;
                }
                if (toggle || press) {
                    state = percent == 1 ? " (On)" : " (Off)";
                } else {
                    state = String.format(" (%d%%)", (int)(percent * 100));
                }
        }
        GlobalRender.drawText((label != null ? label : formatLabel(part.type)) + state, pos, 0.2f, 180 - stock.getRotationYaw() - 90);
    }

    public float getValue(T stock) {
        float pos = stock.getControlPosition(this) + offset;
        return (invert ? 1 - pos : pos) - (part.type == ModelComponentType.REVERSER_X || part.type == ModelComponentType.THROTTLE_BRAKE_X ? 0.5f : 0);
    }

    public Vec3d transform(Vec3d point, T stock) {
        return transform(point, getValue(stock), stock);
    }

    protected Vec3d transform(Vec3d point, float valuePercent, T stock) {
        return transform(valuePercent, stock.getModelMatrix(), stock).apply(point);
    }

    protected Matrix4 transform(float valuePercent, Matrix4 m, T stock) {
        if (location != null) {
            m.multiply(location.apply(stock));
        }

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
    }

    public Vec3d center(EntityRollingStock stock) {
        return transform(part.center, (T)stock);
    }

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
            float applied = (float) (movement.length());
            if (rotationDegrees <= 180) {
                float value = getValue(stock);
                Vec3d grabComponent = transform(center, value, stock).add(movement);
                Vec3d grabComponentNext = transform(center, value + applied, stock);
                Vec3d grabComponentPrev = transform(center, value - applied, stock);
                if (grabComponent.distanceTo(grabComponentNext) < grabComponent.distanceTo(grabComponentPrev)) {
                    delta += applied;
                } else {
                    delta -= applied;
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

    public ControlSoundsDefinition getSounds(T stock) {
        return stock.getDefinition().getControlSound(part.type.name().replace("_X", "_" + part.id));
    }

    private void createSound(T stock, Identifier sound, boolean repeats) {
        if (sound == null) {
            return;
        }
        ISound snd = ImmersiveRailroading.newSound(sound, repeats, 10, stock.gauge);
        snd.setVelocity(stock.getVelocity());
        snd.setVolume(1);
        snd.setPitch(1f);
        snd.disposable();
        snd.play(center(stock));
        sounds.computeIfAbsent(stock.getUUID(), k -> new ArrayList<>()).add(snd);
    }

    public void effects(T stock) {
        ControlSoundsDefinition sounds = getSounds(stock);
        if (sounds != null) {
            if (this.sounds.containsKey(stock.getUUID())) {
                for (ISound snd : new ArrayList<>(this.sounds.get(stock.getUUID()))) {
                    if (snd.isPlaying()) {
                        snd.setVelocity(stock.getVelocity());
                        snd.setPosition(center(stock));
                    } else {
                        this.sounds.get(stock.getUUID()).remove(snd);
                    }
                }
            }

            boolean isPressed = stock.getControlPressed(this);
            Boolean wasPressed = wasSoundPressed.getOrDefault(stock.getUUID(), isPressed);
            wasSoundPressed.put(stock.getUUID(), isPressed);

            float value = stock.getControlPosition(this);
            float lastValue = lastMoveSoundValue.computeIfAbsent(stock.getUUID(), k -> value);

            if (!wasPressed && isPressed) {
                // Start
                createSound(stock, sounds.engage, false);
                if (sounds.move != null && sounds.movePercent == null) {
                    // Start move repeat
                    createSound(stock, sounds.move, true);
                }
            } else if (wasPressed && !isPressed) {
                // Release
                if (this.sounds.containsKey(stock.getUUID())) {
                    for (ISound snd : this.sounds.get(stock.getUUID())) {
                        snd.terminate();
                    }
                }
                createSound(stock, sounds.disengage, false);
            } else if (sounds.move != null && sounds.movePercent != null){
                // Move
                if (Math.abs(lastValue - value) > sounds.movePercent) {
                    createSound(stock, sounds.move, false);
                    lastMoveSoundValue.put(stock.getUUID(), value);
                }
            }
        }
    }
}
