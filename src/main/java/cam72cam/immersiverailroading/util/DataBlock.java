package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.resource.Identifier;
import com.google.gson.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/** TODO multi dimensional arrays... */
@SuppressWarnings("unused")
public interface DataBlock {
    DataBlock getBlock(String key);
    List<DataBlock> getBlocks(String key);

    List<String> getSet(String key);

    Boolean getBoolean(String key);
    default boolean getBoolean(String key, boolean fallback) {
        Boolean val = getBoolean(key);
        return val != null ? val : fallback;
    }

    Integer getInteger(String key);
    default int getInteger(String key, int fallback) {
        Integer val = getInteger(key);
        return val != null ? val : fallback;
    }

    Float getFloat(String key);
    default float getFloat(String key, float fallback) {
        Float val = getFloat(key);
        return val != null ? val : fallback;
    }

    String getString(String key);
    default String getString(String key, String fallback) {
        String val = getString(key);
        return val != null ? val : fallback;
    }

    default Identifier getIdentifier(String key) {
        String value = getString(key);
        return value != null ? new Identifier(ImmersiveRailroading.MODID, new Identifier(value).getPath()) : null;
    }
    default Identifier getIdentifier(String key, Identifier fallback) {
        Identifier val = getIdentifier(key);
        return val != null ? val : fallback;
    }

    Collection<String> getPrimitiveKeys();
    Collection<String> getBlockKeys();
    Collection<String> getSetKeys();

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
    static DataBlock wrapJSON(JsonObject obj) {
        Map<String, JsonPrimitive> primitives = obj.entrySet().stream()
                .filter(e -> e.getValue().isJsonPrimitive())
                .collect(Collectors.toMap(Map.Entry::getKey, t -> t.getValue().getAsJsonPrimitive()));
        Map<String, List<DataBlock>> blocks = obj.entrySet().stream()
                .filter(e -> e.getValue().isJsonObject())
                .collect(Collectors.toMap(Map.Entry::getKey, t -> Collections.singletonList(wrapJSON(t.getValue().getAsJsonObject()))));
        Map<String, List<String>> sets = obj.entrySet().stream()
                .filter(e -> e.getValue().isJsonArray() && (e.getValue().getAsJsonArray().size() == 0 || e.getValue().getAsJsonArray().get(0).isJsonPrimitive()))
                .collect(Collectors.toMap(Map.Entry::getKey, t -> {
                    List<String> result = new ArrayList<>();
                    for (JsonElement elem : t.getValue().getAsJsonArray()) {
                        result.add(elem.getAsString());
                    }
                    return result;
                }));
        blocks.putAll(obj.entrySet().stream()
                .filter(e -> e.getValue().isJsonArray() && (e.getValue().getAsJsonArray().size() == 0 || e.getValue().getAsJsonArray().get(0).isJsonObject()))
                .collect(Collectors.toMap(Map.Entry::getKey, t -> {
                    List<DataBlock> result = new ArrayList<>();
                    for (JsonElement elem : t.getValue().getAsJsonArray()) {
                        result.add(wrapJSON(elem.getAsJsonObject()));
                    }
                    return result;
                })));

        return new DataBlock() {
            @Override
            public DataBlock getBlock(String key) {
                return blocks.containsKey(key) ? blocks.get(key).get(0) : null;
            }

            @Override
            public List<DataBlock> getBlocks(String key) {
                return blocks.get(key);
            }

            @Override
            public List<String> getSet(String key) {
                return sets.get(key);
            }

            @Override
            public Boolean getBoolean(String key) {
                return primitives.containsKey(key) ? primitives.get(key).getAsBoolean() : null;
            }

            @Override
            public Integer getInteger(String key) {
                return primitives.containsKey(key) ? primitives.get(key).getAsInt() : null;
            }

            @Override
            public Float getFloat(String key) {
                return primitives.containsKey(key) ? primitives.get(key).getAsFloat() : null;
            }

            @Override
            public String getString(String key) {
                return primitives.containsKey(key) ? primitives.get(key).getAsString() : null;
            }

            @Override
            public Collection<String> getPrimitiveKeys() {
                return primitives.keySet();
            }

            @Override
            public Collection<String> getBlockKeys() {
                return blocks.keySet();
            }

            @Override
            public Collection<String> getSetKeys() {
                return sets.keySet();
            }
        };
    }
}
