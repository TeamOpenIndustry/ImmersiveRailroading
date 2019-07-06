package cam72cam.mod.item;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraftforge.oredict.OreDictionary;

public class Fuzzy {
    public static final Fuzzy WOOD_PLANK = new Fuzzy("plankWood");
    public static final Fuzzy REDSTONE_DUST = new Fuzzy("dustRedstone");
    public static final Fuzzy SNOW_LAYER = new Fuzzy("layerSnow", Blocks.SNOW_LAYER);
    public static final Fuzzy SNOW_BLOCK = new Fuzzy("blockSnow", Blocks.SNOW);
    public static final Fuzzy LEAD = new Fuzzy("lead", Items.LEAD);

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
}
