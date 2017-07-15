package cam72cam.immersiverailroading;

import cam72cam.immersiverailroading.blocks.BlockRail;
import cam72cam.immersiverailroading.library.TrackType;
import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.track.BuilderCrossing;
import cam72cam.immersiverailroading.track.BuilderSlope;
import cam72cam.immersiverailroading.track.BuilderStraight;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemBlockMeta extends ItemBlock {

	public ItemBlockMeta(Block block) {
		super(block);
		this.setMaxDamage(0);
		this.setHasSubtypes(true);
	}

	public int getMetadata(int damage) {
		return damage;
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return super.getUnlocalizedName(stack) + "." + stack.getItemDamage();
	}
	
	@Override
	public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState)
    {
		EnumFacing facing = newState.getValue(BlockRail.FACING);
		TrackType tt = newState.getValue(BlockRail.TRACK_TYPE);
		BuilderBase builder;
		switch (tt.getType()) {
		case STRAIGHT_SMALL:
			builder = new BuilderStraight(world, pos.getX(), pos.getY(), pos.getZ(), facing, 2);
			break;
		case STRAIGHT_MEDIUM:
			builder = new BuilderStraight(world, pos.getX(), pos.getY(), pos.getZ(), facing, 8);
			break;
		case STRAIGHT_LARGE:
			builder = new BuilderStraight(world, pos.getX(), pos.getY(), pos.getZ(), facing, 16);
			break;
		case CROSSING:
			builder = new BuilderCrossing(world, pos.getX(), pos.getY(), pos.getZ(), facing);
			break;
		case SLOPE_MEDIUM:
			builder = new BuilderSlope(world, pos.getX(), pos.getY(), pos.getZ(), facing, tt);
			break;
		case SLOPE_LARGE:
			builder = new BuilderSlope(world, pos.getX(), pos.getY(), pos.getZ(), facing, tt);
			break;
		default:
			return false;
		}
		
		if (builder.canBuild()) {
			builder.build();
		}

        return true;
    }
}
