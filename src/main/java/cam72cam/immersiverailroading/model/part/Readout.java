package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class Readout extends Control {
    private final Map<UUID, Float> positions = new HashMap<>();

    public static List<Readout> getReadouts(ComponentProvider provider, ModelComponentType type) {
        return provider.parseAll(type).stream().map(Readout::new).collect(Collectors.toList());
    }

    public Readout(ModelComponent part) {
        super(part);
    }

    public void setValue(EntityRollingStock stock, float value) {
        positions.put(stock.getUUID(), value);
    }

    @Override
    public float getValue(EntityRollingStock stock) {
        return positions.getOrDefault(stock.getUUID(), 0f);
    }
}
