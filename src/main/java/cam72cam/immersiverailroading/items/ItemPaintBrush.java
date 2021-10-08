package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
//TODO buildcraft.api.tools.IToolWrench
import net.minecraft.util.ResourceLocation;

import java.util.List;

public class ItemPaintBrush extends AbstractPaintBrush {
	public static final String NAME = "item_paint_brush";
	
	public ItemPaintBrush() {
		super();
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
		setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
		this.setCreativeTab(ItemTabs.MAIN_TAB);
	}

	@Override
	public String selectNewTexture(List<String> texNames, String currentTexture, EntityPlayer player) {
		int idx = texNames.indexOf(currentTexture);
		idx = (idx + (player.isSneaking() ? -1 : 1) + texNames.size()) % (texNames.size());
		return texNames.get(idx);
	}
}
