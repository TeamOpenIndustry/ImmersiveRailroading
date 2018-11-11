package cam72cam.immersiverailroading.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityRidableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.server.FMLServerHandler;

@EventBusSubscriber(Side.SERVER)
public class ServerProxy extends CommonProxy {
	private static int tickCount = 0;
	private static Map<UUID, UUID> logoffRide = new HashMap<UUID, UUID>();

	@Override
	public void preInit(FMLPreInitializationEvent event) throws IOException {
		super.preInit(event);
		
		for (EntityRollingStockDefinition def : DefinitionManager.getDefinitions()) {
			def.clearModel();
		}
	}
	
	@Override
	public void init(FMLInitializationEvent event) {
		super.init(event);
	}

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int entityID, int nop1, int nop2) {
    	return null;
    }

    @Override
	public World getWorld(int dimension)  {
		return FMLServerHandler.instance().getServer().getWorld(dimension);
	}
    
    private InputStream getEmbeddedResourceStream(ResourceLocation location) throws IOException {
        URL url = ImmersiveRailroading.class.getResource(pathString(location, true));
		return url != null ? ImmersiveRailroading.class.getResourceAsStream(pathString(location, true)) : null;
    }

	@Override
	public List<InputStream> getResourceStreamAll(ResourceLocation location) throws IOException {
		List<InputStream> res = new ArrayList<InputStream>();
		InputStream stream = getEmbeddedResourceStream(location);
		if (stream != null) {
			res.add(stream);
		}
		
		res.addAll(getFileResourceStreams(location));
		
		return res;
	}
	
	@SubscribeEvent
	public static void onEntityJoin(EntityJoinWorldEvent event) {
		if(event.getEntity() instanceof EntityRollingStock) {
			EntityRollingStock stock = (EntityRollingStock)event.getEntity();
			String defID = stock.getDefinitionID();
			EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
			if (def == null) {
				String error = String.format("Missing definition %s, do you have all of the required resource packs?", defID);
				ImmersiveRailroading.error(error);
				event.setCanceled(true);
			}
		}
	}
	
	@SubscribeEvent
	public static void onPlayerJoin(PlayerLoggedInEvent event) {
		EntityPlayer player = event.player;
		World world = player.world;
		
		if (logoffRide.containsKey(player.getUniqueID())) {
			for (Entity ent: world.loadedEntityList) {
				if (ent.getUniqueID() == logoffRide.get(player.getUniqueID())) {
					System.out.println("WOOO");
					player.startRiding(ent, true);
				}
			}
		}
	}
	
	@SubscribeEvent
	public static void onPlayerLeave(PlayerLoggedOutEvent event) {
		EntityPlayer player = event.player;
		if (player.getRidingEntity() instanceof EntityRidableRollingStock) {
			logoffRide.put(player.getUniqueID(), player.getRidingEntity().getUniqueID());		
		}
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
