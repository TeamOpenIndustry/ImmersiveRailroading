package cam72cam.mod.input;

import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Keyboard {
    private static Map<UUID, Vec3d> vecs = new HashMap<>();

    public static Vec3d getMovement(Player player) {
        return vecs.getOrDefault(player.getUUID(), Vec3d.ZERO);
    }

    @Mod.EventBusSubscriber(Side.CLIENT)
    public static class onKeyboard {
        @SubscribeEvent
        public static void onKeyInput(TickEvent.ClientTickEvent event) {
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            if (player == null) {
                return;
            }
            new MovementPacket(
                    player.getUniqueID(),
                    new Vec3d(player.moveForward, 0, player.moveStrafing).scale(player.isSprinting() ? 0.4 : 0.2)
            ).sendToServer();
        }
    }

    static {
        Packet.register(MovementPacket::new, PacketDirection.ClientToServer);
    }

    public static class MovementPacket extends Packet {
        public MovementPacket() {

        }
        public MovementPacket(UUID id, Vec3d move) {
            data.setUUID("id", id);
            data.setVec3d("move", move);
            vecs.put(data.getUUID("id"), data.getVec3d("move"));
        }
        @Override
        protected void handle() {
            vecs.put(data.getUUID("id"), data.getVec3d("move"));
        }
    }
}
