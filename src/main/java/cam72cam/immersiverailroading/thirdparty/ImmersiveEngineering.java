package cam72cam.immersiverailroading.thirdparty;

import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.blocks.metal.BlockTypes_MetalDecoration0;
import blusunrize.immersiveengineering.common.blocks.stone.BlockTypes_StoneDecoration;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class ImmersiveEngineering {

	public static void init() {
		ItemStack casing = new ItemStack(IEContent.blockStoneDecoration,1, BlockTypes_StoneDecoration.BLASTBRICK.getMeta());
		ItemStack light_eng = new ItemStack(IEContent.blockMetalDecoration0,1, BlockTypes_MetalDecoration0.LIGHT_ENGINEERING.getMeta());
		ItemStack heavy_eng = new ItemStack(IEContent.blockMetalDecoration0,1, BlockTypes_MetalDecoration0.HEAVY_ENGINEERING.getMeta());
		OreDictionary.registerOre("irCastingCasing", casing);
		OreDictionary.registerOre("irLightEngineering", light_eng);
		OreDictionary.registerOre("irHeavyEngineering", heavy_eng);
	}
}
