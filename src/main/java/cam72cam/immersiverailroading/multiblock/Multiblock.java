package cam72cam.immersiverailroading.multiblock;

import java.util.List;
import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public abstract class Multiblock {
	// z y x
	private final MultiblockComponent[][][] components;
	protected final List<BlockPos> componentPositions = new ArrayList<BlockPos>();
	
	protected static final MultiblockComponent AIR = new MultiblockComponent(Blocks.AIR);

	protected Multiblock(MultiblockComponent[][][] components) {
		this.components = components;
		for (int z = 0; z < components.length; z++) {
			MultiblockComponent[][] zcomp = components[z];
			for (int y = 0; y < components[z].length; y++) {
				MultiblockComponent[] ycomp = zcomp[y];
				for (int x = 0; x < ycomp.length; x++) {
					componentPositions.add(new BlockPos(x, y, z));
				}
			}
		}
	}
	
	protected MultiblockComponent lookup(BlockPos offset) {
		return components[offset.getZ()][offset.getY()][offset.getX()];
	}
	
	private boolean checkValid(IBlockAccess world, BlockPos origin, BlockPos offset, Rotation rot) {
		BlockPos pos = origin.add(offset.rotate(rot));
		MultiblockComponent component = lookup(offset);
		return component.valid(world, pos);
	}

	public abstract void onCreate(World world, BlockPos origin, Rotation rot);
	public abstract boolean onBlockActivated(World world, BlockPos origin, Rotation rotation, EntityPlayer player, EnumHand hand, BlockPos pos);
	public abstract void onBreak(World world, BlockPos origin, Rotation rot);
	
	public boolean tryCreate(World world, BlockPos pos) {
		for (BlockPos activationLocation : this.componentPositions) {
			for (Rotation rot : Rotation.values()) {
				BlockPos origin = pos.subtract(activationLocation.rotate(rot));
				boolean valid = true;
				for (BlockPos offset : this.componentPositions) {
					valid = valid && checkValid(world, origin, offset, rot);
				}
				if (valid) {
					System.out.println("VALID!!!!");
					onCreate(world, origin, rot);
					return true;
				}
			}
		}
		System.out.println("FAIL");
		return false;
	}
}
