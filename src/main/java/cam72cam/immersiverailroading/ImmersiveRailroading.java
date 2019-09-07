package cam72cam.immersiverailroading;

import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.multiblock.*;
import cam72cam.immersiverailroading.proxy.CommonProxy;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.thirdparty.CompatLoader;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.EntityRegistry;
import cam72cam.mod.gui.GuiRegistry;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.sound.Audio;
import cam72cam.mod.sound.ISound;
import net.minecraftforge.fml.common.SidedProxy;

import java.io.IOException;

public class ImmersiveRailroading extends ModCore.Mod {
    public static final String MODID = "immersiverailroading";
	public static final String NAME = "ImmersiveRailroading";
    public static final String VERSION = "1.5.0";

	public static final int ENTITY_SYNC_DISTANCE = 512;
	public static ImmersiveRailroading instance;

	public static GuiRegistry GUI_REGISTRY = new GuiRegistry(ImmersiveRailroading.class);

    static {
    	ModCore.register(ImmersiveRailroading::new);

		EntityRegistry.register(ImmersiveRailroading.class, CarFreight::new, EntityRollingStock.settings, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);
		EntityRegistry.register(ImmersiveRailroading.class, CarPassenger::new, EntityRollingStock.settings, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);
		EntityRegistry.register(ImmersiveRailroading.class, CarTank::new, EntityRollingStock.settings, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);
		EntityRegistry.register(ImmersiveRailroading.class, HandCar::new, EntityRollingStock.settings, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);
		EntityRegistry.register(ImmersiveRailroading.class, LocomotiveDiesel::new, EntityRollingStock.settings, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);
		EntityRegistry.register(ImmersiveRailroading.class, LocomotiveSteam::new, EntityRollingStock.settings, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);
		EntityRegistry.register(ImmersiveRailroading.class, Tender::new, EntityRollingStock.settings, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);

		MultiblockRegistry.register(SteamHammerMultiblock.NAME, new SteamHammerMultiblock());
		MultiblockRegistry.register(PlateRollerMultiblock.NAME, new PlateRollerMultiblock());
		MultiblockRegistry.register(RailRollerMultiblock.NAME, new RailRollerMultiblock());
		MultiblockRegistry.register(BoilerRollerMultiblock.NAME, new BoilerRollerMultiblock());
		MultiblockRegistry.register(CastingMultiblock.NAME, new CastingMultiblock());
	}

	public ImmersiveRailroading() {
    	instance = this;

		try {
			DefinitionManager.initDefinitions();
		} catch (IOException e) {
			throw new RuntimeException("Unable to load IR definitions", e);
		}

		proxy.preInit();
	}

	@Override
	public String modID() {
		return MODID;
	}

	@Override
	public void setup()
	{
		proxy.init();

		IRFuzzy.applyFallbacks();
		CompatLoader.load();
	}

	public static ISound newSound(Identifier oggLocation, boolean repeats, float attenuationDistance, Gauge gauge) {
		return Audio.newSound(oggLocation, repeats, (float) (attenuationDistance * gauge.scale() * ConfigSound.soundDistanceScale), (float)Math.sqrt(Math.sqrt(gauge.scale())));
	}

	@SidedProxy(clientSide="cam72cam.immersiverailroading.proxy.ClientProxy", serverSide="cam72cam.immersiverailroading.proxy.ServerProxy")
	public static CommonProxy proxy;

    public static void debug(String msg, Object...params) {
    	if (instance.logger == null) {
    		System.out.println("DEBUG: " + String.format(msg, params));
    		return;
    	}
    	
    	if (ConfigDebug.debugLog) {
    		instance.logger.info(String.format(msg, params));
    	}
    }
    public static void info(String msg, Object...params) {
    	if (instance.logger == null) {
    		System.out.println("INFO: " + String.format(msg, params));
    		return;
    	}
    	
    	instance.logger.info(String.format(msg, params));
    }
    public static void warn(String msg, Object...params) {
    	if (instance.logger == null) {
    		System.out.println("WARN: " + String.format(msg, params));
    		return;
    	}
    	
    	instance.logger.warn(String.format(msg, params));
    }
    public static void error(String msg, Object...params) {
    	if (instance.logger == null) {
    		System.out.println("ERROR: " + String.format(msg, params));
    		return;
    	}
    	
    	instance.logger.error(String.format(msg, params));
    }
	public static void catching(Throwable ex) {
    	if (instance.logger == null) {
    		ex.printStackTrace();
    		return;
    	}
    	
		instance.logger.catching(ex);
	}
}
