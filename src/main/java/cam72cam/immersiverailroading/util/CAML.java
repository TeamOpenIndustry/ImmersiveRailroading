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
    private static final Pattern base = Pattern.compile("(\\s*)(\\S+)\\s*([=:])\\s?(\"[^\"]*\"|[^#]*)(#.*)?");
    public static DataBlock parse(InputStream stream) throws IOException {
        List<String> lines = IOUtils.readLines(stream, StandardCharsets.UTF_8).stream()
                .filter(s -> !StringUtils.isWhitespace(s.replaceFirst("#.*", "")))
                .collect(Collectors.toList());
        stream.close();

        return createBlock(lines, "");
    }

    private static DataBlock createBlock(List<String> lines, String context) throws ParseException {
        Map<String, DataBlock.Value> primitives = new LinkedHashMap<>();
        Map<String, List<DataBlock.Value>> primitiveSets = new LinkedHashMap<>();
        Map<String, DataBlock> blocks = new LinkedHashMap<>();
        Map<String, List<DataBlock>> blockSets = new LinkedHashMap<>();

        String spaces = null;
        while (!lines.isEmpty()) {
            String line = lines.get(0);
            Matcher m = base.matcher(line);
            if (!m.matches()) {
                throw new ParseException(String.format("Invalid Block line '%s'", line));
            }

            String pre = m.group(1);
            String key = m.group(2);
            String mod = m.group(3);
            String val = m.group(4);

            if (spaces == null) {
                spaces = pre;
            }

            if (!pre.startsWith(spaces) && !spaces.startsWith(pre)) {
                throw new ParseException(String.format("Invalid Block line '%s' mismatched indentation '%s' vs '%s'", line, spaces, pre));
            }

            if (spaces.length() > pre.length()) {
                // Reduced indentation
                break;
            }
            if (!spaces.equals(pre)) {
                throw new ParseException(String.format("Invalid Block line '%s' invalid indentation '%s' vs '%s'", line, spaces, pre));
            }

            lines.remove(0);

            String trimmed = val.trim();
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }

            if (StringUtils.isWhitespace(val)) {
                DataBlock block = createBlock(lines, String.format("%s > %s", context, key));
                if (mod.equals("=")) {
                    if (blocks.containsKey(key) || blockSets.containsKey(key)) {
                        throw new ParseException(String.format("Invalid line: '%s' can not be specified multiple times at %s", line, context));
                    }
                    blocks.put(key, block);
                } else {
                    if (blocks.containsKey(key)) {
                        throw new ParseException(String.format("Invalid line: '%s' can not be specified multiple times %s", line, context));
                    }
                    blockSets.computeIfAbsent(key, k -> new ArrayList<>()).add(block);
                }
            } else {
                if (mod.equals("=")) {
                    if (primitives.containsKey(key) || primitiveSets.containsKey(key)) {
                        throw new ParseException(String.format("Invalid line: '%s' can not be specified multiple times %s", line, context));
                    }
                    primitives.put(key, createValue(trimmed));
                } else {
                    if (primitives.containsKey(key)) {
                        throw new ParseException(String.format("Invalid line: '%s' can not be specified multiple times %s", line, context));
                    }
                    primitiveSets.computeIfAbsent(key, k -> new ArrayList<>()).add(createValue(trimmed));
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

            @Override
            public Value getValue(String key) {
                Value value = DataBlock.super.getValue(key);
                if (value.asString() == null && getValuesMap().containsKey(key)) {
                    throw new FormatException("Error in CAML file: expected single value '=' but found multiple ':' for key %s '%s'", context, key);
                }
                return value;
            }

            @Override
            public List<Value> getValues(String key) {
                List<Value> values = DataBlock.super.getValues(key);
                if (values == null && getValueMap().containsKey(key)) {
                    throw new FormatException("Error in CAML file: multiple values ':' but found single value '=' for key %s '%s'", context, key);
                }
                return values;
            }

            @Override
            public DataBlock getBlock(String key) {
                DataBlock block = DataBlock.super.getBlock(key);
                if (block == null && getBlocksMap().containsKey(key)) {
                    throw new FormatException("Error in CAML file: expected single block '=' but found multiple ':' for key %s '%s'", context, key);
                }
                return block;
            }

            @Override
            public List<DataBlock> getBlocks(String key) {
                List<DataBlock> blocks = DataBlock.super.getBlocks(key);
                if (blocks == null && getBlockMap().containsKey(key)) {
                    throw new FormatException("Error in CAML file: multiple blocks ':' but found single value '=' for key %s '%s'", context, key);
                }
                return blocks;
            }

        };
    }

    public static class ParseException extends RuntimeException {
        public ParseException(String text, Object... params) {
            super(String.format(text, params));
        }
    }

    public static class FormatException extends RuntimeException {
        public FormatException(String text, Object... params) {
            super(String.format(text, params));
        }
    }

    private static DataBlock.Value createValue(String value) {
        if (value == null || value.equalsIgnoreCase("null")) {
            return DataBlock.Value.NULL;
        }
        return new DataBlock.Value() {
            @Override
            public Boolean asBoolean() {
                                     return Boolean.parseBoolean(value);
                                                                                               }

            @Override
            public Integer asInteger() {
                                     return Integer.parseInt(value);
                                                                                           }

            @Override
            public Float asFloat() {
                                 return Float.parseFloat(value);
                                                                                       }

            @Override
            public Double asDouble() {
                                   return Double.parseDouble(value);
                                                                                           }

            @Override
            public String asString() {
                return value;
            }
        };
    }
}
