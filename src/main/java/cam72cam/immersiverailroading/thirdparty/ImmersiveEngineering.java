package cam72cam.immersiverailroading.thirdparty;

import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.blocks.metal.BlockTypes_MetalDecoration0;
import blusunrize.immersiveengineering.common.blocks.stone.BlockTypes_StoneDecoration;
import cam72cam.immersiverailroading.util.OreHelper;
import net.minecraft.item.ItemStack;

public class ImmersiveEngineering {

	public static void init() {
		ItemStack casing = new ItemStack(IEContent.blockStoneDecoration,1, BlockTypes_StoneDecoration.BLASTBRICK.getMeta());
		ItemStack light_eng = new ItemStack(IEContent.blockMetalDecoration0,1, BlockTypes_MetalDecoration0.LIGHT_ENGINEERING.getMeta());
		ItemStack heavy_eng = new ItemStack(IEContent.blockMetalDecoration0,1, BlockTypes_MetalDecoration0.HEAVY_ENGINEERING.getMeta());
		OreHelper.IR_CASTING_CASING.add(casing);
		OreHelper.IR_LIGHT_ENG.add(light_eng);
		OreHelper.IR_HEAVY_ENG.add(heavy_eng);
	}
}
