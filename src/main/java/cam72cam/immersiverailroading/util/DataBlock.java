package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.resource.Identifier;
import com.google.gson.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

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
        return getValueMap().getOrDefault(key, new Value() {
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
        });
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
    }




    static DataBlock load(Identifier ident) throws IOException {
        return load(ident, false);
    }

    static DataBlock load(Identifier ident, boolean last) throws IOException {
        InputStream stream = last ? ident.getLastResourceStream() : ident.getResourceStream();
        if (ident.getPath().toLowerCase(Locale.ROOT).endsWith(".caml")) {
            return parseCAML(stream);
        }
        if (!ident.getPath().toLowerCase(Locale.ROOT).endsWith(".json")) {
            ImmersiveRailroading.warn("Unexpected file extension '%s', trying JSON...", ident.toString());
        }
        return parseJSON(stream);
    }
    static DataBlock parseCAML(InputStream stream) throws IOException {
        DataBlock parse = CAML.parse(stream);
        stream.close();
        return parse;
    }
    static DataBlock parseJSON(InputStream stream) throws IOException {
        DataBlock parse = wrapJSON(new JsonParser().parse(new InputStreamReader(stream)).getAsJsonObject());
        stream.close();
        return parse;
    }

    static Value wrapJSON(JsonPrimitive primitive) {
        return new Value() {
            @Override
            public Boolean asBoolean() {
                return primitive == null ? null : primitive.getAsBoolean();
            }

            @Override
            public Integer asInteger() {
                return primitive == null ? null : primitive.getAsInt();
            }

            @Override
            public Float asFloat() {
                return primitive == null ? null : primitive.getAsFloat();
            }

            @Override
            public Double asDouble() {
                return primitive == null ? null : primitive.getAsDouble();
            }

            @Override
            public String asString() {
                return primitive == null ? null : primitive.getAsString();
            }
        };
    }
    static DataBlock wrapJSON(JsonObject obj) {
        Map<String, Value> primitives = new LinkedHashMap<>();
        Map<String, DataBlock> blocks = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                primitives.put(entry.getKey(), wrapJSON(entry.getValue().getAsJsonPrimitive()));
            }
            if (entry.getValue().isJsonObject()) {
                blocks.put(entry.getKey(), wrapJSON(entry.getValue().getAsJsonObject()));
            }
        }
        Map<String, List<Value>> primitiveSets = new LinkedHashMap<>();
        Map<String, List<DataBlock>> blockSets = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            if (entry.getValue().isJsonArray()) {
                for (JsonElement element : entry.getValue().getAsJsonArray()) {
                    if (element.isJsonPrimitive()) {
                        primitiveSets.computeIfAbsent(entry.getKey(), key -> new ArrayList<>()).add(wrapJSON(element.getAsJsonPrimitive()));
                    }
                    if (element.isJsonObject()) {
                        blockSets.computeIfAbsent(entry.getKey(), key -> new ArrayList<>()).add(wrapJSON(element.getAsJsonObject()));
                    }
                    // TODO Nested Arrays are Ignored for not
                }
            }
        }

        return new DataBlock() {
            @Override
            public Map<String, Value> getValueMap() {
                return primitives;
            }

            @Override
            public Map<String, List<Value>> getValuesMap() {
                return primitiveSets;
            }

            @Override
            public Map<String, DataBlock> getBlockMap() {
                return blocks;
            }

            @Override
            public Map<String, List<DataBlock>> getBlocksMap() {
                return blockSets;
            }

        };
    }
}
