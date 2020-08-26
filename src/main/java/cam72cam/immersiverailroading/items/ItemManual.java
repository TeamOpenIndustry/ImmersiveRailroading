package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.multiblock.Multiblock;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.immersiverailroading.thirdparty.CompatLoader;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.*;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.CollectionUtil;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.world.World;

import java.util.List;

public class ItemManual extends CustomItem {
	public ItemManual() {
		super(ImmersiveRailroading.MODID, "item_manual");

		Fuzzy steel = Fuzzy.STEEL_INGOT.example() != null ? Fuzzy.STEEL_INGOT : Fuzzy.IRON_INGOT;
		Recipes.register(this, 3,
				steel, null, steel, steel, Fuzzy.BOOK, steel, steel, null, steel);
	}

	@Override
	public int getStackSize() {
		return 1;
	}

	@Override
	public List<CreativeTab> getCreativeTabs() {
		return CollectionUtil.listOf(ItemTabs.MAIN_TAB);
	}


	@Override
	public List<String> getTooltip(ItemStack stack) {
		return CollectionUtil.listOf(GuiText.SELECTOR_TYPE.toString(new Data(stack).multiblock.getName()));
	}

	@Override
	public void onClickAir(Player player, World world, Hand hand) {
		if (player.isCrouching()) {
			if (world.isServer) {
				ItemStack item = player.getHeldItem(hand);
				Data data = new Data(item);
				List<Multiblock> keys = MultiblockRegistry.registered();
				data.multiblock = keys.get((keys.indexOf(data.multiblock) + 1) % (keys.size()));
				data.write();
				player.sendMessage(PlayerMessage.direct("Placing: " + data.multiblock.getName()));
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
			Multiblock current = new Data(item).multiblock;
			Vec3i realPos = pos;
			if (facing == Facing.DOWN) {
				realPos = realPos.down();
			}
			if (facing == Facing.UP) {
				realPos = realPos.up();
			}
			current.place(world, player, realPos, Rotation.from(Facing.fromAngle(player.getYawHead()+180)));
		}
		return ClickResult.ACCEPTED;
	}

	public static class Data extends ItemDataSerializer {
		@TagField(value = "name", mapper = MBTagMapper.class)
		public Multiblock multiblock;

		private static class MBTagMapper implements TagMapper<Multiblock> {
			@Override
			public TagAccessor<Multiblock> apply(Class<Multiblock> type, String fieldName, TagField tag) {
				return new TagAccessor<>(
						(d, m) -> d.setString(fieldName, m != null ? m.getName() : null),
						d -> {
							String name = d.getString(fieldName);
							return name != null ? MultiblockRegistry.get(name) : null;
						}
				);
			}
		}

		public Data(ItemStack stack) {
			super(stack);

			if (multiblock == null) {
				multiblock = MultiblockRegistry.registered().get(0);
			}
		}
	}
}
