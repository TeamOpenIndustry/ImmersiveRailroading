package cam72cam.immersiverailroading.multiblock;

import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.blocks.BlockTypes_MetalsAll;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class SteamHammerMultiblock extends Multiblock {
	private static MultiblockComponent steel = new MultiblockComponent(Blocks.IRON_BLOCK);
	private static MultiblockComponent piston = new MultiblockComponent(Blocks.PISTON);
	public static final String NAME = "STEAM_HAMMER";

	public SteamHammerMultiblock() {
		super(NAME, new MultiblockComponent[][][] { // Z
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
		public boolean onBlockActivated(EntityPlayer player, EnumHand hand, BlockPos offset) {
			if (offset.getX() == 2 && offset.getY() == 0 && offset.getZ() == 0) {
				if (!world.isRemote) {
					BlockPos pos = getPos(offset);
					player.openGui(ImmersiveRailroading.instance, GuiTypes.BLOCK_STEAM_HAMMER.ordinal(), world, pos.getX(), pos.getY(), pos.getZ());
				}
				return true;
			}
			return false;
		}
		
		private boolean isCenter(BlockPos offset) {
			return offset.getX() == 2 && offset.getY() == 0 && offset.getZ() == 0;
		}

		@Override
		public boolean isRender(BlockPos offset) {
			return isCenter(offset);
		}

		@Override
		public int getInvSize(BlockPos offset) {
			return isCenter(offset) ? 2 : 0;
		}

		@Override
		public void tick(BlockPos offset) {
			if (world.isRemote) {
				return;
			}
			if (!isCenter(offset)) {
				return;
			}
			TileMultiblock te = getTile(offset);
			if (te == null) {
				ImmersiveRailroading.warn("INVALID MULTIBLOCK TILE AT ", getPos(offset));
				return;
			}
			
			// Decrement craft progress down to 0
			te.setCraftProgress(Math.max(0, te.getCraftProgress() - 1));
			
			float progress = te.getCraftProgress();
			
			IItemHandler container = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
			
			ItemStack input = container.getStackInSlot(0);
			ItemStack output = container.getStackInSlot(1);
			ItemStack steel = new ItemStack(IEContent.blockStorage,1, BlockTypes_MetalsAll.STEEL.getMeta());
			
			
			if (progress == 0) {
				// Try to start crafting
				if (input.isItemEqual(steel) && output.isEmpty() && !te.getCraftItem().isEmpty()) {
					container.extractItem(0, 1, false);
					progress = 100;
					te.setCraftProgress(100);
				}
			}
			
			if (progress == 1) {
				// Stop crafting
				container.insertItem(1, te.getCraftItem().copy(), false);
			}
		}
	}
}
