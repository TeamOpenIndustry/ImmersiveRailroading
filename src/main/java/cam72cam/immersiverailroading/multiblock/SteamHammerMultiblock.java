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
	protected MultiblockInstance newInstance(World world, BlockPos origin, Rotation rot) {
		return new SteamHammerInstance(world, origin, rot);
	}
	private class SteamHammerInstance extends MultiblockInstance {
		public SteamHammerInstance(World world, BlockPos origin, Rotation rot) {
			super(world, origin, rot);
		}

		@Override
		public void onCreate() {
			for (BlockPos offset : componentPositions) {
				MultiblockComponent comp = lookup(offset);
				if (comp == AIR) {
					continue;
				}
				
				BlockPos pos = origin.add(offset.rotate(rot));
				IBlockState origState = world.getBlockState(pos);
				
				world.setBlockState(pos, ImmersiveRailroading.BLOCK_MULTIBLOCK.getDefaultState());
				TileMultiblock te = TileMultiblock.get(world, pos);
				
				te.configure(NAME, rot, offset, origState);
				System.out.println(te.getOrigin().equals(origin));
			}
		}

		@Override
		public boolean onBlockActivated(EntityPlayer player, EnumHand hand, BlockPos offset) {
			if (offset.getX() == 2 && offset.getY() == 0 && offset.getZ() == 0) {
				if (!world.isRemote) {
					player.openGui(ImmersiveRailroading.instance, GuiTypes.BLOCK_STEAM_HAMMER.ordinal(), world, origin.getX(), origin.getY(), origin.getZ());
				}
				return true;
			}
			return false;
		}

		@Override
		public void onBreak() {
			if (world.isRemote) {
				return;
			}
			for (BlockPos offset : componentPositions) {
				MultiblockComponent comp = lookup(offset);
				if (comp == AIR) {
					continue;
				}
				BlockPos pos = origin.add(offset.rotate(rot));
				TileMultiblock te = TileMultiblock.get(world, pos);
				if (te == null) {
					world.destroyBlock(pos, true);
					continue;
				}
				te.onBreak();
			}
		}

		@Override
		public boolean isRender(BlockPos offset) {
			return offset.getX() == 2 && offset.getY() == 0 && offset.getZ() == 0;
		}
		
	}
}
