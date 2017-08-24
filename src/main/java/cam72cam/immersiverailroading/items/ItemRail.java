package cam72cam.immersiverailroading.items;

import java.util.List;

import javax.annotation.Nullable;

import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.track.BuilderBase;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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
	
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
		ItemStack stack = playerIn.getHeldItem(handIn);
		int length = getLength(stack);
		setLength(stack, length+ (playerIn.isSneaking() ? -1 : 1));
		playerIn.setHeldItem(handIn, stack);
		return super.onItemRightClick(worldIn, playerIn, handIn);
	}
	
	// called both server and client side,
	// kind of a hack for onItemLeftClick
	public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack) {
		// TODO set quarter
		return false;
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return super.getUnlocalizedName(stack) + "." + TrackItems.fromMeta(stack.getMetadata());
	}
	
	@Override
	public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState)
    {

		float yawHead = player.getRotationYawHead() % 360 + 360;
		TrackDirection dir = (yawHead % 90 < 45) ? TrackDirection.RIGHT : TrackDirection.LEFT;
		int quarter = MathHelper.floor((yawHead % 90f) /90*4);
		
		EnumFacing facing = player.getHorizontalFacing();
		TrackItems tt = TrackItems.fromMeta(stack.getMetadata());
		
		BuilderBase builder = tt.getBuilder(world, pos, facing, getLength(stack), quarter, 4, dir);
		if (builder.canBuild()) {
			builder.build();
			return true;
		}
		return false;
    }
	
	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(""+getLength(stack));
	}
	
	public static void setLength(ItemStack stack, int length) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setInteger("length", length);
	}
	
	public static int getLength(ItemStack stack) {
		if (stack.getTagCompound() != null){
			return stack.getTagCompound().getInteger("length");
		}
		return 10;
	}
}
