package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.Optional;

@Optional.Interface(iface = "mezz.jei.api.ingredients.ISlowRenderItem", modid = "jei")
public class ItemSteamHammer extends ItemBlock {

	public ItemSteamHammer() {
		super(ImmersiveRailroading.BLOCK_STEAM_HAMMER);
	}

}
