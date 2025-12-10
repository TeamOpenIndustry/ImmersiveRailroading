package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.registry.ConsistDefinition;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.util.SpawnUtil;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;
import cam72cam.mod.text.PlayerMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsistPlacePacket extends Packet {
    @TagField(mapper = StockListMapper.class)
    public List<ConsistDefinition.Stock> stocks;

    @TagField
    public Vec3i clicking;

    @TagField(mapper = StringFloatMapMapper.class)
    public Map<String, Float> defaultCGs;

    public ConsistPlacePacket() {
    }

    public ConsistPlacePacket(ConsistDefinition def, Vec3i clicking) {
        this.stocks = def.getStocks();
        this.clicking = clicking;
        this.defaultCGs = def.getDefaultCGs();
    }

    @Override
    protected void handle() {
        if (stocks.stream()
                  .anyMatch(stock -> (stock.definition = DefinitionManager.getDefinition(stock.defID)) == null)
        ) {
            //Some stock don't exist on server
            getPlayer().sendMessage(PlayerMessage.translate(ChatText.STOCK_INVALID.toString()));
        } else {
            SpawnUtil.placeConsist(getPlayer(), getWorld(), clicking, stocks, defaultCGs == null ? new HashMap<>() : defaultCGs);
        }
    }

    public static class StockListMapper implements TagMapper<List<ConsistDefinition.Stock>> {
        @Override
        public TagAccessor<List<ConsistDefinition.Stock>> apply(Class<List<ConsistDefinition.Stock>> type, String fieldName, TagField tag) throws SerializationException {
            return new TagAccessor<>(
                    (compound, stocks) -> compound.setList("stocks", stocks, stock ->
                                                                   new TagCompound().setString("defID", stock.defID)
                                                                                    .setEnum("direction", stock.direction)
                                                                                    .setString("tex", stock.texture)),
                    (compound) -> compound.getList("stocks", compound1 -> {
                        ConsistDefinition.Stock stock = new ConsistDefinition.Stock();
                        stock.defID = compound1.getString("defID");
                        stock.direction = compound1.getEnum("direction", ConsistDefinition.Direction.class);
                        stock.texture = compound1.getString("tex");
                        return stock;
                    }));
        }
    }

    public static class StringFloatMapMapper implements TagMapper<Map<String, Float>> {
        @Override
        public TagAccessor<Map<String, Float>> apply(Class<Map<String, Float>> aClass, String s, TagField tagField) throws SerializationException {
            return new TagAccessor<>(
                    (compound, data) -> compound.setMap("default_cg", data,
                                                        k -> k,
                                                        v -> new TagCompound().setFloat("val", v)),
                    (compound) -> compound.getMap("default_cg",
                                                  k -> k,
                                                  v -> v.getFloat("val")));
        }
    }
}
