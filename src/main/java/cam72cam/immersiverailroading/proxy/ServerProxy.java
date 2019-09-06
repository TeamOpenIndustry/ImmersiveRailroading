package cam72cam.immersiverailroading.proxy;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityRidableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.mod.entity.Player;
import cam72cam.mod.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(Side.SERVER)
public class ServerProxy extends CommonProxy {
	private static int tickCount = 0;
	private static Map<UUID, UUID> logoffRide = new HashMap<UUID, UUID>();

	@Override
	public void preInit() {
		super.preInit();
		
		for (EntityRollingStockDefinition def : DefinitionManager.getDefinitions()) {
			def.clearModel();
		}
	}
	
	@Override
	public void init() {
		super.init();
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
