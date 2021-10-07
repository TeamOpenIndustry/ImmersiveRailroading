package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.tile.TileRailBase;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
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

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (worldIn.isAirBlock(pos)) {
			if (lastUsedOn == null) {
				return EnumActionResult.FAIL;
			} else if (lastUsedOn.isSwitchForced()){
				lastUsedOn.setSwitchForced(SwitchState.NONE);
				lastUsedOn = null;

				if (!worldIn.isRemote) {
					player.sendMessage(new TextComponentString(ChatText.SWITCH_UNLOCKED.toString()));
				}

				return EnumActionResult.SUCCESS;
			} else {
				lastUsedOn = null;
				return EnumActionResult.FAIL;
			}
		}

		return EnumActionResult.PASS;
	}
}
