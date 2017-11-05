package cam72cam.immersiverailroading.items;

import java.util.List;

import javax.annotation.Nullable;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.library.TrackPositionType;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.fml.common.Optional;

@Optional.Interface(iface = "mezz.jei.api.ingredients.ISlowRenderItem", modid = "jei")
public class ItemRail extends ItemBlock {
	public static final String NAME = "item_rail";

	public ItemRail(Block block) {
		super(block);
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
		setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
        this.setCreativeTab(CreativeTabs.TOOLS);
	}
	
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
		if (worldIn.isRemote) {
            playerIn.openGui(ImmersiveRailroading.instance, GuiTypes.RAIL.ordinal(), worldIn, (int) playerIn.posX, (int) playerIn.posY, (int) playerIn.posZ);
        }
        return super.onItemRightClick(worldIn, playerIn, handIn);
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return super.getUnlocalizedName(stack);
	}
	
	@Override
	public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState)
    {
		if (ItemRail.isPreview(stack)) {
			world.setBlockState(pos, ImmersiveRailroading.BLOCK_RAIL_PREVIEW.getDefaultState());
			TileRailPreview te = TileRailPreview.get(world, pos);
			if (te != null) {
				te.init(stack, player.getRotationYawHead(), hitX, hitY, hitZ);
			}
			// don't decrement stack regardless
			return false;
		}
		if (player.getEntityWorld().getBlockState(pos.down()).getBlock() instanceof BlockRailBase) {
			pos = pos.down();
		}
		
		RailInfo info = new RailInfo(stack, player.world, player.getRotationYawHead(), pos, hitX, hitY, hitZ); 
		info.build(player, pos);
		// don't decrement stack regardless
		return false;
    }

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(GuiText.TRACK_TYPE.toString(getType(stack)));
        tooltip.add(GuiText.TRACK_LENGTH.toString(getLength(stack)));
        tooltip.add(GuiText.TRACK_QUARTERS.toString(getQuarters(stack)));
        tooltip.add(GuiText.TRACK_POSITION.toString(getPosType(stack)));
        tooltip.add(GuiText.TRACK_RAIL_BED.toString(getBed(stack).getDisplayName()));
        tooltip.add(GuiText.TRACK_RAIL_BED_FILL.toString(getBedFill(stack).getDisplayName()));
        tooltip.add((isPreview(stack) ? GuiText.TRACK_PLACE_BLUEPRINT_TRUE : GuiText.TRACK_PLACE_BLUEPRINT_FALSE).toString());
	}

	public static void setType(ItemStack stack, TrackItems type) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setInteger("type", type.ordinal());
	}
	
	public static TrackItems getType(ItemStack stack) {
		if (stack.getTagCompound() != null){
			return TrackItems.values()[stack.getTagCompound().getInteger("type")];
		}
		return TrackItems.STRAIGHT;
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
	
	public static void setPosType(ItemStack stack, TrackPositionType posType) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setInteger("pos_type", posType.ordinal());
	}
	
	public static TrackPositionType getPosType(ItemStack stack) {
		if (stack.getTagCompound() != null){
			return TrackPositionType.values()[stack.getTagCompound().getInteger("pos_type")];
		}
		return TrackPositionType.FIXED;
	}

	public static ItemStack getBed(ItemStack stack) {
		if (stack.getTagCompound() != null && stack.getTagCompound().hasKey("bedItem")) { 
			return new ItemStack(stack.getTagCompound().getCompoundTag("bedItem"));
		} else {
			return ItemStack.EMPTY;
		}
	}
	
	public static void setBed(ItemStack stack, ItemStack base) {
		stack.getTagCompound().setTag("bedItem", base.serializeNBT());
	}

	public static ItemStack getBedFill(ItemStack stack) {
		if (stack.getTagCompound() != null && stack.getTagCompound().hasKey("bedFill")) { 
			return new ItemStack(stack.getTagCompound().getCompoundTag("bedFill"));
		} else {
			return ItemStack.EMPTY;
		}
	}
	public static void setBedFill(ItemStack stack, ItemStack base) {
		stack.getTagCompound().setTag("bedFill", base.serializeNBT());
	}

	public static boolean isPreview(ItemStack stack) {
		if (stack.getTagCompound() != null && stack.getTagCompound().hasKey("isPreview")) { 
			return stack.getTagCompound().getBoolean("isPreview");
		} else {
			return false;
		}
	}
	public static void setPreview(ItemStack stack, boolean value) {
		stack.getTagCompound().setBoolean("isPreview", value);
	}
}
