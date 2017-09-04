package cam72cam.immersiverailroading.items;

import java.util.List;

import javax.annotation.Nullable;

import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.util.RailInfo;
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
		int quarter = getQuarters(stack);
		if (entityLiving.isSneaking()) {
			setQuarters(stack, (quarter) % 4+1);
		}
		return false;
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return super.getUnlocalizedName(stack) + "." + TrackItems.fromMeta(stack.getMetadata());
	}
	
	@Override
	public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState)
    {
		if (player.getEntityWorld().getBlockState(pos.down()).getBlock() instanceof BlockRailBase) {
			pos = pos.down();
		}
		
		RailInfo info = new RailInfo(stack, player, pos, hitX, hitY, hitZ, false);
		
		BuilderBase builder = info.getBuilder(pos);
		if (builder.canBuild()) {
			builder.build();
			return true;
		}
		return false;
    }
	
	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add("Length: " + getLength(stack));
        tooltip.add("Quarters: " + getQuarters(stack));
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
	
	public static void setQuarters(ItemStack stack, int quarters) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setInteger("quarters", quarters);
	}
	
	public static int getQuarters(ItemStack stack) {
		if (stack.getTagCompound() != null){
			return stack.getTagCompound().getInteger("quarters");
		}
		return 3;
	}
}
