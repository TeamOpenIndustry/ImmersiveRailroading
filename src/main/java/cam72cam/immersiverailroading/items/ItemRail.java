package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.blocks.BlockRail;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.library.TrackType;
import cam72cam.immersiverailroading.track.BuilderBase;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemRail extends ItemBlock {

	public ItemRail(Block block) {
		super(block);
		this.setMaxDamage(0);
		this.setHasSubtypes(true);
	}

	public int getMetadata(int damage) {
		return damage;
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return super.getUnlocalizedName(stack) + "." + TrackItems.fromMeta(stack.getMetadata());
	}
	
	@Override
	public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState)
    {
		EnumFacing facing = newState.getValue(BlockRail.FACING);
		TrackType tt = newState.getValue(BlockRail.TRACK_TYPE);
		
		BuilderBase builder = tt.getBuilder(world, pos, facing);
		if (builder.canBuild()) {
			builder.build();
			return true;
		}
		return false;
    }
}
