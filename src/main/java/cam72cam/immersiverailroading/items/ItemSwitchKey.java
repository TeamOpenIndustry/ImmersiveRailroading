package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.*;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.world.World;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ItemSwitchKey extends CustomItem {
	public static final long CLICK_COOLDOWN_MILLIS = 100L;
	public static final String LAST_USED_ON_KEY = "lastUsedOn";
	public static final String FORCED_INTO_STATE_KEY = "forcedIntoState";
	public static final String LAST_USED_AT_KEY = "lastUsedAt";

	public ItemSwitchKey() {
		super(ImmersiveRailroading.MODID, "item_switch_key");

		Fuzzy steel = Fuzzy.STEEL_INGOT;
		IRFuzzy.registerSteelRecipe(this, 2,
				null, steel,
				null, steel,
				steel, steel);
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
	public List<String> getTooltip(ItemStack stack)
	{
		Data data = new Data(stack);
		if (data.isEmpty()) {
			return Collections.singletonList(GuiText.SWITCH_KEY_TOOLTIP.toString());
		} else {
			return Arrays.asList(
					GuiText.SWITCH_KEY_TOOLTIP.toString(),
					GuiText.SWITCH_KEY_DATA_TOOLTIP.toString(
							data.lastUsedOn.toString(),
							data.forcedIntoState.toString())
			);
		}
	}

	@Override
	public void onClickAir(Player player, World world, Player.Hand hand) {
		if (!player.hasPermission(Permissions.SWITCH_CONTROL)) {
			return;
		}

		ItemStack stack = player.getHeldItem(hand);
		Data data = new Data(stack);

		PlayerMessage message = null;

		if (!data.isEmpty()) {
			if (data.isInClickCooldown()) {
				return;
			}

			TileRailBase lastUsedOn = data.getLastUsedOnSwitch(world);
			if (lastUsedOn != null) {
				if (lastUsedOn.isSwitchForced()) {
					lastUsedOn.setSwitchForced(SwitchState.NONE);
					message = PlayerMessage.translate(ChatText.SWITCH_RESET.toString());
				} else {
					message = PlayerMessage.translate(ChatText.SWITCH_ALREADY_RESET.toString());
				}
			}

			data.clear();
			data.write();
		} else {
			message = PlayerMessage.translate(ChatText.SWITCH_CANT_RESET.toString());
		}

		// Only send client-side to avoid spamming the chat for other players
		if (message != null && world.isClient) {
			player.sendMessage(message);
		}
	}

	public static class Data extends ItemDataSerializer {
		@TagField(value = LAST_USED_ON_KEY)
		public Vec3i lastUsedOn;

		@TagField(value = FORCED_INTO_STATE_KEY)
		public SwitchState forcedIntoState;

		@TagField(value = LAST_USED_AT_KEY)
		public Long lastUsedAt;

		public Data(ItemStack stack) {
			super(stack);

			if (lastUsedAt == null) {
				lastUsedAt = System.currentTimeMillis();
			}
		}

		public boolean isEmpty() {
			return lastUsedOn == null;
		}

		public boolean isInClickCooldown() {
			return System.currentTimeMillis() < this.lastUsedAt + CLICK_COOLDOWN_MILLIS;
		}

		public TileRailBase getLastUsedOnSwitch(World world) {
			if (isEmpty()) {
				return null;
			}

			return world.getBlockEntity(lastUsedOn, TileRailBase.class);
		}

		public void clear() {
			lastUsedOn = null;
			forcedIntoState = null;
			lastUsedAt = System.currentTimeMillis();
		}
	}
}
