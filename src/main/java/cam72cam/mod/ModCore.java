package cam72cam.mod;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.sync.EntitySync;
import cam72cam.mod.input.Keyboard;
import cam72cam.mod.input.MousePressPacket;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import cam72cam.mod.render.BlockRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@net.minecraftforge.fml.common.Mod(modid=ImmersiveRailroading.MODID, name=ImmersiveRailroading.NAME, version=ImmersiveRailroading.VERSION, acceptedMinecraftVersions = "[1.12,1.13)")
public class ModCore {
    private static List<Supplier<Mod>> modCtrs = new ArrayList<>();
    private static List<Runnable> onInit = new ArrayList<>();
    private static List<Runnable> onReload = new ArrayList<>();
    private static List<Runnable> onServerStarting = new ArrayList<>();

    private List<Mod> mods;

    public static ModCore instance;
    private Logger logger;

    public static void register(Supplier<Mod> ctr) {
        modCtrs.add(ctr);
    }

    public static abstract class Mod {
        public ModCore instance;
        protected Logger logger;

        private void init() {
            logger = ModCore.instance.logger;
            instance = ModCore.instance;
        }

        public abstract String modID();

        protected void initClient() {}
        protected void initServer() {}

        protected abstract void setup();
        protected void setupClient() {}
        protected void setupServer() {}

        protected abstract void finalize();


        public final Path getConfig(String fname) {
            return Paths.get(Loader.instance().getConfigDir().toString(), fname);
        }
    }

    static {
        Packet.register(EntitySync.EntitySyncPacket::new, PacketDirection.ServerToClient);
        Packet.register(Keyboard.MovementPacket::new, PacketDirection.ClientToServer);
        Packet.register(Keyboard.KeyPacket::new, PacketDirection.ClientToServer);
        Packet.register(ModdedEntity.PassengerPositionsPacket::new, PacketDirection.ServerToClient);
        Packet.register(MousePressPacket::new, PacketDirection.ClientToServer);
        ImmersiveRailroading.forceInit();
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        System.out.println("Welcome to ModCore!");

        instance = this;
        logger = event.getModLog();

        World.MAX_ENTITY_RADIUS = Math.max(World.MAX_ENTITY_RADIUS, 32);

        mods = modCtrs.stream().map(Supplier::get).collect(Collectors.toList());
        mods.forEach(Mod::init);
        proxy.init();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        mods.forEach(Mod::setup);
        onInit.forEach(Runnable::run);
        proxy.setup();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        mods.forEach(Mod::finalize);
    }

    @EventHandler
    public void serverStarting(FMLServerStartedEvent event) {
        onServerStarting.forEach(Runnable::run);
    }

    public static void onInit(Class<? extends Mod> type, Consumer<Mod> fn) {
        onInit.add(() -> instance.mods.stream().filter(type::isInstance).findFirst().ifPresent(fn));
    }

    public static void onReload(Runnable fn) {
        onReload.add(fn);
    }

    public static void onServerStarting(Runnable fn) {
        onServerStarting.add(fn);
    }

    public static abstract class Proxy {
        public abstract void init();
        public abstract void setup();
    }

    public static class ClientProxy extends Proxy {
        public static boolean skipFirst = true;

        @Override
        public void init() {
            instance.mods.forEach(Mod::initClient);
        }

        @Override
        public void setup() {
            instance.mods.forEach(Mod::setupClient);

            ((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(resourceManager -> {
                if (skipFirst) {
                    skipFirst = false;
                    return;
                }
                onReload.forEach(Runnable::run);
            });
            BlockRender.onPostColorSetup();
        }
    }

    public static class ServerProxy extends Proxy {
        @Override
        public void init() {
            instance.mods.forEach(Mod::initServer);
        }

        @Override
        public void setup() {
            instance.mods.forEach(Mod::setupServer);
        }
    }

    @SidedProxy(serverSide = "cam72cam.mod.ModCore$ServerProxy", clientSide = "cam72cam.mod.ModCore$ClientProxy")
    private static Proxy proxy;
}
