package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import util.Matrix4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Readout<T extends EntityMoveableRollingStock> extends Control<T> {
    private final Map<UUID, Float> positions = new HashMap<>();
    private final Function<T, Float> position;
    private final float rangeMin;
    private final float rangeMax;

    public static <T extends EntityMoveableRollingStock> List<Readout<T>> getReadouts(ComponentProvider provider, ModelComponentType type, Readouts value) {
        return provider.parseAll(type).stream().map(p -> new Readout<>(p, (Function<T, Float>) value::getValue, null)).collect(Collectors.toList());
    }

    public static <T extends EntityMoveableRollingStock> List<Readout<T>> getReadouts(ComponentProvider provider, ModelComponentType type, ModelPosition pos, Readouts value, Function<T, Matrix4> loc) {
        return provider.parseAll(type, pos).stream().map(p -> new Readout<>(p, value::getValue, loc)).collect(Collectors.toList());
    }

    public Readout(ModelComponent part, Function<T, Float> position, Function<T, Matrix4> loc) {
        super(null, part, loc);
        this.position = position;

        float min = 0;
        float max = 1;
        Pattern pattern = Pattern.compile("_RANGE_([^_]*)_([^_]*)");
        for (String modelID : part.modelIDs) {
            Matcher matcher = pattern.matcher(modelID);
            while (matcher.find()) {
                min = Float.parseFloat(matcher.group(1));
                max = Float.parseFloat(matcher.group(2));
            }
        }
        rangeMin = min;
        rangeMax = max;
    }

    @Override
    public void effects(T stock) {
        super.effects(stock);
        positions.put(stock.getUUID(), position.apply((T) stock));
    }

    @Override
    public float getValue(T stock) {
        float pos = positions.getOrDefault(stock.getUUID(), 0f);
        pos = Math.min(1, Math.max(0, (pos - rangeMin) / (rangeMax - rangeMin)));
        pos = pos + offset;
        return invert ? 1 - pos : pos;
    }
}
