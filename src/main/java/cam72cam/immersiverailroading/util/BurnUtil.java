package cam72cam.immersiverailroading.util;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;

public class BurnUtil {
	public static int getBurnTime(ItemStack stack) {
		return TileEntityFurnace.getItemBurnTime(stack);
	}
}
