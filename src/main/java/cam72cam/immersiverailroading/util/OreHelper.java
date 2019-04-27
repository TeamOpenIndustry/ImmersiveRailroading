package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.mod.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;
import java.util.stream.Collectors;

public class OreHelper {
	public static class OreAbstract {
		private ItemStack fallback;
		public final String def;
		public final String name;
		
		public OreAbstract(String name, ItemStack fallback) {
			this.name = name;
			this.def = null;
			this.fallback = fallback;
		}

		public OreAbstract(String name, String def, ItemStack fallback) {
			this.name = name;
			this.def = def;
			this.fallback = fallback;
		}
		
		public List<ItemStack> getOres() {
			List<ItemStack> ores = OreDictionary.getOres(name).stream().map(ItemStack::new).collect(Collectors.toList());
			// Someone added stuff to the IR dict
			if (ores.size() != 0) {
				return ores;
			}
			
			// Fallback/merge
			if (def != null) {
				ores = OreDictionary.getOres(def).stream().map(ItemStack::new).collect(Collectors.toList());
				if (ores.size() != 0) {
					return ores;
				}
			}
			
			ores = NonNullList.create();
			
			ores.add(fallback);
			
			return ores;
		}
		
		public void add(ItemStack stack) {
			OreDictionary.registerOre(name, stack.internal);
		}

		public void add(Block block) {
			OreDictionary.registerOre(name, block);
		}
		
		public void add(Item item) {
			OreDictionary.registerOre(name, item);
		}
		
		public boolean matches(ItemStack stack, boolean strict) {
			return oreDictionaryContainsMatch(strict, getOres(), stack);
		}
		
		public ItemStack example() {
			return getOres().get(0);
		}
	}
	
	public static final OreAbstract IR_CASTING_CASING = new OreAbstract("irCastingCasing", new ItemStack(Blocks.NETHER_BRICK));
	public static final OreAbstract IR_LIGHT_ENG = new OreAbstract("irLightEngineering", new ItemStack(Blocks.IRON_BLOCK));
	public static final OreAbstract IR_HEAVY_ENG = new OreAbstract("irHeavyEngineering", new ItemStack(Blocks.IRON_BLOCK));
	public static final OreAbstract IR_SCAFFOLDING = new OreAbstract("irScaffolding", "scaffoldingSteel", new ItemStack(Blocks.IRON_BARS));
	public static final OreAbstract IR_STEEL_BLOCK = new OreAbstract("blockSteel", new ItemStack(Blocks.IRON_BLOCK));
	public static final OreAbstract IR_STEEL_INGOT = new OreAbstract("ingotSteel", new ItemStack(Items.IRON_INGOT));
	public static final OreAbstract IR_TIE = new OreAbstract("irTie", "plankTreatedWood", new ItemStack(Blocks.PLANKS, 1, OreDictionary.WILDCARD_VALUE));
	public static final OreAbstract IR_RAIL = new OreAbstract("irRail", new ItemStack(IRItems.ITEM_RAIL));
	public static final OreAbstract IR_RAIL_BED = new OreAbstract("railBed", new ItemStack(Blocks.GRAVEL));

	
	public static boolean oreDictionaryContainsMatch(boolean strict, List<ItemStack> ores, ItemStack playerStack) {
        for (ItemStack target : ores)
        {
            if (OreDictionary.itemMatches(target.internal, playerStack.internal, strict))
            {
                return true;
            }
        }
        return false;
	}
}
