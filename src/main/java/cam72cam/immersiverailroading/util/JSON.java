package cam72cam.immersiverailroading.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Helper class to turn JSON into DataBlocks */
public class JSON {
    public static DataBlock parse(InputStream stream) throws IOException {
        return wrapObject(new JsonParser().parse(new InputStreamReader(stream)).getAsJsonObject());
    }

    private static DataBlock wrapObject(JsonObject obj) {
        Map<String, DataBlock.Value> primitives = new LinkedHashMap<>();
        Map<String, DataBlock> blocks = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                primitives.put(entry.getKey(), wrapValue(entry.getValue().getAsJsonPrimitive()));
            }
            if (entry.getValue().isJsonObject()) {
                blocks.put(entry.getKey(), wrapObject(entry.getValue().getAsJsonObject()));
            }
        }
        Map<String, List<DataBlock.Value>> primitiveSets = new LinkedHashMap<>();
        Map<String, List<DataBlock>> blockSets = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            if (entry.getValue().isJsonArray()) {
                for (JsonElement element : entry.getValue().getAsJsonArray()) {
                    if (element.isJsonPrimitive()) {
                        primitiveSets.computeIfAbsent(entry.getKey(), key -> new ArrayList<>()).add(wrapValue(element.getAsJsonPrimitive()));
                    }
                    if (element.isJsonObject()) {
                        blockSets.computeIfAbsent(entry.getKey(), key -> new ArrayList<>()).add(wrapObject(element.getAsJsonObject()));
                    }
                    // TODO Nested Arrays are Ignored for now
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

    private static DataBlock.Value wrapValue(JsonPrimitive primitive) {
        return new DataBlock.Value() {
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
}
