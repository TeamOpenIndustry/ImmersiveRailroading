package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public class ItemSwitchHammer extends Item {
	public static final String NAME = "item_switch_hammer";

	public ItemSwitchHammer() {
		super();
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
		setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
		this.setCreativeTab(ItemTabs.MAIN_TAB);
	}
}
