package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.resource.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unused")
public interface DataBlock {
    Map<String, Value> getValueMap();
    Map<String, List<Value>> getValuesMap();
    Map<String, DataBlock> getBlockMap();
    Map<String, List<DataBlock>> getBlocksMap();

    default DataBlock getBlock(String key) {
        return getBlockMap().get(key);
    }
    default List<DataBlock> getBlocks(String key) {
        return getBlocksMap().get(key);
    }
    default Value getValue(String key) {
        return getValueMap().getOrDefault(key, Value.NULL);
    }
    default List<Value> getValues(String key) {
        return getValuesMap().get(key);
    }

    interface Value {
        Boolean asBoolean();
        default boolean asBoolean(boolean fallback) {
            Boolean val = asBoolean();
            return val != null ? val : fallback;
        }

        Integer asInteger();
        default int asInteger(int fallback) {
            Integer val = asInteger();
            return val != null ? val : fallback;
        }

        Float asFloat();
        default float asFloat(float fallback) {
            Float val = asFloat();
            return val != null ? val : fallback;
        }

        Double asDouble();
        default double asDouble(double fallback) {
            Double val = asDouble();
            return val != null ? val : fallback;
        }

        String asString();
        default String asString(String fallback) {
            String val = asString();
            return val != null ? val : fallback;
        }

        default Identifier asIdentifier() {
            String value = asString();
            return value != null ? new Identifier(ImmersiveRailroading.MODID, new Identifier(value).getPath()) : null;
        }
        default Identifier asIdentifier(Identifier fallback) {
            Identifier val = asIdentifier();
            return val != null ? val : fallback;
        }

        Value NULL = new Value() {
            @Override
            public Boolean asBoolean() {
                return null;
            }

            @Override
            public Integer asInteger() {
                return null;
            }

            @Override
            public Float asFloat() {
                return null;
            }

            @Override
            public Double asDouble() {
                return null;
            }

            @Override
            public String asString() {
                return null;
            }
        };
    }


    static DataBlock load(Identifier ident) throws IOException {
        return load(ident, false);
    }

    static DataBlock load(Identifier ident, boolean last) throws IOException {
        InputStream stream = last ? ident.getLastResourceStream() : ident.getResourceStream();
        if (ident.getPath().toLowerCase(Locale.ROOT).endsWith(".caml")) {
            return CAML.parse(stream);
        }
        if (!ident.getPath().toLowerCase(Locale.ROOT).endsWith(".json")) {
            ImmersiveRailroading.warn("Unexpected file extension '%s', trying JSON...", ident.toString());
        }
        return JSON.parse(stream);
    }
}
