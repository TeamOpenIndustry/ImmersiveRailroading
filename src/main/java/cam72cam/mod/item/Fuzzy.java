package cam72cam.mod.item;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;
import java.util.stream.Collectors;

public class Fuzzy {
    public static final Fuzzy WOOD_PLANK = new Fuzzy("plankWood");
    public static final Fuzzy REDSTONE_DUST = new Fuzzy("dustRedstone");
    public static final Fuzzy SNOW_LAYER = new Fuzzy("layerSnow", Blocks.SNOW_LAYER);
    public static final Fuzzy SNOW_BLOCK = new Fuzzy("blockSnow", Blocks.SNOW);
    public static final Fuzzy LEAD = new Fuzzy("lead", Items.LEAD);
    public static final Fuzzy STONE_SLAB = new Fuzzy("slabStone", Blocks.STONE_SLAB);
    public static final Fuzzy STONE_BRICK = new Fuzzy("brickStone", Blocks.STONEBRICK);
    public static final Fuzzy SAND = new Fuzzy("sand");
    public static final Fuzzy PISTON = new Fuzzy("piston", Blocks.PISTON);

    private final String ident;

    public Fuzzy(String ident) {
        this.ident = ident;
    }

    public Fuzzy(String ident, Block block) {
        this(ident);
        OreDictionary.registerOre(ident, block);
    }

    public Fuzzy(String ident, Item item) {
        this(ident);
        OreDictionary.registerOre(ident, item);
    }

    public boolean matches(ItemStack stack) {
        return OreDictionary.getOres(ident).stream().anyMatch((net.minecraft.item.ItemStack potential) -> OreDictionary.itemMatches(potential, stack.internal, false));
    }

    public List<ItemStack> enumerate() {
        return OreDictionary.getOres(ident).stream().map(ItemStack::new).collect(Collectors.toList());
    }

    public ItemStack example() {
        List<ItemStack> stacks = enumerate();
        return stacks.size() != 0 ? stacks.get(0) : ItemStack.EMPTY;
    }
}
