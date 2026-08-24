package cam72cam.immersiverailroading.remotecontrol;

import java.util.Objects;
import java.util.UUID;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.gui.overlay.RemoteOverlay;
import cam72cam.immersiverailroading.items.ItemWirelessRemoteControl;
import cam72cam.immersiverailroading.net.RemoteControlActivePacket;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;

public class WirelessRemotecontrolClient {
	private static UUID loco = null;
	private static RemoteControlData cachedData = null;
    public static RemoteOverlay remoteGui;
	
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
        ItemWirelessRemoteControl.Data data = new ItemWirelessRemoteControl.Data(held);
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
    
    public static void applyLocalReadoutUpdate(Readouts readout, float value) {
        if (cachedData == null) {
            return;
        }
        switch (readout) {
            case THROTTLE -> cachedData.throttle = value;
            case REVERSER -> cachedData.reverser = value;
            case BRAKE_PRESSURE -> cachedData.brakePressure = value;
            case INDEPENDENT_BRAKE -> cachedData.indBrake = value;
            case EMERGENCY -> cachedData.emergency = value > 0.5;
            case WHISTLE, HORN -> cachedData.horn = value;
            case ENGINE -> cachedData.engine = value > 0.5;
            default -> throw new IllegalArgumentException("Unexpected value: " + readout);
        }
    }
}
