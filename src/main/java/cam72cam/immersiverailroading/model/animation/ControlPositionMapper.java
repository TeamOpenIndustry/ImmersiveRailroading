package cam72cam.immersiverailroading.model.animation;

import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.function.Function;

public class ControlPositionMapper implements TagMapper<Map<String, Pair<Boolean, Float>>> {
    @Override
    public TagAccessor<Map<String, Pair<Boolean, Float>>> apply(
            Class<Map<String, Pair<Boolean, Float>>> type,
            String fieldName,
            TagField tag) throws SerializationException {
        return new TagAccessor<>(
                (d, o) -> d.setMap(fieldName, o, Function.identity(), x -> new TagCompound().setBoolean("pressed", x.getLeft()).setFloat("pos", x.getRight())),
                d -> d.getMap(fieldName, Function.identity(), x -> Pair.of(x.hasKey("pressed") && x.getBoolean("pressed"), x.getFloat("pos")))
        );
    }
}
