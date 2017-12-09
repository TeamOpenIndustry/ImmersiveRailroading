package cam72cam.immersiverailroading.multiblock;

import java.util.function.BiFunction;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class MultiblockComponent {
	private final BiFunction<IBlockAccess, BlockPos, Boolean> fn;
	
	public MultiblockComponent() {
		this.fn = (IBlockAccess world, BlockPos pos) -> true;
	}
	
	public MultiblockComponent(Block block) {
		this.fn = (IBlockAccess world, BlockPos pos) -> {
			return world.getBlockState(pos).getBlock() == block;
		};
	}
	
	public MultiblockComponent(BiFunction<IBlockAccess, BlockPos, Boolean> fn) {
		this.fn = fn;
	}
	
	public boolean valid(IBlockAccess world, BlockPos pos) {
		return fn.apply(world, pos);
	}
}
