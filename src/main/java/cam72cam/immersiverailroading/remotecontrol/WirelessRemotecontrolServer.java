package cam72cam.immersiverailroading.remotecontrol;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.net.RemoteControlSyncPacket;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.world.World;

public class WirelessRemotecontrolServer {
	private static final Map<UUID, UUID> playerToLoco = new HashMap<>();
	
	private WirelessRemotecontrolServer() {
	}
	
    public static void init() {
        World.onTick(world -> {
            if (world.isClient) {
                return;
            }
            onServerTick(world);
        });
    }
    
    public static void setActive(UUID player, UUID loco) {
        if (loco == null) {
            playerToLoco.remove(player);
        } else {
            playerToLoco.put(player, loco);
        }
    }
    
    private static void onServerTick(World world) {
        if (world.getTicks() % 10 != 0) {
            return;
        }
        playerToLoco.forEach((playerUUID, locoUUID) -> {
            Entity entity = world.getEntity(locoUUID, LocomotiveDiesel.class);
            Player player = world.getEntity(playerUUID, Player.class);
            if (entity instanceof LocomotiveDiesel loco && player != null) {
                new RemoteControlSyncPacket(loco.getRemoteControlData()).sendToPlayer(player);
            }
        });
    }

}
