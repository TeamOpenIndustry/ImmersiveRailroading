package cam72cam.immersiverailroading.util;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RedstoneUtil {

	public static int getPower(World world, BlockPos pos) {
		int power = 0;
		for (EnumFacing facing : EnumFacing.VALUES) {
			power = Math.max(power, world.getRedstonePower(pos.offset(facing), facing));
		}
		return power;
	}

}
