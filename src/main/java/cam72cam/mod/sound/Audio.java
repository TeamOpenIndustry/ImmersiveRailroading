package cam72cam.mod.sound;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

public class Audio {

    @SidedProxy(clientSide="cam72cam.mod.sound.Audio$ClientProxy", serverSide="cam72cam.mod.sound.Audio$ServerProxy")
    public static IAudioProxy proxy;

    public static void playSound(Vec3d pos, StandardSound sound, SoundCategory category, float volume, float pitch) {
        proxy.playSound(pos, sound, category, volume, pitch);
    }
    public static void playSound(Vec3i pos, StandardSound sound, SoundCategory category, float volume, float pitch) {
        proxy.playSound(new Vec3d(pos), sound, category, volume, pitch);
    }

    public static ISound newSound(Identifier oggLocation, boolean repeats, float attenuationDistance, float scale) {
        return proxy.loadSound(oggLocation, repeats, attenuationDistance, scale);
    }

    private interface IAudioProxy {
        ISound loadSound(Identifier oggLocation, boolean repeats, float attenuationDistance, float scale);
        void playSound(Vec3d pos, StandardSound sound, SoundCategory category, float volume, float pitch);
    }

    public static class ServerProxy implements IAudioProxy {
        @Override
        public ISound loadSound(Identifier oggLocation, boolean repeats, float attenuationDistance, float scale) {
            throw new RuntimeException("Unable to play audio directly on the server...");
        }

        @Override
        public void playSound(Vec3d pos, StandardSound sound, SoundCategory category, float volume, float pitch) {
            //Same as server world in MC
        }
    }

    @Mod.EventBusSubscriber(Side.CLIENT)
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
                soundManager.tick();
            }

            if (world == null && soundManager != null && soundManager.hasSounds()) {
                soundManager.stop();
            }
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

        public ISound loadSound(Identifier oggLocation, boolean repeats, float attenuationDistance, float scale) {
            return soundManager.createSound(oggLocation, repeats, attenuationDistance, scale);
        }

        @Override
        public void playSound(Vec3d pos, StandardSound sound, SoundCategory category, float volume, float pitch) {
            MinecraftClient.getPlayer().getWorld().internal.playSound(pos.x, pos.y, pos.z, sound.event, category.category, volume, pitch, false);
        }
    }
}
