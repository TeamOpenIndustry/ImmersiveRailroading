package cam72cam.immersiverailroading.items;

import java.util.List;

import javax.annotation.Nullable;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.GuiTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemTrackExchanger extends Item {
	public static final String NAME = "item_track_exchanger";

	public ItemTrackExchanger() {
		super();
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
		setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
		this.setCreativeTab(ItemTabs.MAIN_TAB);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, tooltip, flagIn);
		tooltip.add(GuiText.TRACK_SWITCHER_TOOLTIP.toString());
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
		if (worldIn.isRemote && handIn == EnumHand.MAIN_HAND) {
			if (Minecraft.getMinecraft().objectMouseOver.typeOfHit == RayTraceResult.Type.MISS) { //only open gui when player doesn't right-click a block
				playerIn.openGui(ImmersiveRailroading.instance, GuiTypes.TRACK_EXCHANGER.ordinal(), worldIn, (int) playerIn.posX, (int) playerIn.posY, (int) playerIn.posZ);
			}
        }
        return super.onItemRightClick(worldIn, playerIn, handIn);
	}
}
