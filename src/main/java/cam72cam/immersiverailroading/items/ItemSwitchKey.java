package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemSwitchKey extends Item {
	public static final String NAME = "item_switch_key";

	private TileRailBase lastUsedOn = null;

	public ItemSwitchKey() {
		super();
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
		setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
		this.setCreativeTab(ItemTabs.MAIN_TAB);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
	{
		super.addInformation(stack, worldIn, tooltip, flagIn);
		tooltip.add(GuiText.SWITCH_HAMMER_TOOLTIP.toString());
	}

	public TileRailBase getLastUsedOn() {
		return lastUsedOn;
	}

	public void setLastUsedOn(TileRailBase lastUsedOn) {
		this.lastUsedOn = lastUsedOn;
	}

	/*@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
		if (lastUsedOn == null) {
			if (!worldIn.isRemote) {
				playerIn.sendMessage(new TextComponentString(ChatText.SWITCH_CANT_RESET.toString()));
			}
			return ActionResult.newResult(EnumActionResult.FAIL, playerIn.getHeldItem(handIn));
		} else if (lastUsedOn.isSwitchForced()){
			lastUsedOn.setSwitchForced(SwitchState.NONE);
			lastUsedOn = null;

			if (!worldIn.isRemote) {
				playerIn.sendMessage(new TextComponentString(ChatText.SWITCH_RESET.toString()));
			}

			return ActionResult.newResult(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn));
		} else {
			lastUsedOn = null;

			if (!worldIn.isRemote) {
				playerIn.sendMessage(new TextComponentString(ChatText.SWITCH_CANT_RESET.toString()));
			}

			return ActionResult.newResult(EnumActionResult.FAIL, playerIn.getHeldItem(handIn));
		}
	}*/

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (BlockUtil.isIRRail(worldIn, pos)) {
			return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
		}

		EnumActionResult result;
		TextComponentString message;
		if (lastUsedOn == null) {
			if (!worldIn.isRemote) {
				player.sendMessage(new TextComponentString(ChatText.SWITCH_CANT_RESET.toString()));
			}
			result = EnumActionResult.FAIL;
		} else {
			lastUsedOn.setSwitchForced(SwitchState.NONE);
			if (!worldIn.isRemote) {
				player.sendMessage(new TextComponentString(ChatText.SWITCH_RESET.toString()));
			}
			result = EnumActionResult.SUCCESS;

			lastUsedOn = null;
		}
		return result;
	}
}
