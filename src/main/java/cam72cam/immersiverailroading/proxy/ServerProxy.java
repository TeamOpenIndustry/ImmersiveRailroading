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
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.mod.world.World;
import cam72cam.mod.entity.Player;
import cam72cam.mod.util.Identifier;
import net.minecraft.entity.player.EntityPlayer;
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
    public Object getClientGuiElement(int ID, EntityPlayer player, net.minecraft.world.World world, int entityID, int nop1, int nop2) {
    	return null;
    }

    private InputStream getEmbeddedResourceStream(Identifier location) throws IOException {
        URL url = ImmersiveRailroading.class.getResource(pathString(location, true));
		return url != null ? ImmersiveRailroading.class.getResourceAsStream(pathString(location, true)) : null;
    }

	@Override
	public List<InputStream> getResourceStreamAll(Identifier location) throws IOException {
		List<InputStream> res = new ArrayList<>();
		InputStream stream = getEmbeddedResourceStream(location);
		if (stream != null) {
			res.add(stream);
		}
		
		res.addAll(getFileResourceStreams(location));
		
		return res;
	}

	@Override
	public void addPreview(int dimension, TileRailPreview preview) {
		// NOP, never used
	}

	@SubscribeEvent
	public static void onEntityJoin(EntityJoinWorldEvent event) {
		EntityRollingStock stock = World.get(event.getWorld()).getEntity(event.getEntity().getUniqueID(), EntityRollingStock.class);
		if(stock != null) {
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
		Player player = new Player(event.player);

		if (logoffRide.containsKey(player.getUUID())) {
			EntityRidableRollingStock ent = player.getWorld().getEntity(logoffRide.get(player.getUUID()), EntityRidableRollingStock.class);
			if(ent != null) {
				ent.addPassenger(player);
			}
		}
	}
	
	@SubscribeEvent
	public static void onPlayerLeave(PlayerLoggedOutEvent event) {
		Player player = new Player(event.player);
		if (player.getRiding() instanceof EntityRidableRollingStock) {
			logoffRide.put(player.getUUID(), player.getRiding().getUUID());
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
