package cam72cam.immersiverailroading.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.blocks.BlockRail;
import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.blocks.BlockRailGag;
import cam72cam.immersiverailroading.entity.CarFreight;
import cam72cam.immersiverailroading.entity.CarPassenger;
import cam72cam.immersiverailroading.entity.CarTank;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.FreightTank;
import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.entity.Tender;
import cam72cam.immersiverailroading.gui.FreightContainer;
import cam72cam.immersiverailroading.gui.SteamLocomotiveContainer;
import cam72cam.immersiverailroading.gui.TankContainer;
import cam72cam.immersiverailroading.gui.TenderContainer;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.net.CoupleStatusPacket;
import cam72cam.immersiverailroading.net.KeyPressPacket;
import cam72cam.immersiverailroading.net.MRSSyncPacket;
import cam72cam.immersiverailroading.net.PassengerPositionsPacket;
import cam72cam.immersiverailroading.net.SnowRenderUpdatePacket;
import cam72cam.immersiverailroading.net.SwitchStatePacket;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailGag;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
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
    	ImmersiveRailroading.net.registerMessage(SnowRenderUpdatePacket.Handler.class, SnowRenderUpdatePacket.class, 4, Side.CLIENT);
    	ImmersiveRailroading.net.registerMessage(SwitchStatePacket.Handler.class, SwitchStatePacket.class, 5, Side.CLIENT);
    	
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
    	event.getRegistry().register(ImmersiveRailroading.ITEM_RAIL_BLOCK);
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
	
	@SubscribeEvent
	public static void onBlockBreakEvent(BreakEvent event) {
		if (!BlockRailBase.tryBreakRail(event.getWorld(), event.getPos())) {
			event.setCanceled(true);
		}
	}

	public abstract InputStream getResourceStream(ResourceLocation modelLoc) throws IOException;
	


    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int entityID, int nop1, int nop2) {
    	switch(GuiTypes.values()[ID]) {
		case FREIGHT:
	    	return new FreightContainer(player.inventory, (CarFreight) world.getEntityByID(entityID));
		case TANK:
		case DIESEL_LOCOMOTIVE:
	    	return new TankContainer(player.inventory, (FreightTank) world.getEntityByID(entityID));
		case TENDER:
			return new TenderContainer(player.inventory, (Tender) world.getEntityByID(entityID));
		case STEAM_LOCOMOTIVE:
			return new SteamLocomotiveContainer(player.inventory, (LocomotiveSteam) world.getEntityByID(entityID));
		default:
			return null;
    	}
    }
}
