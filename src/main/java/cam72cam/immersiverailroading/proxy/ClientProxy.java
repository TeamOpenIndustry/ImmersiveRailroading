package cam72cam.immersiverailroading.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.entity.CarFreight;
import cam72cam.immersiverailroading.entity.FreightTank;
import cam72cam.immersiverailroading.entity.EntityRidableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.entity.Tender;
import cam72cam.immersiverailroading.gui.FreightContainer;
import cam72cam.immersiverailroading.gui.FreightContainerGui;
import cam72cam.immersiverailroading.gui.SteamLocomotiveContainer;
import cam72cam.immersiverailroading.gui.SteamLocomotiveContainerGui;
import cam72cam.immersiverailroading.gui.TankContainer;
import cam72cam.immersiverailroading.gui.TankContainerGui;
import cam72cam.immersiverailroading.gui.TenderContainer;
import cam72cam.immersiverailroading.gui.TenderContainerGui;
import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.net.KeyPressPacket;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.render.StockEntityRender;
import cam72cam.immersiverailroading.render.StockItemModel;
import cam72cam.immersiverailroading.render.rail.RailRenderUtil;
import cam72cam.immersiverailroading.render.rail.TileRailRender;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
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
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.relauncher.Side;

@EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {
	private static Map<KeyTypes, KeyBinding> keys = new HashMap<KeyTypes, KeyBinding>();

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int entityID, int nop1, int nop2) {
		System.out.println(GuiTypes.values()[ID]);
		switch (GuiTypes.values()[ID]) {
		case FREIGHT:
			return new FreightContainerGui((CarFreight) world.getEntityByID(entityID),
					new FreightContainer(player.inventory, (CarFreight) world.getEntityByID(entityID)));
		case TANK:
		case DIESEL_LOCOMOTIVE:
			return new TankContainerGui((FreightTank) world.getEntityByID(entityID),
					new TankContainer(player.inventory, (FreightTank) world.getEntityByID(entityID)));
		case TENDER:
			return new TenderContainerGui((Tender) world.getEntityByID(entityID),
					new TenderContainer(player.inventory, (Tender) world.getEntityByID(entityID)));
		case STEAM_LOCOMOTIVE:
			return new SteamLocomotiveContainerGui((LocomotiveSteam) world.getEntityByID(entityID),
					new SteamLocomotiveContainer(player.inventory, (LocomotiveSteam) world.getEntityByID(entityID)));
		default:
			return null;
		}
	}
	
	

	@Override
	public void init(FMLInitializationEvent event) {
		super.init(event);
		
		keys.put(KeyTypes.THROTTLE_UP, new KeyBinding("Increase Throttle", Keyboard.KEY_NUMPAD8, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.THROTTLE_ZERO, new KeyBinding("Zero Throttle", Keyboard.KEY_NUMPAD2, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.THROTTLE_DOWN, new KeyBinding("Decrease Throttle", Keyboard.KEY_NUMPAD5, "key.categories." + ImmersiveRailroading.MODID));
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

	@SubscribeEvent
	public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
		for (Class<? extends EntityRollingStock> type : entityClasses) {
			RenderingRegistry.registerEntityRenderingHandler(type, RENDER_INSTANCE);
		}
	}

	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event) {
		OBJLoader.INSTANCE.addDomain(ImmersiveRailroading.MODID.toLowerCase());

		ClientRegistry.bindTileEntitySpecialRenderer(TileRail.class, new TileRailRender());
		for (TrackItems item : TrackItems.values()) {
			ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(ImmersiveRailroading.BLOCK_RAIL), item.getMeta(),
					new ModelResourceLocation(ImmersiveRailroading.BLOCK_RAIL.getRegistryName(), "inventory"));
		}

		ModelLoader.setCustomMeshDefinition(ImmersiveRailroading.ITEM_ROLLING_STOCK, new ItemMeshDefinition() {
			@Override
			public ModelResourceLocation getModelLocation(ItemStack stack) {
				// TODO NBT or Damage
				return new ModelResourceLocation(ImmersiveRailroading.ITEM_ROLLING_STOCK.getRegistryName(), ItemRollingStock.defFromStack(stack));
			}
		});
	}

	@SubscribeEvent
	public static void onModelBakeEvent(ModelBakeEvent event) {
		for (String defID : DefinitionManager.getDefinitionNames()) {
			ModelResourceLocation loc = new ModelResourceLocation(ImmersiveRailroading.ITEM_ROLLING_STOCK.getRegistryName(), defID);
			IBakedModel model = new StockItemModel(DefinitionManager.getDefinition(defID));
			event.getModelRegistry().putObject(loc, model);
		}
	}

	@SubscribeEvent
	public static void onKeyInput(InputEvent.KeyInputEvent event) {
		EntityPlayerSP player = Minecraft.getMinecraft().player;
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

	public InputStream getResourceStream(ResourceLocation modelLoc) throws IOException {
		return Minecraft.getMinecraft().getResourceManager().getResource(modelLoc).getInputStream();
	}
	
	@SubscribeEvent
	public static void onOverlayEvent(RenderGameOverlayEvent.Text event) {
		if (Minecraft.getMinecraft().gameSettings.showDebugInfo) {
			Entity riding = Minecraft.getMinecraft().player.getRidingEntity();
			if (riding instanceof Locomotive) {
				event.getLeft().addAll(((Locomotive)riding).getDebugInfo());
			}
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
		        RailInfo info = new RailInfo(stack, player, pos, hitX, hitY, hitZ);
		        
		        GL11.glPushMatrix();
				{
	                double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)event.getPartialTicks();
	                double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)event.getPartialTicks();
	                double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)event.getPartialTicks();
	                GL11.glTranslated(-d0, -d1, -d2);
	                
	                GL11.glTranslated(pos.getX(), pos.getY(), pos.getZ());
	                
	                GlStateManager.disableBlend();
	                RailRenderUtil.render(info, true);
	                GlStateManager.enableBlend();
				}
				GL11.glPopMatrix();
			}
		}
	}
}
