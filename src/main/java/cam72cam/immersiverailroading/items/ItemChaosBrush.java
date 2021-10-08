package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import java.util.List;
import java.util.Random;

public class ItemChaosBrush extends AbstractPaintBrush {
	public static final String NAME = "item_chaos_brush";

	private static final Random rand = new Random(System.currentTimeMillis());

	public ItemChaosBrush() {
		super();
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
		setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
		this.setCreativeTab(ItemTabs.MAIN_TAB);
	}

	@Override
	public String selectNewTexture(List<String> texNames, String currentTexture, EntityPlayer player) {
		int curIdx = texNames.indexOf(currentTexture);
		int idx;

		// Avoid randomly selecting the current texture
		do {
			idx = rand.nextInt(texNames.size());
		} while (idx == curIdx);

		return texNames.get(idx);
	}

}
