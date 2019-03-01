package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.PlacementInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.*;
//TODO buildcraft.api.tools.IToolWrench
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemGoldenSpike extends Item {
	public static final String NAME = "item_golden_spike";
	
	public ItemGoldenSpike() {
		super();
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
		setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
        this.setCreativeTab(ItemTabs.MAIN_TAB);
	}

	@Override
	public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack)
	{
		//should this have a world.isRemote or something? New to using this behavior
		if(!entityLiving.getEntityWorld().isRemote && entityLiving instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) entityLiving;
			if(player.isSneaking()) {
				decrementGrade(player.getHeldItemMainhand());

				//should we return true here?
			}
		}


		//default response
		return false;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
		if(!worldIn.isRemote && handIn == EnumHand.MAIN_HAND) {
			if(playerIn.isSneaking()) {
				incrementGrade(playerIn.getHeldItem(handIn));
			}
		}
		return super.onItemRightClick(worldIn, playerIn, handIn);
	}
	
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		ItemStack held = player.getHeldItem(hand);
		if (world.getBlockState(pos).getBlock() == IRBlocks.BLOCK_RAIL_PREVIEW) {
			setPosition(held, pos);
			world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 0.5f, 0.2f, false);
		} else {
			pos = pos.up();
			
			BlockPos tepos = getPosition(held);
			if (tepos != null) {
				if (BlockUtil.canBeReplaced(world, pos.down(), true)) {
					if (!BlockUtil.isIRRail(world, pos.down()) || TileRailBase.get(world, pos.down()).getRailHeight() < 0.5) {
						pos = pos.down();
					}
				}
				TileRailPreview tr = TileRailPreview.get(world, tepos);
				if (tr != null) {
					tr.setCustomInfo(new PlacementInfo(tr.getItem(), player.getRotationYawHead(), pos, hitX, hitY, hitZ, getGrade(held)));
				}
			}
		}
		//TODO
		return EnumActionResult.SUCCESS;
	}

	public static BlockPos getPosition(ItemStack stack) {
		if (stack.getTagCompound() != null && stack.getTagCompound().hasKey("pos")) { 
			return NBTUtil.getPosFromTag(stack.getTagCompound().getCompoundTag("pos"));
		} else {
			return null;
		}
	}
	
	public static void setPosition(ItemStack stack, BlockPos pos) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setTag("pos", NBTUtil.createPosTag(pos));
	}

	private static void incrementGrade(ItemStack stack) {
		//I don't know if this null protection is necessary
		if(stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		NBTTagCompound mainTag = stack.getTagCompound();
		if(mainTag.hasKey("grade")) {
			mainTag.setInteger("grade", mainTag.getInteger("grade") + 1);
		}
		else {
			mainTag.setInteger("grade", 1);
		}

		System.out.println("Adjust spikePos up, now: " + mainTag.getInteger("grade"));
	}

	private static void decrementGrade(ItemStack stack) {
		//I don't know if this null protection is necessary
		if(stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		NBTTagCompound mainTag = stack.getTagCompound();
		if(mainTag.hasKey("grade")) {
			mainTag.setInteger("grade", mainTag.getInteger("grade") - 1);
		}
		else {
			mainTag.setInteger("grade", -1);
		}

		System.out.println("Adjust spikePos down, now: " + mainTag.getInteger("grade"));
	}

	private int getGrade(ItemStack stack) {
		//I don't know if this null protection is necessary
		if(stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}

		NBTTagCompound mainTag = stack.getTagCompound();
		if(mainTag.hasKey("grade")) {
			return mainTag.getInteger("grade");
		}
		else {
			return 0;
		}
	}
}
