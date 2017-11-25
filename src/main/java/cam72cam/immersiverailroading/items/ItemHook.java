package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.item.Item;
//TODO buildcraft.api.tools.IToolWrench
import net.minecraft.util.ResourceLocation;

public class ItemHook extends Item {
	public static final String NAME = "item_hook";
	
	public ItemHook() {
		super();
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
		setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
        this.setCreativeTab(ItemTabs.MAIN_TAB);
	}
}
