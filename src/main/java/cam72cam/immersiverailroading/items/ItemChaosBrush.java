package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.Recipes;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ItemChaosBrush extends CustomItem implements TextureSelector {
	private static final Random rand = new Random(System.currentTimeMillis());

	public ItemChaosBrush() {
		super(ImmersiveRailroading.MODID, "item_chaos_brush");

		Recipes.shapedRecipe(this, 1,
				Fuzzy.get("blockObsidian"), Fuzzy.GOLD_INGOT, Fuzzy.WOOD_STICK);
	}


	@Override
	public int getStackSize() {
		return 1;
	}

	@Override
	public List<CreativeTab> getCreativeTabs() {
		return Collections.singletonList(ItemTabs.MAIN_TAB);
	}

	@Override
	public String selectNewTexture(List<String> texNames, String currentTexture, Player player) {
		int curIdx = texNames.indexOf(currentTexture);
		int idx;

		// Avoid randomly selecting the current texture
		do {
			idx = rand.nextInt(texNames.size());
		} while (idx == curIdx);

		String newTexture = texNames.get(idx);
		player.sendMessage(ChatText.BRUSH_CHAOS.getMessage(newTexture));
		return newTexture;
	}
}
