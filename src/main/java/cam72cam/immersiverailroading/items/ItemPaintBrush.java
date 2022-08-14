package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.GuiText;
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
	public List<String> getTooltip(ItemStack stack) {
		PaintBrushMode pbm = new Data(stack).mode;
		if (pbm == null) {
			return super.getTooltip(stack);
		}
		List<String> tips = new ArrayList<>();
		tips.add(GuiText.PAINT_BRUSH_MODE_TOOLTIP.toString(pbm.name()));
		tips.add(GuiText.PAINT_BRUSH_DESCRIPTION_TOOLTIP.toString());
		return tips;
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
		List<String> texNames = new ArrayList<>(textures.keySet());
		int curIdx = texNames.indexOf(currentTexture);
		int newIdx;
		switch (data.mode) {
			case SEQUENTIAL:
				newIdx = selectNextTextureIndex(texNames.size(), curIdx, player);
				break;
			case RANDOM:
				newIdx = selectRandomTextureIndex(texNames.size(), curIdx);
				break;
			default:
				ImmersiveRailroading.error("Programmer error: invalid PaintBrush mode: " + data.mode + " is not supported");
				newIdx = curIdx;
				break;
		}

		String newTexture = texNames.get(newIdx);
		if (Config.ConfigDebug.debugPaintBrush) {
			player.sendMessage(ChatText.BRUSH_NEXT.getMessage(textures.get(newTexture), data.mode.name()));
		}
		return newTexture;
	}

	private int selectNextTextureIndex(int nTextures, int curIdx, Player player) {
		return (curIdx + (player.isCrouching() ? -1 : 1) + nTextures) % (nTextures);
	}

	private int selectRandomTextureIndex(int nTextures, int curIdx) {
		// Avoid randomly selecting the current texture
		int newIdx = curIdx;
		while (newIdx == curIdx) {
			newIdx = rand.nextInt(nTextures);
		}

		return newIdx;
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
