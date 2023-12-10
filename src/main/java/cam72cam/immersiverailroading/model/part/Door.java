package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import util.Matrix4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class Door<T extends EntityMoveableRollingStock> extends Control<T> {
    private static final Map<UUID, Integer> cooldown = new HashMap<>();

    public enum Types {
        INTERNAL,
        CONNECTING,
        EXTERNAL
    }

    public final Types type;

    public static <T extends EntityMoveableRollingStock> List<Door<T>> get(ComponentProvider provider, ModelState state) {
        return provider.parseAll(ModelComponentType.DOOR_X).stream().map(p -> new Door<T>(p, state, provider.internal_model_scale, provider.widgetConfig)).collect(Collectors.toList());
    }

    public static <T extends EntityMoveableRollingStock> List<Door<T>> get(ComponentProvider provider, ModelState state, ModelPosition pos) {
        return provider.parseAll(ModelComponentType.DOOR_X, pos).stream().map(p -> new Door<T>(p, state, provider.internal_model_scale, provider.widgetConfig)).collect(Collectors.toList());
    }

    public Door(ModelComponent part, ModelState state, double internal_model_scale, Map<String, DataBlock> widgetConfig) {
        super(part, state, internal_model_scale, widgetConfig);
        type = part.modelIDs.stream().anyMatch(g -> g.contains("EXTERNAL")) ? Types.EXTERNAL :
                part.modelIDs.stream().anyMatch(g -> g.contains("CONNECTING")) ? Types.CONNECTING :
                Types.INTERNAL;
    }


    public boolean isOpen(EntityRollingStock stock) {
        return stock.getControlPosition(this) > 0.75;
    }

    public boolean isAtOpenDoor(Player player, EntityRollingStock stock, Types type) {
        if (this.type != type) {
            return false;
        }
        int cool = cooldown.getOrDefault(player.getUUID(), 0);
        if (player.getTickCount() < cool + 10 && player.getTickCount() > cool) {
            return false;
        }
        if (!isOpen(stock) || player.getPosition().distanceTo(stock.getPosition()) > stock.getDefinition().getLength(stock.gauge)) {
            return false;
        }
        Matrix4 model = stock.getModelMatrix();
        Matrix4 delta = state.getMatrix((T) stock, 0);
        if (delta != null) {
            model = model.multiply(delta);
        }
        IBoundingBox bb = IBoundingBox.from(
                model.apply(part.min),
                model.apply(part.max)
        ).grow(new Vec3d(0.5, 0.5, 0.5));
        // The added velocity is due to a bug where the player may tick before or after the stock.
        // Ideally we'd be able to fix this in UMC and have all UMC entities tick after the main entities
        // or at least expose a "tick order" function as crappy as that would be...
        if (!bb.contains(player.getPositionEyes().add(stock.getVelocity()))) {
            return false;
        }
        cooldown.put(player.getUUID(), player.getTickCount());
        return true;
    }
}
