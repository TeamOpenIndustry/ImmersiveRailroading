package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public class ItemChaosBrush extends Item {
	public static final String NAME = "item_chaos_brush";

	public ItemChaosBrush() {
		super();
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
		setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
        this.setCreativeTab(ItemTabs.MAIN_TAB);
	}

}
