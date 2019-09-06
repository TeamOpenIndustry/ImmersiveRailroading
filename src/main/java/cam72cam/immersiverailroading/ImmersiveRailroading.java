package cam72cam.immersiverailroading;

import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.proxy.CommonProxy;
import cam72cam.mod.ModCore;
import cam72cam.mod.gui.GuiRegistry;
import net.minecraftforge.fml.common.SidedProxy;

public class ImmersiveRailroading extends ModCore.Mod {
    public static final String MODID = "immersiverailroading";
	public static final String NAME = "ImmersiveRailroading";
    public static final String VERSION = "1.5.0";

	public static final int ENTITY_SYNC_DISTANCE = 512;
	public static ImmersiveRailroading instance;

	public static GuiRegistry GUI_REGISTRY = new GuiRegistry(ImmersiveRailroading.class);

    static {
    	ModCore.register(ImmersiveRailroading::new);
	}

	public ImmersiveRailroading() {
    	instance = this;

		proxy.preInit();
	}

	@Override
	public void setup()
	{
		proxy.init();
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
