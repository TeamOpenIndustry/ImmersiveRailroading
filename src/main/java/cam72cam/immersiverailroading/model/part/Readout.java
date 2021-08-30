package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.model.obj.OBJModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Readout extends Control {
    private final Map<UUID, Float> positions = new HashMap<>();

    public static List<Readout> getReadouts(OBJModel model, ComponentProvider provider, ModelComponentType type) {
        return provider.parseAll(type).stream().map(part -> {
            OBJGroup rot = model.groups.values().stream().filter(g -> Pattern.matches(type.regex.replaceAll("#ID#",  part.id + "_ROT"), g.name)).findFirst().orElse(null);
            return new Readout(part, rot, model);
        }).collect(Collectors.toList());
    }

    public Readout(ModelComponent part, OBJGroup rot, OBJModel model) {
        super(part, model, rot);
    }

    public void setValue(EntityRollingStock stock, float value) {
        positions.put(stock.getUUID(), value);
    }

    @Override
    public float getValue(EntityRollingStock stock) {
        return positions.getOrDefault(stock.getUUID(), 0f);
    }
}
