package cam72cam.immersiverailroading;

import java.io.IOException;

import org.apache.logging.log4j.Logger;

import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.proxy.ChunkManager;
import cam72cam.immersiverailroading.proxy.CommonProxy;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

@Mod(modid = ImmersiveRailroading.MODID, name="ImmersiveRailroading", version = ImmersiveRailroading.VERSION, acceptedMinecraftVersions = "[1.12,1.13)", dependencies = "required-after:trackapi@[1.1,);after:immersiveengineering")
public class ImmersiveRailroading
{
    public static final String MODID = "immersiverailroading";
    public static final String VERSION = "1.5.0";
	public static final int ENTITY_SYNC_DISTANCE = 512;
    
	private static Logger logger;
	public static ImmersiveRailroading instance;
	
	public static final SimpleNetworkWrapper net = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
	
	@SidedProxy(clientSide="cam72cam.immersiverailroading.proxy.ClientProxy", serverSide="cam72cam.immersiverailroading.proxy.ServerProxy")
	public static CommonProxy proxy;
	
	private ChunkManager chunker;
	
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) throws IOException {
        logger = event.getModLog();
        instance = this;
        
        World.MAX_ENTITY_RADIUS = Math.max(World.MAX_ENTITY_RADIUS, 32);
        
    	proxy.preInit(event);
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    	proxy.init(event);
    }
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event) throws IOException {
		chunker = new ChunkManager();
		chunker.init();
    }
    
    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
    	proxy.serverStarting(event);
    }
    
    public static void debug(String msg, Object...params) {
    	if (logger == null) {
    		System.out.println("DEBUG: " + String.format(msg, params));
    		return;
    	}
    	
    	if (ConfigDebug.debugLog) {
    		logger.info(String.format(msg, params));
    	}
    }
    public static void info(String msg, Object...params) {
    	if (logger == null) {
    		System.out.println("INFO: " + String.format(msg, params));
    		return;
    	}
    	
    	logger.info(String.format(msg, params));
    }
    public static void warn(String msg, Object...params) {
    	if (logger == null) {
    		System.out.println("WARN: " + String.format(msg, params));
    		return;
    	}
    	
    	logger.warn(String.format(msg, params));
    }
    public static void error(String msg, Object...params) {
    	if (logger == null) {
    		System.out.println("ERROR: " + String.format(msg, params));
    		return;
    	}
    	
    	logger.error(String.format(msg, params));
    }
	public static void catching(Throwable ex) {
    	if (logger == null) {
    		ex.printStackTrace();
    		return;
    	}
    	
		logger.catching(ex);
	}
}
