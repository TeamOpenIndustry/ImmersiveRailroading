package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Readout<T extends EntityRollingStock> extends Control {
    private final Map<UUID, Float> positions = new HashMap<>();
    private final Function<T, Float> position;

    public static <T extends EntityRollingStock> List<Readout<T>> getReadouts(ComponentProvider provider, ModelComponentType type, Function<T, Float> position) {
        return provider.parseAll(type).stream().map(p -> new Readout<>(p, position)).collect(Collectors.toList());
    }

    public Readout(ModelComponent part, Function<T, Float> position) {
        super(part);
        this.position = position;
    }

    @Override
    public void effects(EntityRollingStock stock) {
        super.effects(stock);
        positions.put(stock.getUUID(), position.apply((T) stock));
    }

    @Override
    public float getValue(EntityRollingStock stock) {
        return positions.getOrDefault(stock.getUUID(), 0f);
    }
}
