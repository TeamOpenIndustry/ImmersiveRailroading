package cam72cam.mod;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.proxy.IRCommand;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@net.minecraftforge.fml.common.Mod(modid=ImmersiveRailroading.MODID, name=ImmersiveRailroading.NAME, version=ImmersiveRailroading.VERSION, acceptedMinecraftVersions = "[1.12,1.13)")
public class ModCore {
    private static List<Supplier<Mod>> modCtrs = new ArrayList<>();
    private static List<Runnable> onInit = new ArrayList<>();

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
        protected abstract void setup();
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        System.out.println("Welcome to ModCore!");

        instance = this;
        logger = event.getModLog();

        World.MAX_ENTITY_RADIUS = Math.max(World.MAX_ENTITY_RADIUS, 32);

        mods = modCtrs.stream().map(Supplier::get).collect(Collectors.toList());
        mods.forEach(Mod::init);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        mods.forEach(Mod::setup);
        onInit.forEach(Runnable::run);
    }

    public static void onInit(Class<? extends Mod> type, Consumer<Mod> fn) {
        onInit.add(() -> instance.mods.stream().filter(type::isInstance).findFirst().ifPresent(fn));
    }


    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new IRCommand());
    }
}
