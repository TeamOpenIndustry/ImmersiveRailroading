package cam72cam.mod.input;

import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.*;
import java.util.function.Consumer;

public class Keyboard {
    private static Map<UUID, Vec3d> vecs = new HashMap<>();
    private static Map<String, Consumer<Player>> keyFuncs = new HashMap<>();

    /* Player Movement */


    public static Vec3d getMovement(Player player) {
        return vecs.getOrDefault(player.getUUID(), Vec3d.ZERO);
    }

    @Mod.EventBusSubscriber(Side.CLIENT)
    public static class KeyboardListener {
        static List<KeyBinding> keys = new ArrayList<>();

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

            for (KeyBinding key : keys) {
                if (key.isKeyDown()) {
                    new KeyPacket(key.getKeyDescription()).sendToServer();
                }
            }
        }
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

    /* Key Bindings */

    public static void registerKey(String name, int keyCode, String category, Consumer<Player> handler) {
        keyFuncs.put(name, handler);
        proxy.registerKey(name, keyCode, category);
    }

    @SidedProxy(clientSide = "cam72cam.mod.input.Keyboard$ClientProxy", serverSide = "cam72cam.mod.input.Keyboard$ServerProxy")
    public static Proxy proxy;

    public static abstract class Proxy {
        public abstract void registerKey(String name, int keyCode, String category);
    }

    public static class ClientProxy extends Proxy {
        @Override
        public void registerKey(String name, int keyCode, String category) {
            KeyBinding key = new KeyBinding(name, keyCode, category);
            ClientRegistry.registerKeyBinding(key);
            KeyboardListener.keys.add(key);
        }
    }

    public static class ServerProxy extends Proxy {
        @Override
        public void registerKey(String name, int keyCode, String category) {
            // NOP
        }
    }

    public static class KeyPacket extends Packet {
        public KeyPacket() {

        }
        public KeyPacket(String name) {
            data.setString("name", name);
        }

        @Override
        protected void handle() {
            keyFuncs.get(data.getString("name")).accept(getPlayer());
        }
    }
}
