package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.gui.ManualGui;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.multiblock.Multiblock;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.immersiverailroading.thirdparty.CompatLoader;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.*;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;

import java.util.Collections;
import java.util.List;

public class ItemManual extends CustomItem {
	public ItemManual() {
		super(ImmersiveRailroading.MODID, "item_manual");

		Fuzzy steel = Fuzzy.STEEL_INGOT;
		IRFuzzy.registerSteelRecipe(this, 3,
				steel, null, steel,
				steel, Fuzzy.BOOK, steel,
				steel, null, steel);
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
		Multiblock mb = new Data(stack).multiblock;
		if (mb == null) {
			return super.getTooltip(stack);
		}
		return Collections.singletonList(GuiText.SELECTOR_TYPE.toString(mb.getName()));
	}

	@Override
	public void onClickAir(Player player, World world, Player.Hand hand) {
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
				GuiTypes.MANUAL.open(player);
//				if (!CompatLoader.openWiki()) {
//				}
			}
		}
	}
	
	@Override
	public ClickResult onClickBlock(Player player, World world, Vec3i pos, Player.Hand hand, Facing facing, Vec3d hit) {
		if (world.isServer) {
			ItemStack item = player.getHeldItem(hand);
			Multiblock current = new Data(item).multiblock;
			if (current == null) {
				return ClickResult.ACCEPTED;
			}
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
				multiblock = MultiblockRegistry.registered().isEmpty() ? null : MultiblockRegistry.registered().get(0);
			}
		}
	}
}
