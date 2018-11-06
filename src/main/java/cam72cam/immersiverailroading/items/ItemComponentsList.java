package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.gui.ComponentsListGUI;
import cam72cam.immersiverailroading.library.GuiTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class ItemComponentsList extends Item{
	public static final String NAME = "item_components_list";
	
	public ItemComponentsList() {
		super();
		
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
		setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
        this.setCreativeTab(ItemTabs.MAIN_TAB);
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		if (world.isRemote) {
			System.out.println("open Gui");
			player.sendMessage(new TextComponentString("open GUI"));
            player.openGui(ImmersiveRailroading.instance, GuiTypes.COMPONENTS_LIST.ordinal(), world, (int) player.posX, (int) player.posY, (int) player.posZ);
        }
        return super.onItemRightClick(world, player, hand);
	}
	
}
