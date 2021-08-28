package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.model.obj.OBJModel;
import util.Matrix4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Door extends Control {
    private static final Map<UUID, Integer> cooldown = new HashMap<>();

    public enum Types {
        INTERNAL,
        CONNECTING,
        EXTERNAL
    }

    public final Types type;

    public static List<Door> get(OBJModel model, ComponentProvider provider) {
        ModelComponentType type = ModelComponentType.DOOR_X;
        return provider.parseAll(type).stream().map(part -> {
            OBJGroup rot = model.groups.values().stream().filter(g -> Pattern.matches(type.regex.replaceAll("#ID#",  part.id + "_ROT"), g.name)).findFirst().orElse(null);
            return new Door(part, rot);
        }).collect(Collectors.toList());
    }

    public Door(ModelComponent part, OBJGroup rot) {
        super(part, rot);
        type = part.modelIDs.stream().anyMatch(g -> g.contains("EXTERNAL")) ? Types.EXTERNAL :
                part.modelIDs.stream().anyMatch(g -> g.contains("CONNECTING")) ? Types.CONNECTING :
                Types.INTERNAL;
    }

    public boolean isAtOpenDoor(Player player, EntityRollingStock stock, Types type) {
        if (this.type != type) {
            return false;
        }
        int cool = cooldown.getOrDefault(player.getUUID(), 0);
        if (player.getTickCount() < cool + 10 && player.getTickCount() > cool) {
            return false;
        }
        if (stock.getControlPosition(this) < 0.75 || player.getPosition().distanceTo(stock.getPosition()) > stock.getDefinition().getLength(stock.gauge)) {
            return false;
        }
        IBoundingBox bb = IBoundingBox.from(
                transform(part.min, 0, stock),
                transform(part.max, 0, stock)
        ).grow(new Vec3d(0.5, 0.5, 0.5));
        if (!bb.contains(player.getPositionEyes())) {
            return false;
        }
        cooldown.put(player.getUUID(), player.getTickCount());
        return true;
    }
}
