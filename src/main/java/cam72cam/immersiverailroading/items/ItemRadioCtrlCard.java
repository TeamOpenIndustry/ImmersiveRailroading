package cam72cam.immersiverailroading.items;

import java.util.List;

import javax.annotation.Nullable;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemRadioCtrlCard extends Item {
	public static final String NAME = "item_radio_control_card";
	
	public ItemRadioCtrlCard() {
		super();
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
		setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
        	this.setCreativeTab(ItemTabs.MAIN_TAB);
    		this.maxStackSize = 1;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
    	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        	super.addInformation(stack, worldIn, tooltip, flagIn);
        	if(stack.getTagCompound() == null) {
			//skip
		}
        	else if(!stack.getTagCompound().hasKey("linked_uuid")) {
			tooltip.add("Not linked to any locomotive");
		}
        	else {
			tooltip.add("Linked to: " + stack.getTagCompound().getString("linked_uuid"));
		}
	}
}
