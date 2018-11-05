package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public class ItemComponentsList extends Item{
	public static final String NAME = "item_components_list";
	
	public ItemComponentsList() {
		super();
		
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
		setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
        this.setCreativeTab(ItemTabs.MAIN_TAB);
	}
	
}
