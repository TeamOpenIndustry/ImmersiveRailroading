package cam72cam.immersiverailroading;

import org.apache.logging.log4j.Logger;

import cam72cam.immersiverailroading.blocks.BlockRail;
import cam72cam.immersiverailroading.blocks.BlockRailGag;
import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.entity.LocomotiveElectric;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.entity.registry.DefinitionManager;
import cam72cam.immersiverailroading.items.ItemRail;
import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.library.KeyBindings;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.net.KeyPressPacket;
import cam72cam.immersiverailroading.net.MRSSyncPacket;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailGag;
import cam72cam.immersiverailroading.tile.TileRailTESR;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = ImmersiveRailroading.MODID, name="ImmersiveRailroading", version = ImmersiveRailroading.VERSION)
public class ImmersiveRailroading
{
    public static final String MODID = "immersiverailroading";
    public static final String VERSION = "0.1";
	public static final int ENTITY_SYNC_DISTANCE = 128;
    
	@ObjectHolder(BlockRailGag.NAME)
	public static final BlockRailGag BLOCK_RAIL_GAG = new BlockRailGag();
	@ObjectHolder(BlockRail.NAME)
	public static BlockRail BLOCK_RAIL = new BlockRail();
	
	@ObjectHolder(ItemRollingStock.NAME)
	public static ItemRollingStock ITEM_ROLLING_STOCK = new ItemRollingStock();
	
	public static Logger logger;
	public static ImmersiveRailroading instance;
	
	public static final SimpleNetworkWrapper net = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
	
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        instance = this;
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    	net.registerMessage(MRSSyncPacket.Handler.class, MRSSyncPacket.class, 0, Side.CLIENT);
    	net.registerMessage(KeyPressPacket.Handler.class, KeyPressPacket.class, 1, Side.SERVER);
    	if (event.getSide() == Side.CLIENT) {
    		KeyBindings.registerKeyBindings();
    	}
    }
    
    @Mod.EventBusSubscriber(modid = MODID)
    public static class Registration
    {
        @SubscribeEvent
        public static void registerBlocks(RegistryEvent.Register<Block> event)
        {
    		event.getRegistry().register(BLOCK_RAIL_GAG);
    		event.getRegistry().register(BLOCK_RAIL);
        	GameRegistry.registerTileEntity(TileRailGag.class, BlockRailGag.NAME);
        	GameRegistry.registerTileEntity(TileRail.class, BlockRail.NAME);
        }

        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<Item> event)
        {
        	event.getRegistry().register(new ItemRail(BLOCK_RAIL).setRegistryName(BLOCK_RAIL.getRegistryName()));
        	event.getRegistry().register(ITEM_ROLLING_STOCK);
        }
        
        private static int lastEntityID = 0;
        private static void registerRollingStock(Class<? extends EntityRollingStock> type) {
        	lastEntityID ++;
        	EntityRegistry.registerModEntity(new ResourceLocation(MODID, type.getSimpleName()), type, type.getSimpleName(), lastEntityID, instance, ENTITY_SYNC_DISTANCE, 20, false);
            RenderingRegistry.registerEntityRenderingHandler(type, DefinitionManager.RENDER_INSTANCE);
        }
        
        @SubscribeEvent
        public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        	registerRollingStock(LocomotiveSteam.class);
        	registerRollingStock(LocomotiveDiesel.class);
        	registerRollingStock(LocomotiveElectric.class);
        }

        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent event)
        {
            OBJLoader.INSTANCE.addDomain(MODID.toLowerCase());
            
            ClientRegistry.bindTileEntitySpecialRenderer(TileRail.class, new TileRailTESR());
            for (TrackItems item : TrackItems.values()) {
	            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(BLOCK_RAIL), item.getMeta(), new ModelResourceLocation(BLOCK_RAIL.getRegistryName(), "inventory"));
            }
            
            ModelLoader.setCustomMeshDefinition(ITEM_ROLLING_STOCK, new ItemMeshDefinition() {
				@Override
				public ModelResourceLocation getModelLocation(ItemStack stack) {
					// TODO NBT or Damage
					return new ModelResourceLocation(ITEM_ROLLING_STOCK.getRegistryName(), ItemRollingStock.defFromStack(stack));
				}
            });
        }
        
        @SubscribeEvent
    	public static void onModelBakeEvent(ModelBakeEvent event)
    	{
        	for (String defID : DefinitionManager.getDefinitionNames()) {
        		ModelResourceLocation loc = new ModelResourceLocation(ITEM_ROLLING_STOCK.getRegistryName(), defID);
        		IBakedModel model = DefinitionManager.getDefinition(defID).getInventoryModel();
        		event.getModelRegistry().putObject(loc, model);
        	}
    	}
        
        @SubscribeEvent
        public static void onTextureStitchedPre(TextureStitchEvent.Pre event) {
        	// This is the first event after the model loaders have managers
        	DefinitionManager.initDefinitions();
        	
        	for(ResourceLocation texture : DefinitionManager.getTextures()) {
        		event.getMap().registerSprite(texture);
        	}
        }
        
        @SubscribeEvent
        public static void onKeyInput(InputEvent.KeyInputEvent event) {
			EntityPlayerSP player = Minecraft.getMinecraft().player;
    		for (KeyBindings binding : KeyBindings.values()) {
    			if (binding.isPressed()) {
    				EntityRollingStock riding = (EntityRollingStock)player.getRidingEntity();
    				if (riding != null) {
    					ImmersiveRailroading.net.sendToServer(new KeyPressPacket(binding, player.getEntityId(), riding.getEntityId()));
    				}
    			}
    		}
        }
    }
}
