package cam72cam.immersiverailroading.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.input.Keyboard;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.CarFreight;
import cam72cam.immersiverailroading.entity.CarTank;
import cam72cam.immersiverailroading.entity.EntityRidableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.gui.FreightContainer;
import cam72cam.immersiverailroading.gui.FreightContainerGui;
import cam72cam.immersiverailroading.gui.TankContainer;
import cam72cam.immersiverailroading.gui.TankContainerGui;
import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.net.KeyPressPacket;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.render.StockEntityRender;
import cam72cam.immersiverailroading.render.StockItemModel;
import cam72cam.immersiverailroading.render.TileRailRender;
import cam72cam.immersiverailroading.tile.TileRail;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
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
import net.minecraft.world.World;
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
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int entityID, int nop1, int nop2) {
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int entityID, int nop1, int nop2) {
		switch (GuiTypes.values()[ID]) {
		case FREIGHT:
			return new FreightContainerGui((CarFreight) world.getEntityByID(entityID),
					new FreightContainer(player.inventory, (CarFreight) world.getEntityByID(entityID)));
		case TANK:
			return new TankContainerGui((CarTank) world.getEntityByID(entityID),
					new TankContainer(player.inventory, (CarTank) world.getEntityByID(entityID)));
		case TENDER:
			return null;
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
		keys.put(KeyTypes.PLAYER_FORWARD, Minecraft.getMinecraft().gameSettings.keyBindForward);
		keys.put(KeyTypes.PLAYER_BACKWARD, Minecraft.getMinecraft().gameSettings.keyBindBack);
		keys.put(KeyTypes.PLAYER_LEFT, Minecraft.getMinecraft().gameSettings.keyBindLeft);
		keys.put(KeyTypes.PLAYER_RIGHT, Minecraft.getMinecraft().gameSettings.keyBindRight);
		
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
		for (KeyTypes key : KeyTypes.values()) {
			KeyBinding binding = keys.get(key);
			if (binding.isKeyDown()) {
				EntityRidableRollingStock riding = (EntityRidableRollingStock) player.getRidingEntity();
				if (riding != null) {
					ImmersiveRailroading.net.sendToServer(new KeyPressPacket(key, riding.getEntityWorld().provider.getDimension(), player.getEntityId(), riding.getEntityId()));
				}
			}
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
}
