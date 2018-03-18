package cam72cam.immersiverailroading.proxy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.IOUtils;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.server.FMLServerHandler;

@EventBusSubscriber(Side.SERVER)
public class ServerProxy extends CommonProxy {
	private static int tickCount = 0;

	@Override
	public void init(FMLInitializationEvent event) {
		super.init(event);
	}
	
	@Override
	public void serverStarting(FMLServerStartingEvent event) {
		event.registerServerCommand(new ReloadResourcesCommand());
	}

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int entityID, int nop1, int nop2) {
    	return null;
    }

    @Override
	public World getWorld(int dimension)  {
		return FMLServerHandler.instance().getServer().getWorld(dimension);
	}
    
    private String pathString(ResourceLocation location, boolean startingSlash) {
    	return (startingSlash ? "/" : "") + "assets/" + location.getResourceDomain() + "/" + location.getResourcePath();
    }
    
    private InputStream getEmbeddedResourceStream(ResourceLocation location) throws IOException {
        URL url = ImmersiveRailroading.class.getResource(pathString(location, true));
		return url != null ? ImmersiveRailroading.class.getResourceAsStream(pathString(location, true)) : null;
    }
    
    private List<InputStream> getFileResourceStreams(ResourceLocation location) throws IOException {
    	List<InputStream> streams = new ArrayList<InputStream>();
    	File folder = new File(this.configDir);
    	if (folder.exists()) {
    		if (folder.isDirectory()) {
	    		File[] files = folder.listFiles(new FilenameFilter() {
				    @Override
				    public boolean accept(File dir, String name) {
				        return name.endsWith(".zip");
				    }
				});
	    		for (File file : files) {
	    			ZipFile resourcePack = new ZipFile(file);
	    			ZipEntry entry = resourcePack.getEntry(pathString(location, false));
	    			if (entry != null) {
	    				// Copy the input stream so we can close the resource pack
	    				InputStream stream = resourcePack.getInputStream(entry);
	    				streams.add(new ByteArrayInputStream(IOUtils.toByteArray(stream)));
	    			}
	    			resourcePack.close();
	    		}
    		} else {
    			ImmersiveRailroading.error("Expecting " + this.configDir + " to be a directory");
    		}
    	} else {
			folder.mkdirs();
    	}
		return streams;
    }

	@Override
	public InputStream getResourceStream(ResourceLocation location) throws IOException {
		InputStream chosen = null;
		for (InputStream strm : getResourceStreamAll(location)) {
			if (chosen == null) {
				chosen = strm;
			} else {
				strm.close();
			}
		}
		return chosen;
	}

	@Override
	public List<InputStream> getResourceStreamAll(ResourceLocation location) throws IOException {
		List<InputStream> res = getFileResourceStreams(location);
		
		InputStream stream = getEmbeddedResourceStream(location);
		if (stream != null) {
			res.add(stream);
		}
		
		return res;
	}
	
	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != Phase.START) {
			return;
		}
		tickCount++;
	}


	@Override
	public int getTicks() {
		return tickCount;
	}
}
