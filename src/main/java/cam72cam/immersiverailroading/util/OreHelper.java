package cam72cam.immersiverailroading.util;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;

public class OreHelper {
	public static boolean OreDictionaryContainsMatch(boolean strict, NonNullList<ItemStack> ores, ItemStack playerStack) {
        for (ItemStack target : ores)
        {
            if (OreDictionary.itemMatches(target, playerStack, strict))
            {
                return true;
            }
        }
        return false;
	}

	public static boolean matches(ItemStack stack, String oreName, boolean strict) {
		return OreDictionaryContainsMatch(strict, OreDictionary.getOres(oreName), stack);
	}
	
	public static ItemStack exampleStack(String name) {
		return OreDictionary.getOres(name).get(0);
	}
	
	public static void init() {
		if (!OreDictionary.doesOreNameExist("irCastingCasing")) {
			OreDictionary.registerOre("irCastingCasing", new ItemStack(Items.NETHERBRICK));
		}
		if (!OreDictionary.doesOreNameExist("irLightEngineering")) {
			OreDictionary.registerOre("irLightEngineering", new ItemStack(Blocks.IRON_BLOCK));
		}
		if (!OreDictionary.doesOreNameExist("irHeavyEngineering")) {
			OreDictionary.registerOre("irHeavyEngineering", new ItemStack(Blocks.IRON_BLOCK));
		}
		if (!OreDictionary.doesOreNameExist("scaffoldingSteel")) {
			OreDictionary.registerOre("irLightEngineering", new ItemStack(Blocks.IRON_BARS));
		}
		if (!OreDictionary.doesOreNameExist("blockSteel")) {
			OreDictionary.registerOre("blockSteel", new ItemStack(Blocks.IRON_BLOCK));
		}
	}
}
