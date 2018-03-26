package cam72cam.immersiverailroading.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.entity.CarFreight;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.FreightTank;
import cam72cam.immersiverailroading.entity.EntityRidableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.EntitySmokeParticle;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.entity.Tender;
import cam72cam.immersiverailroading.gui.CastingGUI;
import cam72cam.immersiverailroading.gui.FreightContainer;
import cam72cam.immersiverailroading.gui.FreightContainerGui;
import cam72cam.immersiverailroading.gui.PlateRollerGUI;
import cam72cam.immersiverailroading.gui.SteamHammerContainer;
import cam72cam.immersiverailroading.gui.SteamHammerContainerGui;
import cam72cam.immersiverailroading.gui.SteamLocomotiveContainer;
import cam72cam.immersiverailroading.gui.SteamLocomotiveContainerGui;
import cam72cam.immersiverailroading.gui.TankContainer;
import cam72cam.immersiverailroading.gui.TankContainerGui;
import cam72cam.immersiverailroading.gui.TenderContainer;
import cam72cam.immersiverailroading.gui.TenderContainerGui;
import cam72cam.immersiverailroading.gui.TrackGui;
import cam72cam.immersiverailroading.gui.overlay.DieselLocomotiveOverlay;
import cam72cam.immersiverailroading.gui.overlay.HandCarOverlay;
import cam72cam.immersiverailroading.gui.overlay.SteamLocomotiveOverlay;
import cam72cam.immersiverailroading.items.nbt.ItemMultiblockType;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.net.KeyPressPacket;
import cam72cam.immersiverailroading.net.MousePressPacket;
import cam72cam.immersiverailroading.render.item.PlateItemModel;
import cam72cam.immersiverailroading.render.item.RailAugmentItemModel;
import cam72cam.immersiverailroading.render.item.RailCastItemRender;
import cam72cam.immersiverailroading.render.item.RailItemRender;
import cam72cam.immersiverailroading.render.item.StockItemComponentModel;
import cam72cam.immersiverailroading.render.item.StockItemModel;
import cam72cam.immersiverailroading.render.item.TrackBlueprintItemModel;
import cam72cam.immersiverailroading.render.multiblock.MBBlueprintRender;
import cam72cam.immersiverailroading.render.RenderCacheTimeLimiter;
import cam72cam.immersiverailroading.render.StockRenderCache;
import cam72cam.immersiverailroading.render.block.RailBaseModel;
import cam72cam.immersiverailroading.render.entity.MagicEntityRender;
import cam72cam.immersiverailroading.render.entity.MagicEntity;
import cam72cam.immersiverailroading.render.entity.ParticleRender;
import cam72cam.immersiverailroading.render.entity.RenderOverride;
import cam72cam.immersiverailroading.render.entity.StockEntityRender;
import cam72cam.immersiverailroading.render.rail.RailRenderUtil;
import cam72cam.immersiverailroading.render.tile.TileMultiblockRender;
import cam72cam.immersiverailroading.render.tile.TileRailPreviewRender;
import cam72cam.immersiverailroading.render.tile.TileRailRender;
import cam72cam.immersiverailroading.sound.IRSoundManager;
import cam72cam.immersiverailroading.sound.ISound;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityEvent.EnteringChunk;
import net.minecraftforge.event.world.WorldEvent.Load;
import net.minecraftforge.event.world.WorldEvent.Unload;
import net.minecraftforge.fml.client.FMLClientHandler;
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
import paulscode.sound.SoundSystemConfig;

@EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {
	private static Map<KeyTypes, KeyBinding> keys = new HashMap<KeyTypes, KeyBinding>();

	private static IRSoundManager manager;

	private static MagicEntity magical;
	public static RenderCacheTimeLimiter renderCacheLimiter = new RenderCacheTimeLimiter();

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int entityIDorPosX, int posY, int posZ) {
		System.out.println(GuiTypes.values()[ID]);
		TileMultiblock te;
		switch (GuiTypes.values()[ID]) {
		case FREIGHT:
			return new FreightContainerGui((CarFreight) world.getEntityByID(entityIDorPosX),
					new FreightContainer(player.inventory, (CarFreight) world.getEntityByID(entityIDorPosX)));
		case TANK:
		case DIESEL_LOCOMOTIVE:
			return new TankContainerGui((FreightTank) world.getEntityByID(entityIDorPosX),
					new TankContainer(player.inventory, (FreightTank) world.getEntityByID(entityIDorPosX)));
		case TENDER:
			return new TenderContainerGui((Tender) world.getEntityByID(entityIDorPosX),
					new TenderContainer(player.inventory, (Tender) world.getEntityByID(entityIDorPosX)));
		case STEAM_LOCOMOTIVE:
			return new SteamLocomotiveContainerGui((LocomotiveSteam) world.getEntityByID(entityIDorPosX),
					new SteamLocomotiveContainer(player.inventory, (LocomotiveSteam) world.getEntityByID(entityIDorPosX)));
		case RAIL:
			return new TrackGui();
		case RAIL_PREVIEW:
			return new TrackGui(world, entityIDorPosX, posY, posZ);
		case STEAM_HAMMER:
			te = TileMultiblock.get(world, new BlockPos(entityIDorPosX, posY, posZ));
			if (te == null || !te.isLoaded()) {
				return null;
			}
			return new SteamHammerContainerGui(new SteamHammerContainer(player.inventory, te));
		case PLATE_ROLLER:
			te = TileMultiblock.get(world, new BlockPos(entityIDorPosX, posY, posZ));
			if (te == null || !te.isLoaded()) {
				return null;
			}
			return new PlateRollerGUI(te);
		case CASTING:
			te = TileMultiblock.get(world, new BlockPos(entityIDorPosX, posY, posZ));
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
			SoundSystemConfig.setNumberNormalChannels(2000);
		}
		
		if (Loader.isModLoaded("igwmod")) {
			FMLInterModComms.sendMessage("igwmod", "cam72cam.immersiverailroading.thirdparty.IGWMod", "init");
		}
	}

	@Override
	public void init(FMLInitializationEvent event) {
		super.init(event);
		
		keys.put(KeyTypes.THROTTLE_UP, new KeyBinding("immersiverailroading:keys.increase_throttle", Keyboard.KEY_NUMPAD8, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.THROTTLE_ZERO, new KeyBinding("immersiverailroading:keys.zero_throttle", Keyboard.KEY_NUMPAD5, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.THROTTLE_DOWN, new KeyBinding("immersiverailroading:keys.decrease_throttle", Keyboard.KEY_NUMPAD2, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.AIR_BRAKE_UP, new KeyBinding("immersiverailroading:keys.increase_brake", Keyboard.KEY_NUMPAD7, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.AIR_BRAKE_ZERO, new KeyBinding("immersiverailroading:keys.zero_brake", Keyboard.KEY_NUMPAD4, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.AIR_BRAKE_DOWN, new KeyBinding("immersiverailroading:keys.decrease_brake", Keyboard.KEY_NUMPAD1, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.HORN, new KeyBinding("immersiverailroading:keys.horn", Keyboard.KEY_NUMPADENTER, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.DEAD_MANS_SWITCH, new KeyBinding("immersiverailroading:keys.dead_mans_switch", Keyboard.KEY_NUMPADEQUALS, "key.categories." + ImmersiveRailroading.MODID));
		
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.THROTTLE_UP));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.THROTTLE_DOWN));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.THROTTLE_ZERO));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.AIR_BRAKE_UP));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.AIR_BRAKE_DOWN));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.AIR_BRAKE_ZERO));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.HORN));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.DEAD_MANS_SWITCH));
		
		((SimpleReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new ClientResourceReloadListener());
	}
	
	@Override
	public World getWorld(int dimension)  {
		return FMLClientHandler.instance().getWorldClient();
	}

	public static final IRenderFactory<EntityRollingStock> RENDER_INSTANCE = new IRenderFactory<EntityRollingStock>() {
		@Override
		public Render<? super EntityRollingStock> createRenderFor(RenderManager manager) {
			return new StockEntityRender(manager);
		}
	};
	
	public static final IRenderFactory<EntitySmokeParticle> PARTICLE_RENDER = new IRenderFactory<EntitySmokeParticle>() {
		@Override
		public Render<? super EntitySmokeParticle> createRenderFor(RenderManager manager) {
			return new ParticleRender(manager);
		}
	};
	
	public static final IRenderFactory<MagicEntity> MAGIC_RENDER = new IRenderFactory<MagicEntity>() {
		@Override
		public Render<? super MagicEntity> createRenderFor(RenderManager manager) {
			return new MagicEntityRender(manager);
		}
	};

	@SubscribeEvent
	public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
		for (Class<? extends EntityRollingStock> type : entityClasses) {
			RenderingRegistry.registerEntityRenderingHandler(type, RENDER_INSTANCE);
		}

		RenderingRegistry.registerEntityRenderingHandler(EntitySmokeParticle.class, PARTICLE_RENDER);
		RenderingRegistry.registerEntityRenderingHandler(MagicEntity.class, MAGIC_RENDER);
	}

	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event) {
		OBJLoader.INSTANCE.addDomain(ImmersiveRailroading.MODID.toLowerCase());

		ClientRegistry.bindTileEntitySpecialRenderer(TileRail.class, new TileRailRender());
		ClientRegistry.bindTileEntitySpecialRenderer(TileRailPreview.class, new TileRailPreviewRender());
		ClientRegistry.bindTileEntitySpecialRenderer(TileMultiblock.class, new TileMultiblockRender());
		
		ModelLoader.setCustomModelResourceLocation(IRItems.ITEM_LARGE_WRENCH, 0,
				new ModelResourceLocation(IRItems.ITEM_LARGE_WRENCH.getRegistryName(), ""));

		ModelLoader.setCustomModelResourceLocation(IRItems.ITEM_HOOK, 0,
				new ModelResourceLocation(IRItems.ITEM_HOOK.getRegistryName(), ""));
		
		ModelLoader.setCustomModelResourceLocation(IRItems.ITEM_CONDUCTOR_WHISTLE, 0,
				new ModelResourceLocation(IRItems.ITEM_CONDUCTOR_WHISTLE.getRegistryName(), ""));
		
		ModelLoader.setCustomModelResourceLocation(IRItems.ITEM_TRACK_BLUEPRINT, 0,
				new ModelResourceLocation(IRItems.ITEM_TRACK_BLUEPRINT.getRegistryName(), ""));

		ModelLoader.setCustomModelResourceLocation(IRItems.ITEM_ROLLING_STOCK_COMPONENT, 0,
				new ModelResourceLocation(IRItems.ITEM_ROLLING_STOCK_COMPONENT.getRegistryName(), ""));

		ModelLoader.setCustomModelResourceLocation(IRItems.ITEM_ROLLING_STOCK, 0,
				new ModelResourceLocation(IRItems.ITEM_ROLLING_STOCK.getRegistryName(), ""));
		
		ModelLoader.setCustomModelResourceLocation(IRItems.ITEM_AUGMENT, 0,
				new ModelResourceLocation(IRItems.ITEM_AUGMENT.getRegistryName(), ""));
		
		ModelLoader.setCustomModelResourceLocation(IRItems.ITEM_RAIL, 0,
				new ModelResourceLocation(IRItems.ITEM_RAIL.getRegistryName(), ""));
		
		ModelLoader.setCustomModelResourceLocation(IRItems.ITEM_CAST_RAIL, 0,
				new ModelResourceLocation(IRItems.ITEM_CAST_RAIL.getRegistryName(), ""));
		
		ModelLoader.setCustomModelResourceLocation(IRItems.ITEM_PLATE, 0,
				new ModelResourceLocation(IRItems.ITEM_PLATE.getRegistryName(), ""));
		
		ModelLoader.setCustomModelResourceLocation(IRItems.ITEM_MANUAL, 0,
				new ModelResourceLocation("minecraft:written_book", ""));
	}

	@SubscribeEvent
	public static void onModelBakeEvent(ModelBakeEvent event) {
		event.getModelRegistry().putObject(new ModelResourceLocation(IRItems.ITEM_ROLLING_STOCK.getRegistryName(), ""), new StockItemModel());
		event.getModelRegistry().putObject(new ModelResourceLocation(IRItems.ITEM_TRACK_BLUEPRINT.getRegistryName(), ""), new TrackBlueprintItemModel());
		event.getModelRegistry().putObject(new ModelResourceLocation(IRItems.ITEM_ROLLING_STOCK_COMPONENT.getRegistryName(), ""), new StockItemComponentModel());
		event.getModelRegistry().putObject(new ModelResourceLocation(IRItems.ITEM_AUGMENT.getRegistryName(), ""), new RailAugmentItemModel());
		event.getModelRegistry().putObject(new ModelResourceLocation(IRItems.ITEM_RAIL.getRegistryName(), ""), new RailItemRender());
		event.getModelRegistry().putObject(new ModelResourceLocation(IRItems.ITEM_CAST_RAIL.getRegistryName(), ""), new RailCastItemRender());
		event.getModelRegistry().putObject(new ModelResourceLocation(IRItems.ITEM_PLATE.getRegistryName(), ""), new PlateItemModel());
		event.getModelRegistry().putObject(new ModelResourceLocation(IRBlocks.BLOCK_RAIL.getRegistryName(), ""), new RailBaseModel());
		event.getModelRegistry().putObject(new ModelResourceLocation(IRBlocks.BLOCK_RAIL_GAG.getRegistryName(), ""), new RailBaseModel());
	}

	@SubscribeEvent
	public static void onKeyInput(ClientTickEvent event) {
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		if (player == null) {
			return;
		}
		if (!player.isRiding()) {
			return;
		}
		
		if (!(player.getRidingEntity() instanceof EntityRidableRollingStock)) {
			return;
		}

		EntityRidableRollingStock riding = (EntityRidableRollingStock) player.getRidingEntity();
		
		for (KeyTypes key : keys.keySet()) {
			KeyBinding binding = keys.get(key);
			if (binding.isKeyDown()) {
				ImmersiveRailroading.net.sendToServer(new KeyPressPacket(key, riding.getEntityWorld().provider.getDimension(), player.getEntityId(), riding.getEntityId()));
			}
		}
		
		if (player.movementInput.leftKeyDown) {
			ImmersiveRailroading.net.sendToServer(new KeyPressPacket(KeyTypes.PLAYER_LEFT, riding.getEntityWorld().provider.getDimension(), player.getEntityId(), riding.getEntityId()));
		}
		if (player.movementInput.rightKeyDown) {
			ImmersiveRailroading.net.sendToServer(new KeyPressPacket(KeyTypes.PLAYER_RIGHT, riding.getEntityWorld().provider.getDimension(), player.getEntityId(), riding.getEntityId()));
		}
		if (player.movementInput.forwardKeyDown) {
			ImmersiveRailroading.net.sendToServer(new KeyPressPacket(KeyTypes.PLAYER_FORWARD, riding.getEntityWorld().provider.getDimension(), player.getEntityId(), riding.getEntityId()));
		}
		if (player.movementInput.backKeyDown) {
			ImmersiveRailroading.net.sendToServer(new KeyPressPacket(KeyTypes.PLAYER_BACKWARD, riding.getEntityWorld().provider.getDimension(), player.getEntityId(), riding.getEntityId()));
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
			
			int button = attackID == event.getButton() ? 0 : 1;
			
			Entity entity = Minecraft.getMinecraft().objectMouseOver.entityHit;
			if (entity != null && entity instanceof EntityRidableRollingStock) {
				ImmersiveRailroading.net.sendToServer(new MousePressPacket(button, entity.world.provider.getDimension(), entity.getEntityId()));
				event.setCanceled(true);
				return;
			}
			Entity riding = Minecraft.getMinecraft().player.getRidingEntity();
			if (riding != null && riding instanceof EntityRidableRollingStock) {
				ImmersiveRailroading.net.sendToServer(new MousePressPacket(button, riding.world.provider.getDimension(), riding.getEntityId()));
				event.setCanceled(true);
				return;
			}
		}
	}

	@Override
	public InputStream getResourceStream(ResourceLocation modelLoc) throws IOException {
		return Minecraft.getMinecraft().getResourceManager().getResource(modelLoc).getInputStream();
	}
	
	@Override
	public List<InputStream> getResourceStreamAll(ResourceLocation modelLoc) throws IOException {
		List<InputStream> res = new ArrayList<InputStream>();
		for (IResource resource : Minecraft.getMinecraft().getResourceManager().getAllResources(modelLoc)) {
			res.add(resource.getInputStream());
		}
		return res;
	}
	
	@SubscribeEvent
	public static void onHackRenderEvent(RenderWorldLastEvent event) {
		/*
		 * Minecraft does NOT support rendering entities which overlap with the field of view but don't exist in it
		 * 
		 * For large entities this breaks in awesome ways, like walking past the center of a rail car
		 * 
		 * To fix this we render the entity the player is riding by hand at the end of the render loop
		 * This is a bad hack but it works
		 * 
		 */

		if (!ConfigGraphics.useShaderFriendlyRender) {
			float partialTicks = event.getPartialTicks();

			GLBoolTracker color = new GLBoolTracker(GL11.GL_COLOR_MATERIAL, true);
			RenderHelper.enableStandardItemLighting();
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
			GlStateManager.enableAlpha();
			RenderOverride.renderTiles(partialTicks);
			RenderOverride.renderStock(partialTicks);
			RenderOverride.renderParticles(partialTicks);

			GlStateManager.disableAlpha();
			Minecraft.getMinecraft().entityRenderer.disableLightmap();
			RenderHelper.disableStandardItemLighting();
			color.restore();
		}
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
		EntityPlayer player = event.getPlayer();
		ItemStack stack = event.getPlayer().getHeldItemMainhand();
		BlockPos pos = event.getTarget().getBlockPos();
		
		if (event.getTarget().typeOfHit == RayTraceResult.Type.BLOCK) {
			if (stack.getItem() == IRItems.ITEM_TRACK_BLUEPRINT) {
				
				Vec3d vec = event.getTarget().hitVec;
		        float hitX = (float)(vec.x - pos.getX());
		        float hitY = (float)(vec.y - pos.getY());
		        float hitZ = (float)(vec.z - pos.getZ());
		        
		        if (player.getEntityWorld().getBlockState(pos).getBlock() instanceof BlockRailBase) {
		        	pos = pos.down();
		        }
		        
		        pos = pos.up();
		        RailInfo info = new RailInfo(stack, player.world, player.getRotationYawHead(), pos, hitX, hitY, hitZ);
		        
		        GL11.glPushMatrix();
				{
					GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, true);
					
					GL11.glBlendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE);
					if (GLContext.getCapabilities().OpenGL14) {
						GL14.glBlendColor(1, 1, 1, 0.5f);
					}
					
	                double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks();
	                double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.getPartialTicks();
	                double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks();
	                GL11.glTranslated(-d0, -d1, -d2);
	                
	                GL11.glTranslated(pos.getX(), pos.getY(), pos.getZ());
	                
	                RailRenderUtil.render(info, true);

					blend.restore();
				}
				GL11.glPopMatrix();
			}
			if (stack.getItem() == IRItems.ITEM_MANUAL) {
				pos = pos.up();
				
				GL11.glPushMatrix();
				{
					GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, true);
					
					GL11.glBlendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE);
					if (GLContext.getCapabilities().OpenGL14) {
						GL14.glBlendColor(1, 1, 1, 0.3f);
					}
					
	                double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks();
	                double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.getPartialTicks();
	                double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks();
	                GL11.glTranslated(-d0, -d1, -d2);
	                
	                GL11.glTranslated(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5);
	                
	                if (Math.random() < 0.1) {
	                }
	                GL11.glRotated(-(int)(((player.getRotationYawHead()%360+360)%360+45) / 90) * 90, 0, 1, 0);
	                
	                GL11.glTranslated(-0.5, -0.5, -0.5);
	                
	                MBBlueprintRender.draw(player.getEntityWorld(), ItemMultiblockType.get(stack));

					blend.restore();
				}
				GL11.glPopMatrix();
			}
		}
	}
	
	@SubscribeEvent
	public static void onSoundLoad(SoundLoadEvent event) {
		if (manager == null) {
			manager = new IRSoundManager(event.getManager());
		} else {
			manager.handleReload();
		}
	}
	
	@SubscribeEvent
	public static void onWorldLoad(Load event) {		
		// This is super fragile
		sndCache = new ArrayList<ISound>();
		for (int i = 0; i < 16; i ++) {
			sndCache.add(ImmersiveRailroading.proxy.newSound(new ResourceLocation(ImmersiveRailroading.MODID, "sounds/default/clack.ogg"), false, 40, Gauge.STANDARD));
		}
		magical = new MagicEntity(event.getWorld());
		event.getWorld().loadedEntityList.add(magical);
	}
	
	@SubscribeEvent
	public static void onWorldUnload(Unload event) {
		manager.stop();
		magical = null;
	}
	
	private static int sndCacheId = 0;
	private static List<ISound> sndCache;
	
	@SubscribeEvent
	public static void onEnterChunk(EnteringChunk event) {
		if (!event.getEntity().getEntityWorld().isRemote) {
			// Somehow loading a chunk in the server thread can call a client event handler
			// what the fuck forge???
			return;
		}
		
		if (!ConfigSound.soundEnabled) {
			return;
		}
		
		if (event.getEntity() instanceof EntityMoveableRollingStock) {
			
			if(event.getNewChunkX() == event.getOldChunkX() && event.getNewChunkZ() % 4 != 0) {
				return;
			}
			
			if(event.getNewChunkZ() == event.getOldChunkZ() && event.getNewChunkX() % 4 != 0) {
				return;
			}
			
			ISound snd = sndCache.get(sndCacheId);
			// TODO Doppler update
			snd.setPitch((float) (1/((EntityMoveableRollingStock)event.getEntity()).gauge.scale()));
			//0.5f + (float) Math.abs(((EntityMoveableRollingStock)event.getEntity()).getCurrentSpeed().metric() / 300f)
			snd.setVolume(0.3f);
			snd.play(event.getEntity().getPositionVector());
	    	sndCacheId++;
	    	sndCacheId = sndCacheId % sndCache.size();
			
		}
	}
	
	@Override
	public ISound newSound(ResourceLocation oggLocation, boolean repeats, float attenuationDistance, Gauge gauge) {
		return manager.createSound(oggLocation, repeats, attenuationDistance, gauge);
	}
	
	private static int tickCount = 0;
	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != Phase.START) {
			return;
		}
		
		if (magical != null) {
			magical.onUpdate();
			
			if (magical.isDead) {
				magical.isDead = false;
				ImmersiveRailroading.warn("Reanimating magic entity");
				magical.world.spawnEntity(magical);
			}
			if (tickCount % 20 == 0) {
				if (!magical.world.loadedEntityList.contains(magical)) {
					ImmersiveRailroading.warn("Respawning magic entity");
					magical.isDead = false;
					magical.world.spawnEntity(magical);
				}
			}
		}
		
		if (tickCount % 40 == 39 ) {
			StockRenderCache.doImageCache();
		}
		tickCount++;
		manager.tick();
		
		StockRenderCache.tryCache();
	}


	@Override
	public int getTicks() {
		return tickCount;
	}
	

	
	@SubscribeEvent
	public static void configChanged(OnConfigChangedEvent event) {
		if (event.getModID().equals(ImmersiveRailroading.MODID)) {
			ConfigManager.sync(ImmersiveRailroading.MODID, net.minecraftforge.common.config.Config.Type.INSTANCE);
		}
	}
}
