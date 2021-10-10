package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.*;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.world.World;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.text.TextComponentString;

import java.util.Collections;
import java.util.List;

public class ItemSwitchKey extends CustomItem {
	private TileRailBase lastUsedOn = null;

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
		return Collections.singletonList(GuiText.SWITCH_HAMMER_TOOLTIP.toString(lastUsedOn != null && lastUsedOn.isSwitchForced() ? "\nCoordinates of the last locked switch: " + lastUsedOn.findSwitchParent().info.placementInfo.placementPosition.toString() + "\nLocked to: " + lastUsedOn.findSwitchParent().info.switchForced : ""));
	}

	public TileRailBase getLastUsedOn() {
		return lastUsedOn;
	}

	public void setLastUsedOn(TileRailBase lastUsedOn) {
		this.lastUsedOn = lastUsedOn;
	}

	@Override
	public void onClickAir(Player player, World world, Player.Hand hand) {
		if (lastUsedOn != null) {
			if (lastUsedOn.isSwitchForced()) {
				lastUsedOn.setSwitchForced(SwitchState.NONE);
				if (world.isServer) {
					player.sendMessage(PlayerMessage.translate(ChatText.SWITCH_RESET.toString()));
				}
			} else {
				if (world.isServer) {
					player.sendMessage(PlayerMessage.translate(ChatText.SWITCH_ALREADY_RESET.toString()));
				}
			}

			lastUsedOn = null;
		} else {
			if (world.isServer) {
				player.sendMessage(PlayerMessage.translate(ChatText.SWITCH_CANT_RESET.toString()));
			}
		}
	}
}
