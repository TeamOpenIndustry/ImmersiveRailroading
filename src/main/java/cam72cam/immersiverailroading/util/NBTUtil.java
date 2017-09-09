package cam72cam.immersiverailroading.util;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public class NBTUtil {
	//TODO move other nbt helpers here
	
	public static BlockPos nbtToBlockPos(NBTTagCompound nbt) {
		return new BlockPos(nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z"));
	}
	
	public static NBTTagCompound blockPosToNBT(BlockPos pos) {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger("x", pos.getX());
		nbt.setInteger("y", pos.getY());
		nbt.setInteger("z", pos.getZ());
		return nbt;
	}
}
