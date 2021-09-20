package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.util.SpawnUtil;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;

import java.util.List;

public abstract class BaseItemRollingStock extends CustomItem {

	public BaseItemRollingStock(String modID, String name) {
		super(modID, name);
	}

	@Override
	public String getCustomName(ItemStack stack) {
		EntityRollingStockDefinition def = new Data(stack).def;
        return def == null ? null : def.name();
	}

	public static ClickResult tryPlaceStock(Player player, World worldIn, Vec3i pos, Player.Hand hand, List<ItemComponentType> parts) {
		if (!player.hasPermission(Permissions.STOCK_ASSEMBLY)) {
			return ClickResult.REJECTED;
		}

		ItemStack stack = player.getHeldItem(hand);
		
		EntityRollingStockDefinition def = new Data(stack).def;
		if (def == null) {
			player.sendMessage(ChatText.STOCK_INVALID.getMessage());
			return ClickResult.REJECTED;
		}

		if (parts == null) {
			parts = def.getItemComponents();
		}

		return SpawnUtil.placeStock(player, hand, worldIn, pos, def, parts);
	}

	protected static class Data extends ItemDataSerializer {
		@TagField(value = "defID")
		public EntityRollingStockDefinition def;

		@TagField(value = "gauge")
		public Gauge gauge;

		@TagField(value = "texture_variant")
		public String texture;

		protected Data(ItemStack stack) {
			super(stack);

			if (gauge == null) {
				gauge = def != null ? def.recommended_gauge : Gauge.from(Gauge.STANDARD);
			}

			if (def != null && !def.textureNames.containsKey(texture)) {
				texture = null;
			}
		}
	}
}
