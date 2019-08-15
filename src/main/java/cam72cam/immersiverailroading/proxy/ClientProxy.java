package cam72cam.immersiverailroading.proxy;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.gui.*;
import cam72cam.immersiverailroading.gui.overlay.DieselLocomotiveOverlay;
import cam72cam.immersiverailroading.gui.overlay.HandCarOverlay;
import cam72cam.immersiverailroading.gui.overlay.SteamLocomotiveOverlay;
import cam72cam.immersiverailroading.items.nbt.ItemMultiblockType;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.net.KeyPressPacket;
import cam72cam.immersiverailroading.net.MousePressPacket;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.render.ExpireableList;
import cam72cam.immersiverailroading.render.RenderCacheTimeLimiter;
import cam72cam.immersiverailroading.render.StockRenderCache;
import cam72cam.immersiverailroading.render.block.RailBaseModel;
import cam72cam.immersiverailroading.render.entity.ParticleRender;
import cam72cam.immersiverailroading.render.entity.RenderOverride;
import cam72cam.immersiverailroading.render.item.*;
import cam72cam.immersiverailroading.render.multiblock.MBBlueprintRender;
import cam72cam.immersiverailroading.render.multiblock.TileMultiblockRender;
import cam72cam.immersiverailroading.render.rail.RailPreviewRender;
import cam72cam.immersiverailroading.render.rail.RailRenderUtil;
import cam72cam.immersiverailroading.sound.IRSoundManager;
import cam72cam.immersiverailroading.sound.ISound;
import cam72cam.immersiverailroading.tile.*;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.*;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.Hand;
import cam72cam.mod.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityEvent.EnteringChunk;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent.Load;
import net.minecraftforge.event.world.WorldEvent.Unload;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;
import paulscode.sound.SoundSystemConfig;

import java.io.IOException;
import java.util.*;

@EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {
	private static Map<KeyTypes, KeyBinding> keys = new HashMap<KeyTypes, KeyBinding>();
	private static Map<Integer, ExpireableList<BlockPos, TileRailPreview>> previews = new HashMap<>();
	private static ExpireableList<String, RailInfo> infoCache = new ExpireableList<>();

	private static IRSoundManager manager;
	
	public static RenderCacheTimeLimiter renderCacheLimiter = new RenderCacheTimeLimiter();

	private static String missingResources;
	private static float dampeningAmount = 1.0f;
	
	public static float getDampeningAmount() {
		return dampeningAmount;
	}
	
	public static void dampenSound() {
		Player player = MinecraftClient.getPlayer();
		dampeningAmount = 1.0f;
		if (player != null && player.getRiding() instanceof EntityRidableRollingStock) {
			EntityRidableRollingStock ridableStock = (EntityRidableRollingStock) player.getRiding();
			dampeningAmount = ridableStock.getDefinition().dampeningAmount;
		}
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, net.minecraft.world.World worldIn, int entityIDorPosX, int posY, int posZ) {
		World world = World.get(worldIn);
		TileMultiblock te;
		switch (GuiTypes.values()[ID]) {
		case FREIGHT:
			return new FreightContainerGui(world.getEntity(entityIDorPosX, CarFreight.class),
					new FreightContainer(player.inventory, world.getEntity(entityIDorPosX, CarFreight.class)));
		case TANK:
		case DIESEL_LOCOMOTIVE:
			return new TankContainerGui(world.getEntity(entityIDorPosX, FreightTank.class),
					new TankContainer(player.inventory, world.getEntity(entityIDorPosX, FreightTank.class)));
		case TENDER:
			return new TenderContainerGui(world.getEntity(entityIDorPosX, Tender.class),
					new TenderContainer(player.inventory, world.getEntity(entityIDorPosX, Tender.class)));
		case STEAM_LOCOMOTIVE:
			return new SteamLocomotiveContainerGui(world.getEntity(entityIDorPosX, LocomotiveSteam.class),
					new SteamLocomotiveContainer(player.inventory, world.getEntity(entityIDorPosX, LocomotiveSteam.class)));
		case RAIL:
			return new TrackGui();
		case RAIL_PREVIEW:
			return new TrackGui(world, entityIDorPosX, posY, posZ);
		case STEAM_HAMMER:
			te = world.getBlockEntity(new Vec3i(entityIDorPosX, posY, posZ), TileMultiblock.class);
			if (te == null || !te.isLoaded()) {
				return null;
			}
			return new SteamHammerContainerGui(new SteamHammerContainer(player.inventory, te));
		case PLATE_ROLLER:
			te = world.getBlockEntity(new Vec3i(entityIDorPosX, posY, posZ), TileMultiblock.class);
			if (te == null || !te.isLoaded()) {
				return null;
			}
			return new PlateRollerGUI(te);
		case CASTING:
			te = world.getBlockEntity(new Vec3i(entityIDorPosX, posY, posZ), TileMultiblock.class);
			if (te == null || !te.isLoaded()) {
				return null;
			}
			return new CastingGUI(te);
		default:
			return null;
		}
	}
	
	@Override
	public void preInit(FMLPreInitializationEvent event) throws IOException {
		super.preInit(event);
		if (ConfigSound.overrideSoundChannels) {
			SoundSystemConfig.setNumberNormalChannels(Math.max(SoundSystemConfig.getNumberNormalChannels(), 300));
		}
		
		if (Loader.isModLoaded("igwmod")) {
			FMLInterModComms.sendMessage("igwmod", "cam72cam.immersiverailroading.thirdparty.IGWMod", "init");
		}

		BlockRender.register(IRBlocks.BLOCK_RAIL, RailBaseModel::getModel, Rail.class);
		BlockRender.register(IRBlocks.BLOCK_RAIL_GAG, RailBaseModel::getModel, RailGag.class);
		BlockRender.register(IRBlocks.BLOCK_RAIL_PREVIEW, RailPreviewRender::render, TileRailPreview.class);
		BlockRender.register(IRBlocks.BLOCK_MULTIBLOCK, TileMultiblockRender::render, TileMultiblock.class);

		ItemRender.register(IRItems.ITEM_PLATE, PlateItemModel::getModel);
		ItemRender.register(IRItems.ITEM_AUGMENT, RailAugmentItemModel::getModel);
		ItemRender.register(IRItems.ITEM_RAIL, RailItemRender::getModel);
		ItemRender.register(IRItems.ITEM_CAST_RAIL, RailCastItemRender::getModel);
		ItemRender.register(IRItems.ITEM_TRACK_BLUEPRINT, TrackBlueprintItemModel::getModel);
		ItemRender.register(IRItems.ITEM_ROLLING_STOCK_COMPONENT, StockItemComponentModel::getModel);
		ItemRender.register(IRItems.ITEM_ROLLING_STOCK, StockItemModel::getModel, StockItemModel::getIcon);


		IEntityRender<EntityRollingStock> stockRender = ClientProxy::stockRender;
		EntityRenderer.register(LocomotiveSteam.class, stockRender);
		EntityRenderer.register(LocomotiveDiesel.class, stockRender);
		EntityRenderer.register(CarPassenger.class, stockRender);
		EntityRenderer.register(CarFreight.class, stockRender);
		EntityRenderer.register(CarTank.class, stockRender);
		EntityRenderer.register(Tender.class, stockRender);
		EntityRenderer.register(HandCar.class, stockRender);


		GlobalRender.registerRender(partialTicks -> {
			RenderOverride.renderTiles(partialTicks);
			RenderOverride.renderParticles(partialTicks);
		});
	}

	@Override
	public void init(FMLInitializationEvent event) {
		super.init(event);
		
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
		
		((SimpleReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new ClientResourceReloadListener());

		BlockRender.onPostColorSetup();
	}

	private static void stockRender(EntityRollingStock entity, float partialTicks) {
		GLBoolTracker light = new GLBoolTracker(GL11.GL_LIGHTING, true);
		GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);

		String def = entity.getDefinitionID();

		StockRenderCache.getRender(def).draw(entity, partialTicks);

		cull.restore();
		light.restore();
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
		if (!(player.getRiding() instanceof EntityRidableRollingStock)) {
			return;
		}

		EntityRidableRollingStock riding = (EntityRidableRollingStock) player.getRiding();
		
		for (KeyTypes key : keys.keySet()) {
			KeyBinding binding = keys.get(key);
			if (binding.isKeyDown()) {
				new KeyPressPacket(key, player, riding).sendToServer();
			}
		}
		
		if (player.isLeftKeyDown()) {
			new KeyPressPacket(KeyTypes.PLAYER_LEFT, player, riding).sendToServer();
		}
		if (player.isRightKeyDown()) {
			new KeyPressPacket(KeyTypes.PLAYER_RIGHT, player, riding).sendToServer();
		}
		if (player.isForwardKeyDown()) {
			new KeyPressPacket(KeyTypes.PLAYER_FORWARD, player, riding).sendToServer();
		}
		if (player.isBackKeyDown()) {
			new KeyPressPacket(KeyTypes.PLAYER_BACKWARD, player, riding).sendToServer();
		}
	}
	
	@SubscribeEvent
	public static void onClick(MouseEvent event) {
		// So it turns out that the client sends mouse click packets to the server regardless of 
		// if the entity being clicked is within the requisite distance.
		// We need to override that distance because train centers are further away
		// than 36m.
		
		int attackID = Minecraft.getMinecraft().gameSettings.keyBindAttack.getKeyCode() + 100;
		int useID = Minecraft.getMinecraft().gameSettings.keyBindUseItem.getKeyCode() + 100;
		
		if ((event.getButton() == attackID || event.getButton() == useID) && event.isButtonstate()) {
			if (Minecraft.getMinecraft().objectMouseOver == null) {
				return;
			}
			
			Hand button = attackID == event.getButton() ? Hand.SECONDARY : Hand.PRIMARY;
			
			Entity entity = MinecraftClient.getEntityMouseOver();
			if (entity instanceof EntityRidableRollingStock) {
				new MousePressPacket(button, entity).sendToServer();
				event.setCanceled(true);
				return;
			}
			Entity riding = MinecraftClient.getPlayer().getRiding();
			if (riding instanceof EntityRidableRollingStock) {
				new MousePressPacket(button, riding).sendToServer();
				event.setCanceled(true);
				return;
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
			new SteamLocomotiveOverlay().draw();
			new DieselLocomotiveOverlay().draw();
			new HandCarOverlay().draw();
		}
	}
	
	@SubscribeEvent
	public static void onRenderMouseover(DrawBlockHighlightEvent event) {
		Player player = MinecraftClient.getPlayer();
		World world = player.getWorld();
		ItemStack stack = event.getPlayer().getHeldItemMainhand();
		if (event.getTarget().getBlockPos() == null) {
			return;
		}
		Vec3i pos = new Vec3i(event.getTarget().getBlockPos());
		
		if (event.getTarget().typeOfHit == RayTraceResult.Type.BLOCK) {
			if (stack.getItem() == IRItems.ITEM_TRACK_BLUEPRINT.internal) {
				
				Vec3d vec = new Vec3d(event.getTarget().hitVec);
				Vec3d hit = vec.subtract(pos);

				pos = pos.up();

				if (BlockUtil.canBeReplaced(world, pos.down(), true)) {
					if (!BlockUtil.isIRRail(world, pos.down()) || world.getBlockEntity(pos.down(), RailBase.class).getRailHeight() < 0.5) {
						pos = pos.down();
					}
				}

		        RailInfo info = new RailInfo(world, new cam72cam.mod.item.ItemStack(stack), new PlacementInfo(new cam72cam.mod.item.ItemStack(stack), player.getRotationYawHead(), pos, hit), null);
		        String key = info.uniqueID + info.placementInfo.placementPosition;
				RailInfo cached = infoCache.get(key);
		        if (cached != null) {
					info = cached;
				} else {
		        	infoCache.put(key, info);
				}

		        GL11.glPushMatrix();
				{
					GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, true);
					
					GL11.glBlendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE);
					if (GLContext.getCapabilities().OpenGL14) {
						GL14.glBlendColor(1, 1, 1, 0.5f);
					}
					
					Vec3d cameraPos = RenderOverride.getCameraPos(event.getPartialTicks());
					Vec3d offPos = info.placementInfo.placementPosition.subtract(cameraPos);
					GL11.glTranslated(offPos.x, offPos.y, offPos.z);

	                RailRenderUtil.render(info, true);

					blend.restore();
				}
				GL11.glPopMatrix();
			}
			if (stack.getItem() == IRItems.ITEM_MANUAL.internal) {
				pos = pos.up();
				
				GL11.glPushMatrix();
				{
					GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, true);
					
					GL11.glBlendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE);
					if (GLContext.getCapabilities().OpenGL14) {
						GL14.glBlendColor(1, 1, 1, 0.3f);
					}

					Vec3d playerPos = player.getPosition();
					Vec3d lastPos = player.getLastTickPos();
					Vec3d offset = new Vec3d(pos).add(0.5, 0.5, 0.5).subtract(lastPos.add(playerPos.subtract(lastPos).scale(event.getPartialTicks())));
	                GL11.glTranslated(offset.x, offset.y, offset.z);
	                
	                GL11.glRotated(-(int)(((player.getRotationYawHead()%360+360)%360+45) / 90) * 90, 0, 1, 0);

	                MBBlueprintRender.draw(ItemMultiblockType.get(new cam72cam.mod.item.ItemStack(stack)));

					blend.restore();
				}
				GL11.glPopMatrix();
			}
		}
	}

	@SubscribeEvent
	public static void onDebugRender(RenderGameOverlayEvent.Text event) {
		if (Minecraft.getMinecraft().gameSettings.showDebugInfo && GPUInfo.hasGPUInfo()) {
			int i;
			for (i = 0; i < event.getRight().size(); i++) {
				if (event.getRight().get(i).startsWith("Display: ")) {
					i++;
					break;
				}
			}
			event.getRight().add(i, GPUInfo.debug());
		}
	}

	@SubscribeEvent
	public static void onSoundLoad(SoundLoadEvent event) {
		if (manager == null) {
			manager = new IRSoundManager(event.getManager());
		} else {
			manager.handleReload(false);
		}
	}
	
	@SubscribeEvent
	public static void onWorldLoad(Load event) {
		manager.handleReload(true);
		
		if (sndCache == null) {
			sndCache = new ArrayList<ISound>();
			for (int i = 0; i < 16; i ++) {
				sndCache.add(ImmersiveRailroading.proxy.newSound(new Identifier(ImmersiveRailroading.MODID, "sounds/default/clack.ogg"), false, 30, Gauge.from(Gauge.STANDARD)));
			}
		}
	}
	
	@SubscribeEvent
	public static void onWorldUnload(Unload event) {
		//manager.stop();
		//sndCache = null;
	}
	
	private static int sndCacheId = 0;
	private static List<ISound> sndCache;
	
	@SubscribeEvent
	public static void onEnterChunk(EnteringChunk event) {
		if (event.getEntity().getEntityWorld().isRemote) {
			// Somehow loading a chunk in the server thread can call a client event handler
			// what the fuck forge???
			return;
		}
		if (World.get(event.getEntity().getEntityWorld()) == null) {
			return;
		}
		//TODO call modded entity onEnterChunk instead
		EntityMoveableRollingStock stock = World.get(event.getEntity().getEntityWorld()).getEntity(event.getEntity().getUniqueID(), EntityMoveableRollingStock.class);

		if (stock == null || stock.getWorld().isClient) {
			return;
		}

		if (!ConfigSound.soundEnabled) {
			return;
		}
		
		if (sndCache != null) {
			
			if(event.getNewChunkX() == event.getOldChunkX() && event.getNewChunkZ() % 8 != 0) {
				return;
			}
			
			if(event.getNewChunkZ() == event.getOldChunkZ() && event.getNewChunkX() % 8 != 0) {
				return;
			}
			
			ISound snd = sndCache.get(sndCacheId);
			float adjust = (float) Math.abs(stock.getCurrentSpeed().metric()) / 300;
			if(stock.getDefinition().shouldScalePitch()) {
				snd.setPitch((float) ((adjust + 0.7)/stock.gauge.scale()));
			} else {
				snd.setPitch((float) ((adjust + 0.7)));
			}
			snd.setVolume(0.01f + adjust);
			snd.play(stock.getPosition());
	    	sndCacheId++;
	    	sndCacheId = sndCacheId % sndCache.size();
			
		}
	}
	
	@Override
	public ISound newSound(Identifier oggLocation, boolean repeats, float attenuationDistance, Gauge gauge) {
		return manager.createSound(oggLocation, repeats, attenuationDistance, gauge);
	}
	
	private static int tickCount = 0;
	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != Phase.START) {
			return;
		}

		dampenSound();
		
		if (missingResources != null) {
			Minecraft.getMinecraft().getConnection().getNetworkManager().closeChannel(PlayerMessage.direct(missingResources).internal);
			Minecraft.getMinecraft().loadWorld(null);
			Minecraft.getMinecraft().displayGuiScreen(new GuiDisconnected(new GuiMultiplayer(new GuiMainMenu()), "disconnect.lost", PlayerMessage.direct(missingResources).internal));
			missingResources = null;
		}

		Player player = MinecraftClient.getPlayer();
		World world = null;
		if (player != null) {
			world = player.getWorld();
		}
		
		
		if (world == null && manager != null && manager.hasSounds()) {
			ImmersiveRailroading.warn("Unloading IR sound system");
			manager.stop();
			sndCache = null;
		}
		
		tickCount++;
		manager.tick();
	}


	@Override
	public int getTicks() {
		return tickCount;
	}
	
	@Override
	public int getRenderDistance() {
		return Minecraft.getMinecraft().gameSettings.renderDistanceChunks;
	}

	@Override
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
