package cam72cam.immersiverailroading.proxy;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityRidableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.EntitySmokeParticle;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.net.KeyPressPacket;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.render.ExpireableList;
import cam72cam.immersiverailroading.render.RenderCacheTimeLimiter;
import cam72cam.immersiverailroading.render.entity.ParticleRender;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {
	private static Map<KeyTypes, KeyBinding> keys = new HashMap<KeyTypes, KeyBinding>();
	private static Map<Integer, ExpireableList<BlockPos, TileRailPreview>> previews = new HashMap<>();

	public static RenderCacheTimeLimiter renderCacheLimiter = new RenderCacheTimeLimiter();

	private static String missingResources;

	@Override
	public void init() {
		super.init();
		
		keys.put(KeyTypes.THROTTLE_UP, new KeyBinding("ir_keys.increase_throttle", Keyboard.KEY_NUMPAD8, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.THROTTLE_ZERO, new KeyBinding("ir_keys.zero_throttle", Keyboard.KEY_NUMPAD5, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.THROTTLE_DOWN, new KeyBinding("ir_keys.decrease_throttle", Keyboard.KEY_NUMPAD2, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.AIR_BRAKE_UP, new KeyBinding("ir_keys.increase_brake", Keyboard.KEY_NUMPAD7, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.AIR_BRAKE_ZERO, new KeyBinding("ir_keys.zero_brake", Keyboard.KEY_NUMPAD4, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.AIR_BRAKE_DOWN, new KeyBinding("ir_keys.decrease_brake", Keyboard.KEY_NUMPAD1, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.HORN, new KeyBinding("ir_keys.horn", Keyboard.KEY_NUMPADENTER, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.DEAD_MANS_SWITCH, new KeyBinding("ir_keys.dead_mans_switch", Keyboard.KEY_NUMPADEQUALS, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.START_STOP_ENGINE, new KeyBinding("ir_keys.start_stop_engine", Keyboard.KEY_ADD, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.BELL, new KeyBinding("ir_keys.bell", Keyboard.KEY_SUBTRACT, "key.categories." + ImmersiveRailroading.MODID));

		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.THROTTLE_UP));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.THROTTLE_DOWN));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.THROTTLE_ZERO));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.AIR_BRAKE_UP));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.AIR_BRAKE_DOWN));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.AIR_BRAKE_ZERO));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.HORN));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.BELL));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.DEAD_MANS_SWITCH));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.START_STOP_ENGINE));
	}


	public static final IRenderFactory<EntitySmokeParticle> PARTICLE_RENDER = ParticleRender::new;

	@SubscribeEvent
	public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
		RenderingRegistry.registerEntityRenderingHandler(EntitySmokeParticle.class, PARTICLE_RENDER);
	}

	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event) {
		OBJLoader.INSTANCE.addDomain(ImmersiveRailroading.MODID.toLowerCase());

		ModelLoader.setCustomModelResourceLocation(IRItems.ITEM_LARGE_WRENCH.internal, 0,
				new ModelResourceLocation(IRItems.ITEM_LARGE_WRENCH.getRegistryName().internal, ""));

		ModelLoader.setCustomModelResourceLocation(IRItems.ITEM_HOOK.internal, 0,
				new ModelResourceLocation(IRItems.ITEM_HOOK.getRegistryName().internal, ""));
		
		ModelLoader.setCustomModelResourceLocation(IRItems.ITEM_CONDUCTOR_WHISTLE.internal, 0,
				new ModelResourceLocation(IRItems.ITEM_CONDUCTOR_WHISTLE.getRegistryName().internal, ""));

		ModelLoader.setCustomModelResourceLocation(IRItems.ITEM_MANUAL.internal, 0,
				new ModelResourceLocation("minecraft:written_book", ""));
		
		ModelLoader.setCustomModelResourceLocation(IRItems.ITEM_PAINT_BRUSH.internal, 0,
				new ModelResourceLocation(IRItems.ITEM_PAINT_BRUSH.getRegistryName().internal, ""));

		ModelLoader.setCustomModelResourceLocation(IRItems.ITEM_GOLDEN_SPIKE.internal, 0,
				new ModelResourceLocation(IRItems.ITEM_GOLDEN_SPIKE.getRegistryName().internal, ""));

		ModelLoader.setCustomModelResourceLocation(IRItems.ITEM_RADIO_CONTROL_CARD.internal, 0,
				new ModelResourceLocation(IRItems.ITEM_RADIO_CONTROL_CARD.getRegistryName().internal, ""));

		ModelLoader.setCustomModelResourceLocation(IRItems.ITEM_SWITCH_KEY.internal, 0,
				new ModelResourceLocation(IRItems.ITEM_SWITCH_KEY.getRegistryName().internal, ""));
	}

	@SubscribeEvent
	public static void onKeyInput(ClientTickEvent event) {
		Player player = MinecraftClient.getPlayer();
		if (player == null) {
			return;
		}
		if (!(player.getRiding() instanceof Locomotive)) {
			return;
		}

		Locomotive riding = (Locomotive) player.getRiding();
		
		for (KeyTypes key : keys.keySet()) {
			KeyBinding binding = keys.get(key);
			if (binding.isKeyDown()) {
				new KeyPressPacket(key, player, riding).sendToServer();
			}
		}
	}
	
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
		renderCacheLimiter.reset();
	}
	
	@SubscribeEvent
	public static void onOverlayEvent(RenderGameOverlayEvent.Pre event) {
		if (event.getType() == ElementType.ALL) {
		}
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

		//TODO clear sndCache
	}

	public void addPreview(int dimension, TileRailPreview preview) {
		/* TODO HACKS
		if (!previews.containsKey(dimension)) {
			previews.put(dimension, new ExpireableList<BlockPos, TileRailPreview>() {
				@Override
				public int lifespan() {
					return 2;
				}
				@Override
				public boolean sliding() {
					return false;
				}
			});
		}
		ExpireableList<BlockPos, TileRailPreview> pvs = previews.get(dimension);
		TileRailPreview curr = pvs.get(preview.pos.internal);
		if (curr != null) {
			if (curr.writeToNBT(new NBTTagCompound()).equals(preview.writeToNBT(new NBTTagCompound()))) {
				preview = curr;
			}
		}
		previews.get(dimension).put(preview.pos.internal, preview);
		*/
	}
	public Collection<TileRailPreview> getPreviews() {
		ExpireableList<BlockPos, TileRailPreview> pvs = previews.get(Minecraft.getMinecraft().player.dimension);
		if (pvs != null) {
			return pvs.values();
		}
		return null;
	}

	@SubscribeEvent
	public static void configChanged(OnConfigChangedEvent event) {
		if (event.getModID().equals(ImmersiveRailroading.MODID)) {
			ConfigManager.sync(ImmersiveRailroading.MODID, net.minecraftforge.common.config.Config.Type.INSTANCE);
		}
	}
}
