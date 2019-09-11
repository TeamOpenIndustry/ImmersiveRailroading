package cam72cam.immersiverailroading.proxy;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;

@EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {
	private static String missingResources;

	@SubscribeEvent
	public static void onEntityJoin(EntityJoinWorldEvent event) {
		if (event.getWorld() == null || World.get(event.getWorld()) == null) {
			return;
		}
		// TODO this does not actually work!
		World world = World.get(event.getWorld());
		EntityRollingStock stock = world.getEntity(event.getEntity().getUniqueID(), EntityRollingStock.class);
		
		if(stock != null) {
			String defID = stock.getDefinitionID();
			EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
			if (def == null) {
				String error = String.format("Missing definition %s, do you have all of the required resource packs?", defID);
				ImmersiveRailroading.error(error);
				event.setCanceled(true);
				
				if (!Minecraft.getMinecraft().isSingleplayer()) {
					missingResources = error;
				}
			}
		}
	}
	
	@SubscribeEvent
	public static void onHackRenderEvent(RenderWorldLastEvent event) {
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != Phase.START) {
			return;
		}

		if (missingResources != null) {
			Minecraft.getMinecraft().getConnection().getNetworkManager().closeChannel(PlayerMessage.direct(missingResources).internal);
			Minecraft.getMinecraft().loadWorld(null);
			Minecraft.getMinecraft().displayGuiScreen(new GuiDisconnected(new GuiMultiplayer(new GuiMainMenu()), "disconnect.lost", PlayerMessage.direct(missingResources).internal));
			missingResources = null;
		}
	}

	@SubscribeEvent
	public static void configChanged(OnConfigChangedEvent event) {
		if (event.getModID().equals(ImmersiveRailroading.MODID)) {
			ConfigManager.sync(ImmersiveRailroading.MODID, net.minecraftforge.common.config.Config.Type.INSTANCE);
		}
	}
}
