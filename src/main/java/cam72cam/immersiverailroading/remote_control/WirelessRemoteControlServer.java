package cam72cam.immersiverailroading.remote_control;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.net.RemoteControlServerPacket;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.EntityPool;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.world.World;

public class WirelessRemoteControlServer {
	private static final Map<UUID, UUID> playerToLoco = new HashMap<>();
	
	private WirelessRemoteControlServer() {
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
    
    public static boolean isActive(UUID player, UUID loco) {
        return loco != null && loco.equals(playerToLoco.get(player));
    }

    private static void onServerTick(World world) {
        if (world.getTicks() % 10 != 0) {
            return;
        }
        playerToLoco.forEach((playerUUID, locoUUID) -> {
            // 优先 pool.all，和 Simulation.loaded 的优先级一致。
            // Simulation 用 pool.all 的 self 算物理，遥控必须改同一个 self 的字段，
            // 否则遥控改 A、物理读 B，永远不一致。
            Locomotive loco = null;
            ModdedEntity pooled = EntityPool.get(world).getEntity(locoUUID);
            if (pooled != null && pooled.getSelf() instanceof Locomotive l) {
                loco = l;
            } else {
                Entity entity = world.getEntity(locoUUID, Locomotive.class);
                if (entity instanceof Locomotive l) {
                    loco = l;
                }
            }

            Player player = world.getEntity(playerUUID, Player.class);
            if (loco != null && player != null) {
                loco.keepLoaded();
                new RemoteControlServerPacket(loco.getRemoteControlData()).sendToPlayer(player);
            }
        });
    }

}
