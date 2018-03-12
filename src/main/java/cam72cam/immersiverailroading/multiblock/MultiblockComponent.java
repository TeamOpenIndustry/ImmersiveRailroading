package cam72cam.immersiverailroading.multiblock;

import java.util.function.Function;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class MultiblockComponent {
	private final Function<IBlockState, Boolean> blockCheck;
	private final Function<ItemStack, Boolean> itemCheck;
	public final String name;
	protected final IBlockState def;
	
	public MultiblockComponent() {
		def = null;
		name = "none";
		this.blockCheck = (IBlockState target) -> true;
		this.itemCheck = (ItemStack stack) -> false;
	}
	
	public MultiblockComponent(Block block) {
		def = block.getDefaultState();
		name = block.getLocalizedName();
		this.blockCheck = (IBlockState target) -> target.getBlock() == block;
		this.itemCheck = (ItemStack stack) -> Block.getBlockFromItem(stack.getItem()) == block;
	}
	
	public MultiblockComponent(IBlockState state, ItemStack stack) {
		this.def = state;
		this.name = stack.getDisplayName(); 
		this.blockCheck = (IBlockState target) -> target.equals(state);
		this.itemCheck = (ItemStack tstack) -> tstack.isItemEqual(stack);
	}
	
	public MultiblockComponent(Function<IBlockState, Boolean> blockCheck, Function<ItemStack, Boolean> itemCheck, IBlockState def, String name) {
		this.def = def;
		this.name = name;
		this.blockCheck = blockCheck;
		this.itemCheck = itemCheck;
	}
	
	public boolean valid(IBlockAccess world, BlockPos pos) {
		return blockCheck.apply(world.getBlockState(pos));
	}

	public boolean place(World world, EntityPlayer player, BlockPos pos) {
		if (player.isCreative()) {
			if (def != null) {
				world.setBlockState(pos, def);
				world.notifyBlockUpdate(pos, def, def, 3);
			}
			return true;
		} else {
			for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
				ItemStack stack = player.inventory.getStackInSlot(slot);
				if (itemCheck.apply(stack)) {
					
					int count = stack.getCount();
					
					ItemStack backup = player.getHeldItem(EnumHand.MAIN_HAND).copy();
					EnumActionResult result = stack.getItem().onItemUse(player, world, pos, EnumHand.MAIN_HAND, EnumFacing.DOWN, 0.5f, 0f, 0.5f);
					player.setHeldItem(EnumHand.MAIN_HAND, backup);
					
					if (result == EnumActionResult.SUCCESS) {
						if (player.inventory.getStackInSlot(slot).getCount() == count) {
							//Decrement inv slot
							stack.setCount(stack.getCount() - 1);
							player.inventory.setInventorySlotContents(slot, stack);
						}
						world.setBlockState(pos, def);
						world.notifyBlockUpdate(pos, def, def, 3);
						return true;
					}
				}
			}
			return false;
		}
	}
}
