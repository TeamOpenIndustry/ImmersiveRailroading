package cam72cam.mod.item;

import net.minecraftforge.oredict.OreDictionary;

public class Fuzzy {
    public static final Fuzzy WOOD_PLANK = new Fuzzy("plankWood");

    private final String ident;

    public Fuzzy(String ident) {
        this.ident = ident;
    }

    public boolean matches(ItemStack stack) {
        return OreDictionary.getOres(ident).stream().anyMatch((net.minecraft.item.ItemStack potential) -> OreDictionary.itemMatches(potential, stack.internal, false));
    }
}
