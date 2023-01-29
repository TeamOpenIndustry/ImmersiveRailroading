package cam72cam.immersiverailroading.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*
 * CAML =
 *      # Cam's Awesome Markup Language V1
 *      description: CAML is an incredibly simple markup language, with a very small rule set.
 *      description: It is an indentation based format, which allows both tabs and spaces, as long as they are consistent.
 *      formatting =
 *          comment = "Any character that follows a # is ignored, unless it is a contained in a quoted string"
 *          whitespace = Any combination of spaces and tabs
 *          key =
 *              # An identifier made up of any non-whitespace character followed by a '=' or ':'
 *              set: When a key is followed by ':' there can be multiple keys with that name which are grouped in a collection
 *              single = When a key is followed by '=' there can only be a single key with that name.
 *          string = Any sequence of characters that is optionally wrapped in "quotation marks"
 *      types =
 *          # There are two types in CAML
 *          blocks =
 *              description = A block is a named container
 *              format = A block is started with a key at the current indentation level
 *              contents = Any subsequent lines at a deeper indentation level are read and then added to the block as their respective types.
 *
 *          primitive = A key followed by a value
 *
 * */
public class CAML {
    private static final Pattern base = Pattern.compile("(\\s*)(\\S+)\\s*([=:])\\s?(.*)");
    public static DataBlock parse(InputStream stream) throws IOException {
        List<String> lines = IOUtils.readLines(stream, StandardCharsets.UTF_8).stream()
                .map(s -> s.replaceFirst("#.*", ""))
                .filter(s -> !StringUtils.isWhitespace(s))
                .collect(Collectors.toList());
        stream.close();

        return createBlock(lines);
    }

    private static DataBlock createBlock(List<String> lines) throws IOException {
        Map<String, DataBlock.Value> primitives = new LinkedHashMap<>();
        Map<String, List<DataBlock.Value>> primitiveSets = new LinkedHashMap<>();
        Map<String, DataBlock> blocks = new LinkedHashMap<>();
        Map<String, List<DataBlock>> blockSets = new LinkedHashMap<>();

        String spaces = null;
        while (!lines.isEmpty()) {
            String line = lines.get(0);
            Matcher m = base.matcher(line);
            if (!m.matches()) {
                throw new IOException(String.format("Invalid Block line '%s'", line));
            }

            String pre = m.group(1);
            String key = m.group(2);
            String mod = m.group(3);
            String val = m.group(4);

            if (spaces == null) {
                spaces = pre;
            }

            if (!pre.startsWith(spaces) && !spaces.startsWith(pre)) {
                throw new IOException(String.format("Invalid Block line '%s' mismatched indentation '%s' vs '%s'", line, spaces, pre));
            }

            if (spaces.length() > pre.length()) {
                // Reduced indentation
                break;
            }
            if (!spaces.equals(pre)) {
                throw new IOException(String.format("Invalid Block line '%s' invalid indentation '%s' vs '%s'", line, spaces, pre));
            }

            lines.remove(0);

            String trimmed = val.trim();
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                val = val.substring(1, val.length() - 1);
            }

            if (StringUtils.isWhitespace(val)) {
                DataBlock block = createBlock(lines);
                if (mod.equals("=")) {
                    if (blocks.containsKey(key) || blockSets.containsKey(key)) {
                        throw new IOException(String.format("Invalid line: '%s' can not be specified multiple times", line));
                    }
                    blocks.put(key, block);
                } else {
                    if (blocks.containsKey(key)) {
                        throw new IOException(String.format("Invalid line: '%s' can not be specified multiple times", line));
                    }
                    blockSets.computeIfAbsent(key, k -> new ArrayList<>()).add(block);
                }
            } else {
                if (mod.equals("=")) {
                    if (primitives.containsKey(key) || primitiveSets.containsKey(key)) {
                        throw new IOException(String.format("Invalid line: '%s' can not be specified multiple times", line));
                    }
                    primitives.put(key, createValue(val));
                } else {
                    if (primitives.containsKey(key)) {
                        throw new IOException(String.format("Invalid line: '%s' can not be specified multiple times", line));
                    }
                    primitiveSets.computeIfAbsent(key, k -> new ArrayList<>()).add(createValue(val));
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

    private static DataBlock.Value createValue(String value) {
            return new DataBlock.Value() {
                @Override
                public Boolean asBoolean() {
                    return value == null ? null : Boolean.parseBoolean(value);
                }

                @Override
                public Integer asInteger() {
                    return value == null ? null : Integer.parseInt(value);
                }

                @Override
                public Float asFloat() {
                    return value == null ? null : Float.parseFloat(value);
                }

                @Override
                public Double asDouble() {
                    return value == null ? null : Double.parseDouble(value);
                }

                @Override
                public String asString() {
                    return value;
                }
            };
    }
}
