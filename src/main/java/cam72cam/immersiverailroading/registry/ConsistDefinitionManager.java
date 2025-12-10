package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.util.MinecraftFiles;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

//Client-sided
public class ConsistDefinitionManager {
    private static final Map<String, ConsistDefinition> display;

    private static final Map<String, ConsistDefinition> playerMadeConsist;
    private static final Map<String, ConsistDefinition> unmodifiableConsist;
    private static final File save;

    static {
        save = new File(MinecraftFiles.getConfigDir(), "immersiverailroading_consist.cfg");
        if (!save.exists()) {
            try {
                save.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        playerMadeConsist = new TreeMap<>(String::compareToIgnoreCase);
        display = new TreeMap<>(String::compareToIgnoreCase);
        unmodifiableConsist = new HashMap<>();
    }

    public static void save() {
        try {
            // Structure:
            // multi_unit [name]
            // stock [stockName] [direction] [text variant] TODO optional: [cg:value]
            List<String> list = new LinkedList<>();
            list.add("//DO NOT TOUCH UNLESS YOU KNOW WHAT ARE YOU DOING");
            for (Map.Entry<String, ConsistDefinition> entry : playerMadeConsist.entrySet()) {
                list.add("consist "+entry.getKey().replace(" ", "^"));
                for (ConsistDefinition.Stock stock : entry.getValue().getStocks()) {
                    String texture = (stock.texture == null||stock.texture.isEmpty()) ? "default" : stock.texture;
                    list.add("stock "+stock.defID+" "+stock.direction+" "+texture);
                }
            }
            Files.write(save.toPath(), list);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void load() {
        try {
            // Structure:
            // multi_unit [name]
            // stock [stockName] [direction] [text variant] TODO optional: [cg:value]
            List<String> lines = Files.readAllLines(save.toPath());
            playerMadeConsist.clear();
            display.clear();
            ConsistDefinition.ConsistDefBuilder builder = null;
            for (String line : lines) {
                if (line.startsWith("consist")) {
                    String[] split = line.split(" ");
                    if(builder != null) {
                        ConsistDefinition built = builder.build();
                        playerMadeConsist.put(built.getName(), built);
                        if (built.valid()) {
                            display.put(built.getName(), built);
                        }
                    }
                    builder = ConsistDefinition.ConsistDefBuilder.of(split[1].replace("^", " "));
                } else if (line.startsWith("stock")) {
                    String[] split = line.split(" ");
                    if (builder != null) {
                        builder.appendStock(split[1], ConsistDefinition.Direction.valueOf(split[2]), split[3], Collections.emptyMap());
                    }
                }
            }
            if(builder != null) {
                ConsistDefinition built = builder.build();
                playerMadeConsist.put(built.getName(), built);
                if (built.valid()) {
                    display.put(built.getName(), built);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        display.putAll(unmodifiableConsist);
    }

    public static void loadUnmodifiable(DataBlock block) {
        String name = block.getValue("name").asString();
        ConsistDefinition.ConsistDefBuilder builder = ConsistDefinition.ConsistDefBuilder.of(name);
        block.getBlock("default_CG").getValueMap().forEach((s, val) ->
                                                                   builder.addDefaultCG(s, val.asFloat()));
        block.getValues("add_tooltip").stream().map(DataBlock.Value::asString).collect(Collectors.toList()); //TODO
        block.getBlocks("consist").stream()
             .map(b -> {
                 ConsistDefinition.Stock stock = new ConsistDefinition.Stock();
                stock.defID = b.getValue("stock").asString();
                stock.texture = b.getValue("stock").asString();
                stock.direction = ConsistDefinition.Direction.of(b.getValue("direction").asString());
                return stock;
             })
             .forEach(builder::appendStock);
        builder.setEditable(false);
        unmodifiableConsist.put(name, builder.build());
    }

    public static void addConsist(ConsistDefinition build) {
        playerMadeConsist.put(build.getName(), build);
        save();
        load();
    }

    public static void removeConsist(ConsistDefinition current) {
        playerMadeConsist.remove(current.getName(), current);
        save();
        load();
    }

    public static Map<String, ConsistDefinition> getValidConsists() {
        return display;
    }

    public static ConsistDefinition getConsistDefinition(String name) {
        if (name == null) {
            return null;
        }
        return playerMadeConsist.get(name);
    }
}
