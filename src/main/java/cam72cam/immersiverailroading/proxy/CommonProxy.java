package cam72cam.immersiverailroading.proxy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.blocks.BlockMultiblock;
import cam72cam.immersiverailroading.blocks.BlockRail;
import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.blocks.BlockRailGag;
import cam72cam.immersiverailroading.blocks.BlockRailPreview;
import cam72cam.immersiverailroading.entity.CarFreight;
import cam72cam.immersiverailroading.entity.CarPassenger;
import cam72cam.immersiverailroading.entity.CarTank;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.FreightTank;
import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.entity.Tender;
import cam72cam.immersiverailroading.gui.FreightContainer;
import cam72cam.immersiverailroading.gui.SteamHammerContainer;
import cam72cam.immersiverailroading.gui.SteamLocomotiveContainer;
import cam72cam.immersiverailroading.gui.TankContainer;
import cam72cam.immersiverailroading.gui.TenderContainer;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.multiblock.BoilerRollerMultiblock;
import cam72cam.immersiverailroading.multiblock.CastingMultiblock;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.immersiverailroading.multiblock.PlateRollerMultiblock;
import cam72cam.immersiverailroading.multiblock.RailRollerMultiblock;
import cam72cam.immersiverailroading.multiblock.SteamHammerMultiblock;
import cam72cam.immersiverailroading.net.BuildableStockSyncPacket;
import cam72cam.immersiverailroading.net.ItemRailUpdatePacket;
import cam72cam.immersiverailroading.net.KeyPressPacket;
import cam72cam.immersiverailroading.net.MRSSyncPacket;
import cam72cam.immersiverailroading.net.MousePressPacket;
import cam72cam.immersiverailroading.net.PassengerPositionsPacket;
import cam72cam.immersiverailroading.net.MultiblockSelectCraftPacket;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailGag;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;

@EventBusSubscriber(modid = ImmersiveRailroading.MODID)
public abstract class CommonProxy implements IGuiHandler {
	protected static List<Class<? extends EntityRollingStock>> entityClasses = new ArrayList<Class<? extends EntityRollingStock>>();
	protected String configDir;
    static {
    	entityClasses.add(LocomotiveSteam.class);
    	entityClasses.add(LocomotiveDiesel.class);
    	entityClasses.add(CarPassenger.class);
    	entityClasses.add(CarFreight.class);
    	entityClasses.add(CarTank.class);
    	entityClasses.add(Tender.class);
    }
    
    public void preInit(FMLPreInitializationEvent event) throws IOException {
    	configDir = event.getModConfigurationDirectory().getAbsolutePath() + File.separator + ImmersiveRailroading.MODID;
    	DefinitionManager.initDefinitions();
    	OreDictionary.registerOre(ImmersiveRailroading.ORE_RAIL_BED, Blocks.BRICK_BLOCK);
    	OreDictionary.registerOre(ImmersiveRailroading.ORE_RAIL_BED, Blocks.COBBLESTONE);
    	OreDictionary.registerOre(ImmersiveRailroading.ORE_RAIL_BED, new ItemStack(Blocks.CONCRETE, 1, OreDictionary.WILDCARD_VALUE));
    	OreDictionary.registerOre(ImmersiveRailroading.ORE_RAIL_BED, Blocks.DIRT);
    	OreDictionary.registerOre(ImmersiveRailroading.ORE_RAIL_BED, Blocks.GRAVEL);
    	OreDictionary.registerOre(ImmersiveRailroading.ORE_RAIL_BED, new ItemStack(Blocks.HARDENED_CLAY, 1, OreDictionary.WILDCARD_VALUE));
    	OreDictionary.registerOre(ImmersiveRailroading.ORE_RAIL_BED, new ItemStack(Blocks.LOG, 1, OreDictionary.WILDCARD_VALUE));
    	OreDictionary.registerOre(ImmersiveRailroading.ORE_RAIL_BED, new ItemStack(Blocks.LOG2, 1, OreDictionary.WILDCARD_VALUE));
    	OreDictionary.registerOre(ImmersiveRailroading.ORE_RAIL_BED, Blocks.NETHER_BRICK);
    	OreDictionary.registerOre(ImmersiveRailroading.ORE_RAIL_BED, new ItemStack(Blocks.PLANKS, 1, OreDictionary.WILDCARD_VALUE));
    }
    
