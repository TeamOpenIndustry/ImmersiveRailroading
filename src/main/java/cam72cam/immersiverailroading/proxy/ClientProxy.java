package cam72cam.immersiverailroading.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.entity.CarFreight;
import cam72cam.immersiverailroading.entity.FreightTank;
import cam72cam.immersiverailroading.entity.EntityRidableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.EntitySmokeParticle;
import cam72cam.immersiverailroading.entity.Locomotive;
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
import cam72cam.immersiverailroading.render.StockRenderCache;
import cam72cam.immersiverailroading.render.block.RailBaseModel;
import cam72cam.immersiverailroading.render.entity.ParticleRender;
import cam72cam.immersiverailroading.render.entity.StockEntityRender;
import cam72cam.immersiverailroading.render.rail.RailRenderUtil;
import cam72cam.immersiverailroading.render.tile.TileMultiblockRender;
import cam72cam.immersiverailroading.render.tile.TileRailPreviewRender;
import cam72cam.immersiverailroading.render.tile.TileRailRender;
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
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EntitySelectors;
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
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.relauncher.Side;

@EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {
	private static Map<KeyTypes, KeyBinding> keys = new HashMap<KeyTypes, KeyBinding>();

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
	public void init(FMLInitializationEvent event) {
		super.init(event);
		
		keys.put(KeyTypes.THROTTLE_UP, new KeyBinding("Increase Throttle", Keyboard.KEY_NUMPAD8, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.THROTTLE_ZERO, new KeyBinding("Zero Throttle", Keyboard.KEY_NUMPAD5, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.THROTTLE_DOWN, new KeyBinding("Decrease Throttle", Keyboard.KEY_NUMPAD2, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.AIR_BRAKE_UP, new KeyBinding("Increase Air Brake", Keyboard.KEY_NUMPAD7, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.AIR_BRAKE_ZERO, new KeyBinding("Zero Air Brake", Keyboard.KEY_NUMPAD4, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.AIR_BRAKE_DOWN, new KeyBinding("Decrease Air Brake", Keyboard.KEY_NUMPAD1, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.HORN, new KeyBinding("Sound Horn", Keyboard.KEY_NUMPADENTER, "key.categories." + ImmersiveRailroading.MODID));
		//keys.put(KeyTypes.PLAYER_FORWARD, Minecraft.getMinecraft().gameSettings.keyBindForward);
		//keys.put(KeyTypes.PLAYER_BACKWARD, Minecraft.getMinecraft().gameSettings.keyBindBack);
		//keys.put(KeyTypes.PLAYER_LEFT, Minecraft.getMinecraft().gameSettings.keyBindLeft);
		//keys.put(KeyTypes.PLAYER_RIGHT, Minecraft.getMinecraft().gameSettings.keyBindRight);
		
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.THROTTLE_UP));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.THROTTLE_DOWN));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.THROTTLE_ZERO));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.AIR_BRAKE_UP));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.AIR_BRAKE_DOWN));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.AIR_BRAKE_ZERO));
		ClientRegistry.registerKeyBinding(keys.get(KeyTypes.HORN));
		
		((SimpleReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new ClientResourceReloadListener());
	}
	
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

	@SubscribeEvent
	public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
		for (Class<? extends EntityRollingStock> type : entityClasses) {
			RenderingRegistry.registerEntityRenderingHandler(type, RENDER_INSTANCE);
		}

		RenderingRegistry.registerEntityRenderingHandler(EntitySmokeParticle.class, PARTICLE_RENDER);
	}

	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event) {
		OBJLoader.INSTANCE.addDomain(ImmersiveRailroading.MODID.toLowerCase());

		ClientRegistry.bindTileEntitySpecialRenderer(TileRail.class, new TileRailRender());
		ClientRegistry.bindTileEntitySpecialRenderer(TileRailPreview.class, new TileRailPreviewRender());
		ClientRegistry.bindTileEntitySpecialRenderer(TileMultiblock.class, new TileMultiblockRender());
		
		ModelLoader.setCustomModelResourceLocation(ImmersiveRailroading.ITEM_LARGE_WRENCH, 0,
				new ModelResourceLocation(ImmersiveRailroading.ITEM_LARGE_WRENCH.getRegistryName(), ""));

		ModelLoader.setCustomModelResourceLocation(ImmersiveRailroading.ITEM_HOOK, 0,
				new ModelResourceLocation(ImmersiveRailroading.ITEM_HOOK.getRegistryName(), ""));
		
		ModelLoader.setCustomModelResourceLocation(ImmersiveRailroading.ITEM_RAIL_BLOCK, 0,
				new ModelResourceLocation(ImmersiveRailroading.ITEM_RAIL_BLOCK.getRegistryName(), ""));

		ModelLoader.setCustomModelResourceLocation(ImmersiveRailroading.ITEM_ROLLING_STOCK_COMPONENT, 0,
				new ModelResourceLocation(ImmersiveRailroading.ITEM_ROLLING_STOCK_COMPONENT.getRegistryName(), ""));

		ModelLoader.setCustomModelResourceLocation(ImmersiveRailroading.ITEM_ROLLING_STOCK, 0,
				new ModelResourceLocation(ImmersiveRailroading.ITEM_ROLLING_STOCK.getRegistryName(), ""));
		
		ModelLoader.setCustomModelResourceLocation(ImmersiveRailroading.ITEM_AUGMENT, 0,
				new ModelResourceLocation(ImmersiveRailroading.ITEM_AUGMENT.getRegistryName(), ""));
		
		ModelLoader.setCustomModelResourceLocation(ImmersiveRailroading.ITEM_RAIL, 0,
				new ModelResourceLocation(ImmersiveRailroading.ITEM_RAIL.getRegistryName(), ""));
		
		ModelLoader.setCustomModelResourceLocation(ImmersiveRailroading.ITEM_CAST_RAIL, 0,
				new ModelResourceLocation(ImmersiveRailroading.ITEM_CAST_RAIL.getRegistryName(), ""));
		
		ModelLoader.setCustomModelResourceLocation(ImmersiveRailroading.ITEM_PLATE, 0,
				new ModelResourceLocation(ImmersiveRailroading.ITEM_PLATE.getRegistryName(), ""));
		
		ModelLoader.setCustomModelResourceLocation(ImmersiveRailroading.ITEM_MANUAL, 0,
				new ModelResourceLocation("minecraft:written_book", ""));
	}

	@SubscribeEvent
	public static void onModelBakeEvent(ModelBakeEvent event) {
		event.getModelRegistry().putObject(new ModelResourceLocation(ImmersiveRailroading.ITEM_ROLLING_STOCK.getRegistryName(), ""), new StockItemModel());
		event.getModelRegistry().putObject(new ModelResourceLocation(ImmersiveRailroading.ITEM_RAIL_BLOCK.getRegistryName(), ""), new TrackBlueprintItemModel());
		event.getModelRegistry().putObject(new ModelResourceLocation(ImmersiveRailroading.ITEM_ROLLING_STOCK_COMPONENT.getRegistryName(), ""), new StockItemComponentModel());
		event.getModelRegistry().putObject(new ModelResourceLocation(ImmersiveRailroading.ITEM_AUGMENT.getRegistryName(), ""), new RailAugmentItemModel());
		event.getModelRegistry().putObject(new ModelResourceLocation(ImmersiveRailroading.ITEM_RAIL.getRegistryName(), ""), new RailItemRender());
		event.getModelRegistry().putObject(new ModelResourceLocation(ImmersiveRailroading.ITEM_CAST_RAIL.getRegistryName(), ""), new RailCastItemRender());
		event.getModelRegistry().putObject(new ModelResourceLocation(ImmersiveRailroading.ITEM_PLATE.getRegistryName(), ""), new PlateItemModel());
		event.getModelRegistry().putObject(new ModelResourceLocation(ImmersiveRailroading.BLOCK_RAIL.getRegistryName(), ""), new RailBaseModel());
		event.getModelRegistry().putObject(new ModelResourceLocation(ImmersiveRailroading.BLOCK_RAIL_GAG.getRegistryName(), ""), new RailBaseModel());
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
		if ((event.getButton() == 0 || event.getButton() == 1) && event.isButtonstate()) {
			if (Minecraft.getMinecraft().objectMouseOver == null) {
				return;
			}
			Entity entity = Minecraft.getMinecraft().objectMouseOver.entityHit;
			if (entity != null && entity instanceof EntityRidableRollingStock) {
				ImmersiveRailroading.net.sendToServer(new MousePressPacket(event.getButton(), entity.world.provider.getDimension(), entity.getEntityId()));
				event.setCanceled(true);
				return;
			}
			Entity riding = Minecraft.getMinecraft().player.getRidingEntity();
			if (riding != null && riding instanceof EntityRidableRollingStock) {
				ImmersiveRailroading.net.sendToServer(new MousePressPacket(event.getButton(), riding.world.provider.getDimension(), riding.getEntityId()));
				event.setCanceled(true);
				return;
			}
		}
	}

	public InputStream getResourceStream(ResourceLocation modelLoc) throws IOException {
		return Minecraft.getMinecraft().getResourceManager().getResource(modelLoc).getInputStream();
	}
	
	public List<InputStream> getResourceStreamAll(ResourceLocation modelLoc) throws IOException {
		List<InputStream> res = new ArrayList<InputStream>();
		for (IResource resource : Minecraft.getMinecraft().getResourceManager().getAllResources(modelLoc)) {
			res.add(resource.getInputStream());
		}
		return res;
	}
	
	@SubscribeEvent
	public static void onOverlayEvent(RenderGameOverlayEvent.Text event) {
		if (Minecraft.getMinecraft().gameSettings.showDebugInfo) {
			Entity riding = Minecraft.getMinecraft().player.getRidingEntity();
			if (riding instanceof Locomotive) {
				//event.getLeft().addAll(((Locomotive)riding).getDebugInfo());
			}
		}
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
		
		Minecraft.getMinecraft().mcProfiler.startSection("ir_entity");

        GLBoolTracker color = new GLBoolTracker(GL11.GL_COLOR_MATERIAL, true);
        RenderHelper.enableStandardItemLighting();
        Minecraft.getMinecraft().entityRenderer.enableLightmap();

		GlStateManager.enableAlpha();
        
        float partialTicks = event.getPartialTicks();
        ICamera camera = new Frustum();
        Entity playerrRender = Minecraft.getMinecraft().getRenderViewEntity();
        double d0 = playerrRender.lastTickPosX + (playerrRender.posX - playerrRender.lastTickPosX) * (double)partialTicks;
        double d1 = playerrRender.lastTickPosY + (playerrRender.posY - playerrRender.lastTickPosY) * (double)partialTicks;
        double d2 = playerrRender.lastTickPosZ + (playerrRender.posZ - playerrRender.lastTickPosZ) * (double)partialTicks;
        camera.setPosition(d0, d1, d2);
        
        List<EntityRollingStock> entities = Minecraft.getMinecraft().player.getEntityWorld().getEntities(EntityRollingStock.class, EntitySelectors.IS_ALIVE);
        for (EntityRollingStock entity : entities) {
        	if (camera.isBoundingBoxInFrustum(entity.getRenderBoundingBox()) ) {
        		Minecraft.getMinecraft().mcProfiler.startSection("render_stock");
        		Minecraft.getMinecraft().getRenderManager().renderEntityStatic(entity, partialTicks, true);
        		Minecraft.getMinecraft().mcProfiler.endSection();;
        	}
        }
        
        GlStateManager.depthMask(false);
        
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        Vec3d ep = player.getPositionEyes(partialTicks);
        
        List<EntitySmokeParticle> smokeEnts = player.getEntityWorld().getEntities(EntitySmokeParticle.class, EntitySelectors.IS_ALIVE);
        Comparator<EntitySmokeParticle> compare = (EntitySmokeParticle e1, EntitySmokeParticle e2) -> {
        	Double p1 = e1.getPositionVector().distanceTo(ep);
        	Double p2 = e1.getPositionVector().distanceTo(ep);
        	return p1.compareTo(p2);
        };
        Collections.sort(smokeEnts,  compare);
        

		Minecraft.getMinecraft().mcProfiler.startSection("ir_particles");
		
        ParticleRender.shader.bind();
		GLBoolTracker light = new GLBoolTracker(GL11.GL_LIGHTING, false);
		GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);
		GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, false);
		GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, true);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        for (EntitySmokeParticle entity : smokeEnts) {
        	if (camera.isBoundingBoxInFrustum(entity.getRenderBoundingBox()) ) {
        		Minecraft.getMinecraft().mcProfiler.startSection("render_particle");
        		Minecraft.getMinecraft().getRenderManager().renderEntityStatic(entity, partialTicks, true);
        		Minecraft.getMinecraft().mcProfiler.endSection();;
        	}
        }

		blend.restore();
		tex.restore();
		cull.restore();
		light.restore();
		
		ParticleRender.shader.unbind();
		
		Minecraft.getMinecraft().mcProfiler.endSection();
        
        Minecraft.getMinecraft().entityRenderer.disableLightmap();;
        RenderHelper.disableStandardItemLighting();
        color.restore();
        
        GlStateManager.depthMask(true);
        
        Minecraft.getMinecraft().mcProfiler.endSection();;
	}
	
	@SubscribeEvent
	public static void onOverlayEvent(RenderGameOverlayEvent.Pre event) {
		if (event.getType() == ElementType.CHAT) {
			new SteamLocomotiveOverlay().draw();
			new DieselLocomotiveOverlay().draw();
			new HandCarOverlay().draw();
		}
	}
	
	@SubscribeEvent
	public static void onRenderMouseover(DrawBlockHighlightEvent event) {
		if (event.getTarget().typeOfHit == RayTraceResult.Type.BLOCK) {
			if (event.getPlayer().getHeldItemMainhand().getItem() == ImmersiveRailroading.ITEM_RAIL_BLOCK) {
				EntityPlayer player = event.getPlayer();
				ItemStack stack = event.getPlayer().getHeldItemMainhand();
				BlockPos pos = event.getTarget().getBlockPos();
				
				Vec3d vec = event.getTarget().hitVec;
		        float hitX = (float)(vec.x - (double)pos.getX());
		        float hitY = (float)(vec.y - (double)pos.getY());
		        float hitZ = (float)(vec.z - (double)pos.getZ());
		        
		        if (player.getEntityWorld().getBlockState(pos).getBlock() instanceof BlockRailBase) {
		        	pos = pos.down();
		        }
		        
		        pos = pos.up();
		        RailInfo info = new RailInfo(stack, player.world, player.rotationYaw, pos, hitX, hitY, hitZ);
		        
		        GL11.glPushMatrix();
				{
					GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, true);
					
					GL11.glBlendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE);
					if (GLContext.getCapabilities().OpenGL14) {
						GL14.glBlendColor(1, 1, 1, 0.5f);
					}
					
	                double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)event.getPartialTicks();
	                double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)event.getPartialTicks();
	                double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)event.getPartialTicks();
	                GL11.glTranslated(-d0, -d1, -d2);
	                
	                GL11.glTranslated(pos.getX(), pos.getY(), pos.getZ());
	                
	                RailRenderUtil.render(info, true);

					blend.restore();
				}
				GL11.glPopMatrix();
			}
		}
	}
	
	private static int tickCount = 0;
	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (tickCount % 40 == 39 ) {
			StockRenderCache.doImageCache();
		}
		tickCount++;
	}
}
