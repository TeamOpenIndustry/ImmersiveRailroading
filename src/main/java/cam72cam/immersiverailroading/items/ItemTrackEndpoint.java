package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.PlacementInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
//TODO buildcraft.api.tools.IToolWrench
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemTrackEndpoint extends Item {
	public static final String NAME = "item_track_endpoint";
	
	public ItemTrackEndpoint() {
		super();
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
		setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
        this.setCreativeTab(ItemTabs.MAIN_TAB);
	}
	
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		ItemStack held = player.getHeldItem(hand);
		if (world.getBlockState(pos).getBlock() == IRBlocks.BLOCK_RAIL_PREVIEW) {
			setPosition(held, pos);
			System.out.println("Linked");
		} else {
			pos = pos.up();
			
			BlockPos tepos = getPosition(held);
			if (tepos != null) {
				if (BlockUtil.canBeReplaced(world, pos.down(), true)) {
					pos = pos.down();
				}
				TileRailPreview tr = TileRailPreview.get(world, tepos);
				if (tr != null) {
					tr.setCustomInfo(new PlacementInfo(tr.getItem(), player.getRotationYawHead(), pos, hitX, hitY, hitZ));
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
}
