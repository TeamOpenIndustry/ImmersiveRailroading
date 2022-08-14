package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.PaintBrushMode;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.Recipes;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ItemPaintBrush extends CustomItem {
	private static final Random rand = new Random(System.currentTimeMillis());

	public ItemPaintBrush() {
		super(ImmersiveRailroading.MODID, "item_paint_brush");

		Recipes.shapedRecipe(this, 1,
				Fuzzy.WOOL_BLOCK, Fuzzy.IRON_INGOT, Fuzzy.WOOD_STICK);
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
	public void onClickAir(Player player, World world, Player.Hand hand) {
		if (player.isCrouching()) {
			if (world.isServer) {
				ItemStack item = player.getHeldItem(hand);
				Data data = new Data(item);
				data.mode = PaintBrushMode.values()[(data.mode.ordinal() + 1) % (PaintBrushMode.values().length)];
				data.write();
				player.sendMessage(PlayerMessage.direct("Set mode to: " + data.mode));
			}
		}
	}

	public String selectNewTexture(Map<String, String> textures, String currentTexture, Player player, ItemStack stack) {
		Data data = new Data(stack);
		switch (data.mode) {
			case SEQUENTIAL:
				return selectNextTexture(new ArrayList<>(textures.keySet()), currentTexture, player);
			case RANDOM:
				return selectRandomTexture(textures, currentTexture, player);
			default:
				ImmersiveRailroading.error("Programmer error: invalid PaintBrush mode: " + data.mode + " is not supported");
				return currentTexture;
		}
	}

	private String selectNextTexture(List<String> texNames, String currentTexture, Player player) {
		int curIdx = texNames.indexOf(currentTexture);
		int idx = (curIdx + (player.isCrouching() ? -1 : 1) + texNames.size()) % (texNames.size());
		return texNames.get(idx);
	}

	private String selectRandomTexture(Map<String, String> textures, String currentTexture, Player player) {
		List<String> texNames = new ArrayList<>(textures.keySet());
		int curIdx = texNames.indexOf(currentTexture);
		int idx;

		// Avoid randomly selecting the current texture
		do {
			idx = rand.nextInt(textures.size());
		} while (idx == curIdx);

		String newTexture = texNames.get(idx);
		player.sendMessage(ChatText.BRUSH_CHAOS.getMessage(textures.get(newTexture)));
		return newTexture;
	}

	public static class Data extends ItemDataSerializer {
		@TagField(value = "mode")
		public PaintBrushMode mode;

		public Data(ItemStack stack) {
			super(stack);

			if (mode == null) {
				mode = PaintBrushMode.SEQUENTIAL;
			}
		}
	}

}
