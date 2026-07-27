package cam72cam.immersiverailroading.remotecontrol;

import java.util.Objects;
import java.util.UUID;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.ItemWirelessRemotecontrol;
import cam72cam.immersiverailroading.net.RemoteControlActivePacket;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;

public class WirelessRemotecontrolClient {
	private static UUID loco = null;
	private static RemoteControlData cachedData = null;
	
	private WirelessRemotecontrolClient() {
	}

    public static void onClientTick() {
        if (!MinecraftClient.isReady()) {
            return;
        }
        Player player = MinecraftClient.getPlayer();
        UUID newLoco = resolveLoco(player);

        if (!Objects.equals(newLoco, loco)) {
            new RemoteControlActivePacket(newLoco).sendToServer();
            if (newLoco == null) {
            	clearData();
            }
        }
        loco = newLoco;
    }
    
    private static UUID resolveLoco(Player player) {
        ItemStack held = player.getHeldItem(Player.Hand.SECONDARY);
        if (!held.is(IRItems.ITEM_WIRELESS_REMOTECONTROL)) {
            return null;
        }
        ItemWirelessRemotecontrol.Data data = new ItemWirelessRemotecontrol.Data(held);
        return data.linked;
    }

    public static UUID getLoco() {
        return loco;
    }
    
    public static void updateData(RemoteControlData data) {
    	cachedData = data;
    }
    
    public static RemoteControlData getData() {
    	return cachedData;
    }
    
    private static void clearData() {
    	cachedData = null;
    }
}
