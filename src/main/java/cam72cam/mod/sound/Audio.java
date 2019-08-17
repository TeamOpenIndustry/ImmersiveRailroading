package cam72cam.mod.sound;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Audio {

    @SidedProxy(clientSide="cam72cam.mod.sound.Audio$ClientProxy", serverSide="cam72cam.mod.sound.Audio$ServerProxy")
    public static IAudioProxy proxy;

    public static ISound newSound(Identifier oggLocation, boolean repeats, float attenuationDistance, float scale) {
        return proxy.newSound(oggLocation, repeats, attenuationDistance, scale);
    }

    private interface IAudioProxy {
        ISound newSound(Identifier oggLocation, boolean repeats, float attenuationDistance, float scale);
    }

    public static class ServerProxy implements IAudioProxy {
        @Override
        public ISound newSound(Identifier oggLocation, boolean repeats, float attenuationDistance, float scale) {
            throw new RuntimeException("Unable to play audio directly on the server...");
        }
    }

    @Mod.EventBusSubscriber
    public static class ClientProxy implements IAudioProxy {
        private static ModSoundManager soundManager;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.START) {
                return;
            }

            Player player = MinecraftClient.getPlayer();
            World world = null;
            if (player != null) {
                world = player.getWorld();
            }

            if (world == null && soundManager != null && soundManager.hasSounds()) {
                soundManager.stop();
            }

            soundManager.tick();

        }

        @SubscribeEvent
        public static void onSoundLoad(SoundLoadEvent event) {
            if (soundManager == null) {
                soundManager = new ModSoundManager(event.getManager());
            } else {
                soundManager.handleReload(false);
            }
        }

        @SubscribeEvent
        public static void onWorldLoad(WorldEvent.Load event) {
            soundManager.handleReload(true);
        }

        @SubscribeEvent
        public static void onWorldUnload(WorldEvent.Unload event) {
            soundManager.stop();
        }

        public ISound newSound(Identifier oggLocation, boolean repeats, float attenuationDistance, float scale) {
            return soundManager.createSound(oggLocation, repeats, attenuationDistance, scale);
        }
    }
}
