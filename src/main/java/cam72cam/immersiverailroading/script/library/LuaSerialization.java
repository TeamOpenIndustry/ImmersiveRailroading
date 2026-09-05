package cam72cam.immersiverailroading.script.library;

import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LuaSerialization {

    private LuaSerialization() {
        /* */
    }

    private static TagCompound serializeLuaValue(TagCompound compound, String key, LuaValue value) {
        switch (value.typename()) {
            case "boolean" -> compound.setBoolean(key, value.toboolean());
            case "number" -> {
                if (value.isint()) {
                    compound.setInteger(key, value.toint());
                } else if (value.isnumber()) {
                    compound.setFloat(key, value.tofloat());
                }
            }
            case "string" -> compound.setString(key, value.tojstring());
            case "table" -> {
                List<LuaValue> list = Arrays.stream(value.checktable().keys()).collect(Collectors.toList());
                compound.setList(key, list, l -> serializeLuaValue(compound, "index", l));
            }
            default -> {
                /* not supported */
            }
        }

        return compound;
    }

    private static LuaValue deserializeLuaValue(TagCompound compound, String key) {
        if (compound.hasKey(key)) {
            if (compound.getBoolean(key) != null) {
                return LuaValue.valueOf(compound.getBoolean(key));
            } else if (compound.getInteger(key) != null) {
                return LuaValue.valueOf(compound.getInteger(key));
            } else if (compound.getFloat(key) != null) {
                return LuaValue.valueOf(compound.getFloat(key));
            } else if (compound.getString(key) != null) {
                return LuaValue.valueOf(compound.getString(key));
            } else if (compound.getList(key, v -> deserializeLuaValue(v, "index")) != null) {
                List<LuaValue> values = compound.getList(key, v -> deserializeLuaValue(v, "index"));
                return LuaTable.tableOf(values.toArray(new LuaValue[0]));
            }
        }
        return LuaValue.NIL;
    }

    public static class LuaMapper implements TagMapper<Map<String, LuaValue>> {

        @Override
        public TagAccessor<Map<String, LuaValue>> apply(Class<Map<String, LuaValue>> type, String fieldName, TagField tag) throws SerializationException {
            return new TagAccessor<>(
                    (d, o) -> d.setMap(fieldName, o, Function.identity(), v -> serializeLuaValue(new TagCompound(), "value", v)),
                    d -> d.getMap(fieldName, Function.identity(), v -> deserializeLuaValue(v, "value"))
            );
        }
    }
}
