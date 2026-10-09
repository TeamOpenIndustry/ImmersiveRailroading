package cam72cam.immersiverailroading.net;

import java.util.UUID;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.items.ItemWirelessRemoteControl;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;

public class KeyPressPacket extends Packet {
	@TagField
	private boolean disableIndependentThrottle;
	@TagField
	private KeyTypes type;
	@TagField
	private UUID loco;

	public KeyPressPacket() {
	}

	public KeyPressPacket(KeyTypes type, UUID loco) {
		this.type = type;
		this.loco = loco;
		
	}

	public KeyPressPacket(KeyTypes type) {
		this.disableIndependentThrottle = Config.ImmersionConfig.disableIndependentThrottle;
		this.type = type;
		Player player = MinecraftClient.getPlayer();
		if (player.getRiding() instanceof EntityRollingStock) {
			// Do it client side, expect server to overwrite
			player.getRiding().as(EntityRollingStock.class).handleKeyPress(player, type, disableIndependentThrottle);
		}
	}

	@Override
	protected void handle() {
		Player player = getPlayer();

		if (loco != null) {
			// Player controls with Wireless Remote Control
			handleRemoteControl(player);
		} else if (player.hasPermission(Permissions.LOCOMOTIVE_CONTROL)) {
			// Player is in the Locomotive
			player.getRiding().as(EntityRollingStock.class).handleKeyPress(player, type, disableIndependentThrottle);
		}
	}
	
	private void handleRemoteControl(Player player) {
		Locomotive stock = getWorld().getEntity(loco, Locomotive.class); 
		if (stock != null && player.hasPermission(Permissions.LOCOMOTIVE_CONTROL)) {
			ItemStack held = player.getHeldItem(Player.Hand.SECONDARY); 
			ItemWirelessRemoteControl.Data data = new ItemWirelessRemoteControl.Data(held);
			if (loco.equals(data.linked)) {
				stock.handleKeyPress(player, type, disableIndependentThrottle);
			}
		}
	}
}
