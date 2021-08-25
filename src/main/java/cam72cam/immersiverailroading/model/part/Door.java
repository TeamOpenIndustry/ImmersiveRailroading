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

    public static List<Door> get(OBJModel model, ComponentProvider provider) {
        ModelComponentType type = ModelComponentType.DOOR_X;
        return provider.parseAll(type).stream().map(part -> {
            OBJGroup rot = model.groups.values().stream().filter(g -> Pattern.matches(type.regex.replaceAll("#ID#",  part.id + "_ROT"), g.name)).findFirst().orElse(null);
            return new Door(part, rot);
        }).collect(Collectors.toList());
    }

    public Door(ModelComponent part, OBJGroup rot) {
        super(part, rot);
    }

    public boolean isAtOpenDoor(Player player, EntityRollingStock stock) {
        if (this.part.type != ModelComponentType.DOOR_X) {
            return false;
        }
        int cool = cooldown.getOrDefault(player.getUUID(), 0);
        if (player.getTickCount() < cool + 10 && player.getTickCount() > cool) {
            return false;
        }
        if (stock.getControlPosition(this) < 0.75 || player.getPosition().distanceTo(stock.getPosition()) > stock.getDefinition().getLength(stock.gauge)) {
            return false;
        }
        if (part.modelIDs.stream().allMatch(x -> x.contains("INTERNAL"))) {
            return false;
        }
        Vec3d playerPos = new Matrix4().rotate(Math.toRadians(stock.getRotationYaw() - 90), 0, 1, 0).apply(player.getPosition().add(0, 0.5, 0).subtract(stock.getPosition()));
        IBoundingBox bb = IBoundingBox.from(
                transform(part.min, 0, stock.gauge.scale()),
                transform(part.max, 0, stock.gauge.scale())
        ).grow(new Vec3d(0.5, 0.5, 0.5));
        if (!bb.contains(playerPos)) {
            return false;
        }
        cooldown.put(player.getUUID(), player.getTickCount());
        return true;
    }
}
