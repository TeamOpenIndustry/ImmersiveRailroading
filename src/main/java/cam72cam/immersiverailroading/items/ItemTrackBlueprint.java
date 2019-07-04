package cam72cam.immersiverailroading.items;

import java.util.List;

import javax.annotation.Nullable;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.library.TrackPositionType;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import net.minecraftforge.fml.common.Optional;

@Optional.Interface(iface = "mezz.jei.api.ingredients.ISlowRenderItem", modid = "jei")
public class ItemTrackBlueprint extends Item {
	public static final String NAME = "item_rail";
	public static final float gradeChangeDelta = 0.25f;

	public ItemTrackBlueprint() {
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
				decrementGrade(player, player.getHeldItemMainhand());

				//should we return true here?
			}
		}


		//default response
		return false;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
		if (worldIn.isRemote && handIn == EnumHand.MAIN_HAND && !playerIn.isSneaking()) {
			playerIn.openGui(ImmersiveRailroading.instance, GuiTypes.RAIL.ordinal(), worldIn, (int) playerIn.posX, (int) playerIn.posY, (int) playerIn.posZ);
		}
		else if(!worldIn.isRemote && handIn == EnumHand.MAIN_HAND && playerIn.isSneaking()) {
			incrementGrade(playerIn, playerIn.getHeldItem(handIn));
		}
        return super.onItemRightClick(worldIn, playerIn, handIn);
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		ItemStack stack = player.getHeldItem(hand);

		if (!world.isRemote && hand == EnumHand.OFF_HAND) {
			RailSettings info = settings(stack);
			ItemStack blockinfo = world.getBlockState(pos).getBlock().getItem(world, pos, world.getBlockState(pos));
			if (player.isSneaking()) {
				info = new RailSettings(
                    info.gauge,
                    info.track,
                    info.type,
                    info.length,
                    info.quarters,
                    info.posType,
                    info.direction,
                    info.railBed,
                    blockinfo,
                    info.isPreview,
                    info.isGradeCrossing
                );
			} else {
				info = new RailSettings(
                    info.gauge,
                    info.track,
                    info.type,
                    info.length,
                    info.quarters,
                    info.posType,
                    info.direction,
                    blockinfo,
                    info.railBedFill,
                    info.isPreview,
                    info.isGradeCrossing
				);
			}
			settings(stack, info);
			return EnumActionResult.SUCCESS;
		}

		pos = pos.up();

		if (BlockUtil.canBeReplaced(world, pos.down(), true)) {
			if (!BlockUtil.isIRRail(world, pos.down()) || TileRailBase.get(world, pos.down()).getRailHeight() < 0.5) {
				pos = pos.down();
			}
		}
		PlacementInfo placementInfo = new PlacementInfo(stack, player.getRotationYawHead(), pos, hitX, hitY, hitZ, getGrade(stack));

		if (settings(stack).isPreview) {
			if (!BlockUtil.canBeReplaced(world, pos, false)) {
				pos = pos.up();
			}
			world.setBlockState(pos, IRBlocks.BLOCK_RAIL_PREVIEW.getDefaultState());
			TileRailPreview te = TileRailPreview.get(world, pos);
			if (te != null) {
				te.setup(stack, placementInfo);
			}
			return EnumActionResult.SUCCESS;
		}

		RailInfo info = new RailInfo(player.world, stack, placementInfo, null);
		info.build(player);
		return EnumActionResult.SUCCESS;
    }

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        RailSettings settings = settings(stack);
        tooltip.add(GuiText.TRACK_TYPE.toString(settings.type));
        tooltip.add(GuiText.TRACK_GAUGE.toString(settings.gauge));
        tooltip.add(GuiText.TRACK_LENGTH.toString(settings.length));
        tooltip.add(GuiText.TRACK_POSITION.toString(settings.posType));
        tooltip.add(GuiText.TRACK_DIRECTION.toString(settings.direction));
        tooltip.add(GuiText.TRACK_RAIL_BED.toString(settings.railBed.getDisplayName()));
        tooltip.add(GuiText.TRACK_RAIL_BED_FILL.toString(settings.railBedFill.getDisplayName()));
        tooltip.add((settings.isPreview ? GuiText.TRACK_PLACE_BLUEPRINT_TRUE : GuiText.TRACK_PLACE_BLUEPRINT_FALSE).toString());
        tooltip.add(GuiText.TRACK_QUARTERS.toString(settings.quarters * 90.0/4 ));
	}

	public static void settings(ItemStack stack, RailSettings settings) {
		//I don't know if this null protection is necessary
		if(stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}

		stack.getTagCompound().setTag("settings", settings.toNBT());
	}

	public static RailSettings settings(ItemStack stack) {
		NBTTagCompound settingsTag = stack.getOrCreateSubCompound("settings");
		return new RailSettings(settingsTag);
	}

	private static void incrementGrade(EntityPlayer player, ItemStack stack) {
		//I don't know if this null protection is necessary
		if(stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}

		NBTTagCompound mainTag = stack.getTagCompound();
		float newGrade = gradeChangeDelta;

		if(mainTag.hasKey("grade")) {
			newGrade = mainTag.getFloat("grade") + gradeChangeDelta;
		}

		mainTag.setFloat("grade", newGrade);
		displayGrade(player, newGrade);
	}

	private static void decrementGrade(EntityPlayer player, ItemStack stack) {
		//I don't know if this null protection is necessary
		if(stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}

		NBTTagCompound mainTag = stack.getTagCompound();
		float newGrade = -gradeChangeDelta;

		if(mainTag.hasKey("grade")) {
			newGrade = mainTag.getFloat("grade") - gradeChangeDelta;
		}

		mainTag.setFloat("grade", newGrade);
		displayGrade(player, newGrade);
	}

	private static float getGrade(ItemStack stack) {
		//I don't know if this null protection is necessary
		if(stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}

		NBTTagCompound mainTag = stack.getTagCompound();
		if(mainTag.hasKey("grade")) {
			return mainTag.getFloat("grade");
		}
		else {
			return 0;
		}
	}

	private static void displayGrade(EntityPlayer player, float grade) {
		player.sendStatusMessage(new TextComponentString("Grade now: " + grade), true);
	}
}
