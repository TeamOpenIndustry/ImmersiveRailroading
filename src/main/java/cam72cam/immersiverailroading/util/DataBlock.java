package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.resource.Identifier;
import com.google.gson.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

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
            public Boolean getBoolean() {
                return null;
            }

            @Override
            public Integer getInteger() {
                return null;
            }

            @Override
            public Float getFloat() {
                return null;
            }

            @Override
            public Double getDouble() {
                return null;
            }

            @Override
            public String getString() {
                return null;
            }
        });
    }

    default List<Value> getValues(String key) {
        return getValuesMap().get(key);
    }

    interface Value {
        Boolean getBoolean();
        default boolean getBoolean(boolean fallback) {
            Boolean val = getBoolean();
            return val != null ? val : fallback;
        }

        Integer getInteger();
        default int getInteger(int fallback) {
            Integer val = getInteger();
            return val != null ? val : fallback;
        }

        Float getFloat();
        default float getFloat(float fallback) {
            Float val = getFloat();
            return val != null ? val : fallback;
        }

        Double getDouble();
        default double getDouble(double fallback) {
            Double val = getDouble();
            return val != null ? val : fallback;
        }

        String getString();
        default String getString(String fallback) {
            String val = getString();
            return val != null ? val : fallback;
        }

        default Identifier getIdentifier() {
            String value = getString();
            return value != null ? new Identifier(ImmersiveRailroading.MODID, new Identifier(value).getPath()) : null;
        }
        default Identifier getIdentifier(Identifier fallback) {
            Identifier val = getIdentifier();
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
            public Boolean getBoolean() {
                return primitive == null ? null : primitive.getAsBoolean();
            }

            @Override
            public Integer getInteger() {
                return primitive == null ? null : primitive.getAsInt();
            }

            @Override
            public Float getFloat() {
                return primitive == null ? null : primitive.getAsFloat();
            }

            @Override
            public Double getDouble() {
                return primitive == null ? null : primitive.getAsDouble();
            }

            @Override
            public String getString() {
                return primitive == null ? null : primitive.getAsString();
            }
        };
    }
    static DataBlock wrapJSON(JsonObject obj) {
        Map<String, Value> primitives = obj.entrySet().stream()
                .filter(e -> e.getValue().isJsonPrimitive())
                .collect(Collectors.toMap(Map.Entry::getKey, t -> wrapJSON(t.getValue().getAsJsonPrimitive())));
        Map<String, DataBlock> blocks = obj.entrySet().stream()
                .filter(e -> e.getValue().isJsonObject())
                .collect(Collectors.toMap(Map.Entry::getKey, t -> wrapJSON(t.getValue().getAsJsonObject())));
        Map<String, List<Value>> primitiveSets = obj.entrySet().stream()
                .filter(e -> e.getValue().isJsonArray() && (e.getValue().getAsJsonArray().size() == 0 || e.getValue().getAsJsonArray().get(0).isJsonPrimitive()))
                .collect(Collectors.toMap(Map.Entry::getKey, t -> {
                    List<Value> result = new ArrayList<>();
                    for (JsonElement elem : t.getValue().getAsJsonArray()) {
                        result.add(wrapJSON(elem.getAsJsonPrimitive()));
                    }
                    return result;
                }));
        Map<String, List<DataBlock>> blockSets = obj.entrySet().stream()
                .filter(e -> e.getValue().isJsonArray() && (e.getValue().getAsJsonArray().size() == 0 || e.getValue().getAsJsonArray().get(0).isJsonObject()))
                .collect(Collectors.toMap(Map.Entry::getKey, t -> {
                    List<DataBlock> result = new ArrayList<>();
                    for (JsonElement elem : t.getValue().getAsJsonArray()) {
                        result.add(wrapJSON(elem.getAsJsonObject()));
                    }
                    return result;
                }));

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
