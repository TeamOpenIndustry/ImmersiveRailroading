package cam72cam.immersiverailroading.multiblock;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SteamHammerMultiblock extends Multiblock {
	private static MultiblockComponent steel = new MultiblockComponent(Blocks.IRON_BLOCK);
	private static MultiblockComponent piston = new MultiblockComponent(Blocks.PISTON);
	private static BlockPos main = new BlockPos(2, 0, 0);
	public static final String NAME = "STEAM_HAMMER";

	public SteamHammerMultiblock() {
		super(new MultiblockComponent[][][] { // Z
			{ // Y
				{ //X
					steel, AIR, steel, AIR, steel
				},
				{
					steel, AIR, AIR, AIR, steel
				},
				{
					steel, steel, steel, steel, steel
				},
				{
					AIR, steel, steel, steel, AIR
				},
				{
					AIR, AIR, piston, AIR, AIR
				},
				{
					AIR, AIR, steel, AIR, AIR
				}
			}
		});
	}


	@Override
	public void onCreate(World world, BlockPos origin, Rotation rot) {
		BlockPos posMain = origin.add(main);
		for (BlockPos offset : componentPositions) {
			MultiblockComponent comp = lookup(offset);
			if (comp == AIR) {
				continue;
			}
			
			BlockPos pos = origin.add(offset.rotate(rot));
			IBlockState origState = world.getBlockState(pos);
			
			world.setBlockState(pos, ImmersiveRailroading.BLOCK_MULTIBLOCK.getDefaultState());
			TileMultiblock te = TileMultiblock.get(world, pos);
			
			te.setAux(posMain, offset, origState);
			if (pos.equals(posMain)) {
				te.setMain(NAME, rot);
			}
			
			te.markDirty();
		}
	}

	public boolean onBlockActivated(World world, BlockPos origin, Rotation rotation, EntityPlayer player, EnumHand hand, BlockPos pos) {
		if (!world.isRemote) {
			player.openGui(ImmersiveRailroading.instance, GuiTypes.BLOCK_STEAM_HAMMER.ordinal(), world, origin.getX(), origin.getY(), origin.getZ());
		}
		return true;
	}

	@Override
	public void onBreak(World world, BlockPos origin, Rotation rot) {
		for (BlockPos offset : componentPositions) {
			MultiblockComponent comp = lookup(offset);
			if (comp == AIR) {
				continue;
			}
			BlockPos pos = origin.add(offset.rotate(rot));
			TileMultiblock te = TileMultiblock.get(world, pos);
			if (te == null) {
				System.out.println("NULL TE!");
				System.out.println(pos);
				continue;
			}
			
			te.onBreak();
		}
		
	}
}
