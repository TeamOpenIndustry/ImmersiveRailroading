package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemMultiblockType;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.immersiverailroading.thirdparty.CompatLoader;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.*;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.CollectionUtil;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.world.World;

import java.util.List;

public class ItemManual extends ItemBase {
	public ItemManual() {
		super(ImmersiveRailroading.MODID, "item_manual", 1, ItemTabs.MAIN_TAB);

		Fuzzy steel = Fuzzy.STEEL_INGOT.example() != null ? Fuzzy.STEEL_INGOT : Fuzzy.IRON_INGOT;
		Recipes.register(this, 3,
				steel, null, steel, steel, Fuzzy.BOOK, steel, steel, null, steel);
	}
	
	@Override
	public List<String> getTooltip(ItemStack stack) {
		return CollectionUtil.listOf(GuiText.SELECTOR_TYPE.toString(ItemMultiblockType.get(stack)));
	}

	@Override
	public void onClickAir(Player player, World world, Hand hand) {
		if (player.isCrouching()) {
			if (world.isServer) {
				ItemStack item = player.getHeldItem(hand);
				String current = ItemMultiblockType.get(item);
				List<String> keys = MultiblockRegistry.keys();
				current = keys.get((keys.indexOf(current) + 1) % (keys.size()));
				ItemMultiblockType.set(item, current);
				player.sendMessage(PlayerMessage.direct("Placing: " + current));
			}
		} else {
			if (world.isClient) {
				if (!CompatLoader.openWiki()) {
					player.sendMessage(PlayerMessage.url("https://github.com/cam72cam/ImmersiveRailroading/wiki"));
				}
			}
		}
	}
	
	@Override
	public ClickResult onClickBlock(Player player, World world, Vec3i pos, Hand hand, Facing facing, Vec3d hit) {
		if (world.isServer) {
			ItemStack item = player.getHeldItem(hand);
			String current = ItemMultiblockType.get(item);
			Vec3i realPos = pos;
			if (facing == Facing.DOWN) {
				realPos = realPos.down();
			}
			if (facing == Facing.UP) {
				realPos = realPos.up();
			}
			MultiblockRegistry.get(current).place(world, player, realPos, Rotation.from(Facing.fromAngle(player.getYawHead()+180)));
		}
		return ClickResult.ACCEPTED;
	}
}
