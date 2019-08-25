package cam72cam.mod.item;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Fuzzy {
    public static final Fuzzy WOOD_STICK = new Fuzzy("stickWood");
    public static final Fuzzy WOOD_PLANK = new Fuzzy("plankWood");
    public static final Fuzzy REDSTONE_DUST = new Fuzzy("dustRedstone");
    public static final Fuzzy SNOW_LAYER = new Fuzzy("layerSnow").add(Blocks.SNOW_LAYER);
    public static final Fuzzy SNOW_BLOCK = new Fuzzy("blockSnow").add(Blocks.SNOW);
    public static final Fuzzy LEAD = new Fuzzy("lead").add(Items.LEAD);

    public static final Fuzzy STONE_SLAB = new Fuzzy("slabStone").add(Blocks.STONE_SLAB);
    public static final Fuzzy STONE_BRICK = new Fuzzy("brickStone").add(Blocks.STONEBRICK);
    public static final Fuzzy SAND = new Fuzzy("sand");
    public static final Fuzzy PISTON = new Fuzzy("piston").add(Blocks.PISTON);

    public static final Fuzzy GOLD_INGOT = new Fuzzy("ingotGold");
    public static final Fuzzy STEEL_INGOT = new Fuzzy("ingotSteel");
    public static final Fuzzy IRON_INGOT = new Fuzzy("ingotIron");
    public static final Fuzzy IRON_BLOCK = new Fuzzy("blockIron");
    public static final Fuzzy IRON_BARS = new Fuzzy("barsIron").add(Blocks.IRON_BARS);

    public static final Fuzzy NETHER_BRICK = new Fuzzy("brickNether").add(Blocks.NETHER_BRICK);
    public static final Fuzzy GRAVEL_BLOCK = new Fuzzy("gravel");
    public static final Fuzzy BRICK_BLOCK = new Fuzzy("brickBlock");
    public static final Fuzzy COBBLESTONE = new Fuzzy("cobblestone");
    public static final Fuzzy CONCRETE = new Fuzzy("concrete").add(new ItemStack(Blocks.CONCRETE, 1, OreDictionary.WILDCARD_VALUE));
    public static final Fuzzy DIRT = new Fuzzy("dirt");
    public static final Fuzzy HARDENED_CLAY = new Fuzzy("hardened_clay").add(new ItemStack(Blocks.HARDENED_CLAY, 1, OreDictionary.WILDCARD_VALUE));
    public static final Fuzzy LOG_WOOD = new Fuzzy("logWood");
    public static final Fuzzy PAPER = new Fuzzy("paper");
    public static final Fuzzy BOOK = new Fuzzy("book").add(Items.BOOK);
    public static final Fuzzy WOOL_BLOCK = new Fuzzy("wool").add(new ItemStack(Blocks.WOOL, 1, OreDictionary.WILDCARD_VALUE));
    public static final Fuzzy BUCKET = new Fuzzy("bucket").add(new ItemStack(Items.BUCKET, 1));

    private final String ident;

    public Fuzzy(String ident) {
        this.ident = ident;
    }

    public boolean matches(ItemStack stack) {
        return OreDictionary.getOres(ident).stream().anyMatch((net.minecraft.item.ItemStack potential) -> OreDictionary.itemMatches(potential, stack.internal, false));
    }

    public List<ItemStack> enumerate() {
        List<ItemStack> results = new ArrayList<>();
        for (net.minecraft.item.ItemStack stack : OreDictionary.getOres(ident)) {
            if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                NonNullList<net.minecraft.item.ItemStack> temp = NonNullList.create();
                stack.getItem().getSubItems(stack.getItem().getCreativeTab(), temp);
                results.addAll(temp.stream().map(ItemStack::new).collect(Collectors.toList()));
            } else {
                results.add(new ItemStack(stack));
            }
        }
        return results;
    }

    public ItemStack example() {
        List<ItemStack> stacks = enumerate();
        return stacks.size() != 0 ? stacks.get(0) : ItemStack.EMPTY;
    }

    public Fuzzy add(ItemStack item) {
        OreDictionary.registerOre(ident, item.internal);
        return this;
    }
    public Fuzzy add(Block block) {
        OreDictionary.registerOre(ident, block);
        return this;
    }
    public Fuzzy add(Item item) {
        OreDictionary.registerOre(ident, item);
        return this;
    }

    public Fuzzy addAll(Fuzzy other) {
        return addAll(other.ident);
    }

    public Fuzzy addAll(String other) {
        OreDictionary.getOres(other).stream().map(ItemStack::new).forEach(this::add);
        return this;
    }

    public void clear() {
        // This might break stuff in fantastic ways!
        OreDictionary.getOres(ident).clear();
    }

    @Override
    public String toString() {
        return ident;
    }
}
