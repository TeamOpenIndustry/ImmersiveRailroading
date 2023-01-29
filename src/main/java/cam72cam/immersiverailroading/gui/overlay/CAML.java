package cam72cam.immersiverailroading.gui.overlay;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Cam's Awesome Markup Language */
public class CAML {

    public static void main(String[] args) throws IOException {
        Block root = parse(Files.newInputStream(Paths.get("/home/cmesh/Games/Minecraft/ImmersiveRailroading/github/ImmersiveRailroading/src/main/resources/assets/immersiverailroading/gui/default/steam.caml")));
        System.out.println(root);
    }

    private static final Pattern base = Pattern.compile("(\\s*)(\\S+)\\s*([=:])\\s?(.*)");
    public static Block parse(InputStream stream) throws IOException {
        List<String> lines = IOUtils.readLines(stream, StandardCharsets.UTF_8).stream()
                .map(s -> s.replaceFirst("#.*", ""))
                .filter(s -> !StringUtils.isWhitespace(s))
                .collect(Collectors.toList());

        return new Block(lines);
    }

    public static class Block implements DataBlock {
        private final Map<String, String> values = new HashMap<>();
        private final Map<String, List<Block>> blocks = new HashMap<>();
        private final Map<String, List<String>> sets = new HashMap<>();

        private Block(List<String> lines) throws IOException {
            int spaces = -1;
            while (!lines.isEmpty()) {
                String line = lines.get(0);
                Matcher m = base.matcher(line);
                if (!m.matches()) {
                    throw new IOException(String.format("Invalid Block line '%s'", line));
                }

                int pre = m.group(1).length();
                String key = m.group(2);
                String mod = m.group(3);
                String val = m.group(4);

                if (spaces == -1) {
                    spaces = pre;
                }

                if (spaces > pre) {
                    // Reduced indentation
                    return;
                }
                if (spaces < pre) {
                    throw new IOException(String.format("Invalid Block line '%s' mismatched spaces %s vs %s", line, spaces, pre));
                }

                lines.remove(0);

                switch (mod) {
                    case "=":
                        values.put(key, val);
                        break;
                    case ":":
                        if (StringUtils.isWhitespace(val)) {
                            blocks.computeIfAbsent(key, k -> new ArrayList<>()).add(new Block(lines));
                        } else {
                            sets.computeIfAbsent(key, k -> new ArrayList<>()).add(val);
                        }
                        break;
                    default:
                        throw new IOException(String.format("Invalid Block line '%s'", line));
                }
            }
        }

        @Override
        public Block getBlock(String key) {
            return blocks.containsKey(key) ? blocks.get(key).get(0) : null;
        }

        @Override
        public List<Block> getBlocks(String key) {
            return blocks.get(key);
        }

        @Override
        public Boolean getBoolean(String key) {
            return values.containsKey(key) ? Boolean.parseBoolean(values.get(key)) : null;
        }

        @Override
        public Float getFloat(String key) {
            return values.containsKey(key) ? Float.parseFloat(values.get(key)) : null;
        }

        @Override
        public String getString(String key) {
            return values.get(key);
        }

        @Override
        public List<String> getSet(String key) {
            return sets.get(key);
        }

        @Override
        public Set<String> getPrimitiveKeys() {
            return values.keySet();
        }

        @Override
        public Collection<String> getBlockKeys() {
            return blocks.keySet();
        }

        @Override
        public Collection<String> getSetKeys() {
            return sets.keySet();
        }
    }
}