    public void init(FMLInitializationEvent event) {
    	ImmersiveRailroading.net.registerMessage(MRSSyncPacket.Handler.class, MRSSyncPacket.class, 0, Side.CLIENT);
    	ImmersiveRailroading.net.registerMessage(KeyPressPacket.Handler.class, KeyPressPacket.class, 1, Side.SERVER);
    	ImmersiveRailroading.net.registerMessage(PassengerPositionsPacket.Handler.class, PassengerPositionsPacket.class, 2, Side.CLIENT);
    	ImmersiveRailroading.net.registerMessage(MousePressPacket.Handler.class, MousePressPacket.class, 6, Side.SERVER);
    	ImmersiveRailroading.net.registerMessage(ItemRailUpdatePacket.Handler.class, ItemRailUpdatePacket.class, 7, Side.SERVER);
    	ImmersiveRailroading.net.registerMessage(BuildableStockSyncPacket.Handler.class, BuildableStockSyncPacket.class, 8, Side.CLIENT);
    	ImmersiveRailroading.net.registerMessage(MultiblockSelectCraftPacket.Handler.class, MultiblockSelectCraftPacket.class, 9, Side.SERVER);
    	
    	MultiblockRegistry.register(SteamHammerMultiblock.NAME, new SteamHammerMultiblock());
    	MultiblockRegistry.register(PlateRollerMultiblock.NAME, new PlateRollerMultiblock());
    	MultiblockRegistry.register(RailRollerMultiblock.NAME, new RailRollerMultiblock());
    	MultiblockRegistry.register(BoilerRollerMultiblock.NAME, new BoilerRollerMultiblock());
    	MultiblockRegistry.register(CastingMultiblock.NAME, new CastingMultiblock());
    	
    	NetworkRegistry.INSTANCE.registerGuiHandler(ImmersiveRailroading.instance, this);
    }
    
	public void serverStarting(FMLServerStartingEvent event) {
	}
    
    public abstract World getWorld(int dimension);
    
    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event)
    {
		event.getRegistry().register(ImmersiveRailroading.BLOCK_RAIL_GAG);
		event.getRegistry().register(ImmersiveRailroading.BLOCK_RAIL);
		event.getRegistry().register(ImmersiveRailroading.BLOCK_RAIL_PREVIEW);
		event.getRegistry().register(ImmersiveRailroading.BLOCK_MULTIBLOCK);
    	GameRegistry.registerTileEntity(TileRailGag.class, BlockRailGag.NAME);
    	GameRegistry.registerTileEntity(TileRail.class, BlockRail.NAME);
    	GameRegistry.registerTileEntity(TileRailPreview.class, BlockRailPreview.NAME);
    	GameRegistry.registerTileEntity(TileMultiblock.class, BlockMultiblock.NAME);
    }
    
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
    	event.getRegistry().register(ImmersiveRailroading.ITEM_RAIL_BLOCK);
    	event.getRegistry().register(ImmersiveRailroading.ITEM_ROLLING_STOCK);
    	event.getRegistry().register(ImmersiveRailroading.ITEM_ROLLING_STOCK_COMPONENT);
    	event.getRegistry().register(ImmersiveRailroading.ITEM_LARGE_WRENCH);
    	event.getRegistry().register(ImmersiveRailroading.ITEM_HOOK);
    	event.getRegistry().register(ImmersiveRailroading.ITEM_AUGMENT);
    	event.getRegistry().register(ImmersiveRailroading.ITEM_MANUAL);
    	event.getRegistry().register(ImmersiveRailroading.ITEM_RAIL);
    	event.getRegistry().register(ImmersiveRailroading.ITEM_PLATE);
    	event.getRegistry().register(ImmersiveRailroading.ITEM_CAST_RAIL);
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
	public static void onBlockBreakEvent(BreakEvent event) {
		if (!BlockRailBase.tryBreakRail(event.getWorld(), event.getPos())) {
			event.setCanceled(true);
		} else if (BlockRailPreview.tryBreakPreview(event.getWorld(), event.getPos(), event.getPlayer())) {
			event.setCanceled(true);
		}
	}
	
	@SubscribeEvent
	public static void onWorldTick(WorldTickEvent event) {
		if (!event.world.isRemote) {
			ChunkManager.handleWorldTick(event.world);
			// We do this here as to let all the entities do their tick first.  Otherwise some might be one tick ahead
			// if we did this in the onUpdate method
			List<EntityCoupleableRollingStock> entities = event.world.getEntities(EntityCoupleableRollingStock.class, EntitySelectors.IS_ALIVE);
			for (EntityCoupleableRollingStock stock : entities) {
				stock.tickPosRemainingCheck();
			}
		}
	}

	public abstract InputStream getResourceStream(ResourceLocation modelLoc) throws IOException;
	public abstract List<InputStream> getResourceStreamAll(ResourceLocation modelLoc) throws IOException;
	


    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int entityIDorX, int y, int z) {
    	switch(GuiTypes.values()[ID]) {
		case FREIGHT:
	    	return new FreightContainer(player.inventory, (CarFreight) world.getEntityByID(entityIDorX));
		case TANK:
		case DIESEL_LOCOMOTIVE:
	    	return new TankContainer(player.inventory, (FreightTank) world.getEntityByID(entityIDorX));
		case TENDER:
			return new TenderContainer(player.inventory, (Tender) world.getEntityByID(entityIDorX));
		case STEAM_LOCOMOTIVE:
			return new SteamLocomotiveContainer(player.inventory, (LocomotiveSteam) world.getEntityByID(entityIDorX));
		case STEAM_HAMMER:
			TileMultiblock te = TileMultiblock.get(world, new BlockPos(entityIDorX, y, z));
			if (te == null) {
				return null;
			}
			return new SteamHammerContainer(player.inventory, te);
		default:
			return null;
    	}
    }
}
