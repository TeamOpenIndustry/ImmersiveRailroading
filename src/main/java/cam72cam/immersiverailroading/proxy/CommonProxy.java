package cam72cam.immersiverailroading.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.blocks.BlockRail;
import cam72cam.immersiverailroading.blocks.BlockRailGag;
import cam72cam.immersiverailroading.entity.CarFreight;
import cam72cam.immersiverailroading.entity.CarPassenger;
import cam72cam.immersiverailroading.entity.CarTank;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.entity.LocomotiveElectric;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.entity.Tender;
import cam72cam.immersiverailroading.entity.registry.DefinitionManager;
import cam72cam.immersiverailroading.items.ItemRail;
import cam72cam.immersiverailroading.net.CoupleStatusPacket;
import cam72cam.immersiverailroading.net.KeyPressPacket;
import cam72cam.immersiverailroading.net.MRSSyncPacket;
import cam72cam.immersiverailroading.net.PassengerPositionsPacket;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailGag;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

@EventBusSubscriber(modid = ImmersiveRailroading.MODID)
public abstract class CommonProxy implements IGuiHandler {
    protected static List<Class<? extends EntityRollingStock>> entityClasses = new ArrayList<Class<? extends EntityRollingStock>>();
    static {
    	entityClasses.add(LocomotiveSteam.class);
    	entityClasses.add(LocomotiveDiesel.class);
    	entityClasses.add(LocomotiveElectric.class);
    	entityClasses.add(CarPassenger.class);
    	entityClasses.add(CarFreight.class);
    	entityClasses.add(CarTank.class);
    	entityClasses.add(Tender.class);
    }
	
    public void preInit(FMLPreInitializationEvent event) {
    	DefinitionManager.initDefinitions();
    }
    
    public void init(FMLInitializationEvent event) {
    	ImmersiveRailroading.net.registerMessage(MRSSyncPacket.Handler.class, MRSSyncPacket.class, 0, Side.CLIENT);
    	ImmersiveRailroading.net.registerMessage(KeyPressPacket.Handler.class, KeyPressPacket.class, 1, Side.SERVER);
    	ImmersiveRailroading.net.registerMessage(PassengerPositionsPacket.Handler.class, PassengerPositionsPacket.class, 2, Side.CLIENT);
    	ImmersiveRailroading.net.registerMessage(CoupleStatusPacket.Handler.class, CoupleStatusPacket.class, 3, Side.CLIENT);
    	
    	NetworkRegistry.INSTANCE.registerGuiHandler(ImmersiveRailroading.instance, this);
    }
    
    public abstract World getWorld(int dimension);
    
    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event)
    {
		event.getRegistry().register(ImmersiveRailroading.BLOCK_RAIL_GAG);
		event.getRegistry().register(ImmersiveRailroading.BLOCK_RAIL);
    	GameRegistry.registerTileEntity(TileRailGag.class, BlockRailGag.NAME);
    	GameRegistry.registerTileEntity(TileRail.class, BlockRail.NAME);
    }
    
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
    	event.getRegistry().register(new ItemRail(ImmersiveRailroading.BLOCK_RAIL).setRegistryName(ImmersiveRailroading.BLOCK_RAIL.getRegistryName()));
    	event.getRegistry().register(ImmersiveRailroading.ITEM_ROLLING_STOCK);
    }
    
    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
    	int lastEntityID = 0;
    	for (Class<? extends EntityRollingStock> type : entityClasses) {
        	lastEntityID ++;
        	EntityRegistry.registerModEntity(new ResourceLocation(ImmersiveRailroading.MODID, type.getSimpleName()), type, type.getSimpleName(), lastEntityID, ImmersiveRailroading.instance, ImmersiveRailroading.ENTITY_SYNC_DISTANCE, 20, false);	
    	}
    }
    
    @SubscribeEvent
    public static void onEntitySpawnHack(EntityJoinWorldEvent event) {
    	if (event.getEntity() instanceof EntityRollingStock) {
        	EntityRollingStock entity = (EntityRollingStock)event.getEntity();
        	entity.tryRollingStockInit();
    	}
    }

	public abstract InputStream getResourceStream(ResourceLocation modelLoc) throws IOException;
}
