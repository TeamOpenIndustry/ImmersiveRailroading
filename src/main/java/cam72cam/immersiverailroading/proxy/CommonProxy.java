package cam72cam.immersiverailroading.proxy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;

import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.IRItems;
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
import cam72cam.immersiverailroading.entity.HandCar;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.entity.Tender;
import cam72cam.immersiverailroading.gui.FreightContainer;
import cam72cam.immersiverailroading.gui.SteamHammerContainer;
import cam72cam.immersiverailroading.gui.SteamLocomotiveContainer;
import cam72cam.immersiverailroading.gui.TankContainer;
import cam72cam.immersiverailroading.gui.TenderContainer;
import cam72cam.immersiverailroading.library.Gauge;
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
import cam72cam.immersiverailroading.net.MultiblockSelectCraftPacket;
import cam72cam.immersiverailroading.net.PassengerPositionsPacket;
import cam72cam.immersiverailroading.net.SoundPacket;
import cam72cam.immersiverailroading.net.PaintSyncPacket;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.sound.ISound;
import cam72cam.immersiverailroading.thirdparty.CompatLoader;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailGag;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.OreHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistryModifiable;

@EventBusSubscriber(modid = ImmersiveRailroading.MODID)
public abstract class CommonProxy implements IGuiHandler {
	protected static List<Class<? extends EntityRollingStock>> entityClasses = new ArrayList<Class<? extends EntityRollingStock>>();
	protected String configDir;
	private static String cacheDir;
    static {
    	entityClasses.add(LocomotiveSteam.class);
    	entityClasses.add(LocomotiveDiesel.class);
    	entityClasses.add(CarPassenger.class);
    	entityClasses.add(CarFreight.class);
    	entityClasses.add(CarTank.class);
    	entityClasses.add(Tender.class);
    	entityClasses.add(HandCar.class);
    }
    
    public static String getCacheFile(String fname) {
    	return cacheDir + fname;
    }
    
    public void preInit(FMLPreInitializationEvent event) throws IOException {
    	configDir = event.getModConfigurationDirectory().getAbsolutePath() + File.separator + ImmersiveRailroading.MODID;
    	// foo/config/immersiverailroading/../../cache/fname
    	cacheDir = configDir + File.separator + ".." + File.separator + ".." + File.separator + "cache" + File.separator;
    	new File(configDir).mkdirs();
    	new File(cacheDir).mkdirs();
    	
    	DefinitionManager.initDefinitions();
    	;
    	OreHelper.IR_RAIL_BED.add(Blocks.BRICK_BLOCK);
    	OreHelper.IR_RAIL_BED.add(Blocks.COBBLESTONE);
    	OreHelper.IR_RAIL_BED.add(new ItemStack(Blocks.CONCRETE, 1, OreDictionary.WILDCARD_VALUE));
    	OreHelper.IR_RAIL_BED.add(Blocks.DIRT);
    	OreHelper.IR_RAIL_BED.add(Blocks.GRAVEL);
    	OreHelper.IR_RAIL_BED.add(new ItemStack(Blocks.HARDENED_CLAY, 1, OreDictionary.WILDCARD_VALUE));
    	OreHelper.IR_RAIL_BED.add(new ItemStack(Blocks.LOG, 1, OreDictionary.WILDCARD_VALUE));
    	OreHelper.IR_RAIL_BED.add(new ItemStack(Blocks.LOG2, 1, OreDictionary.WILDCARD_VALUE));
    	OreHelper.IR_RAIL_BED.add(Blocks.NETHER_BRICK);
    	OreHelper.IR_RAIL_BED.add(new ItemStack(Blocks.PLANKS, 1, OreDictionary.WILDCARD_VALUE));
    }
    
    public void init(FMLInitializationEvent event) {
    	ImmersiveRailroading.net.registerMessage(MRSSyncPacket.Handler.class, MRSSyncPacket.class, 0, Side.CLIENT);
    	ImmersiveRailroading.net.registerMessage(KeyPressPacket.Handler.class, KeyPressPacket.class, 1, Side.SERVER);
    	ImmersiveRailroading.net.registerMessage(PassengerPositionsPacket.Handler.class, PassengerPositionsPacket.class, 2, Side.CLIENT);
    	ImmersiveRailroading.net.registerMessage(MousePressPacket.Handler.class, MousePressPacket.class, 6, Side.SERVER);
    	ImmersiveRailroading.net.registerMessage(ItemRailUpdatePacket.Handler.class, ItemRailUpdatePacket.class, 7, Side.SERVER);
    	ImmersiveRailroading.net.registerMessage(BuildableStockSyncPacket.Handler.class, BuildableStockSyncPacket.class, 8, Side.CLIENT);
    	ImmersiveRailroading.net.registerMessage(MultiblockSelectCraftPacket.Handler.class, MultiblockSelectCraftPacket.class, 9, Side.SERVER);
    	ImmersiveRailroading.net.registerMessage(SoundPacket.Handler.class, SoundPacket.class, 10, Side.CLIENT);
    	ImmersiveRailroading.net.registerMessage(PaintSyncPacket.Handler.class, PaintSyncPacket.class, 11, Side.CLIENT);

    	NetworkRegistry.INSTANCE.registerGuiHandler(ImmersiveRailroading.instance, this);
    	
    	CompatLoader.load();
    	
    	MultiblockRegistry.register(SteamHammerMultiblock.NAME, new SteamHammerMultiblock());
    	MultiblockRegistry.register(PlateRollerMultiblock.NAME, new PlateRollerMultiblock());
    	MultiblockRegistry.register(RailRollerMultiblock.NAME, new RailRollerMultiblock());
    	MultiblockRegistry.register(BoilerRollerMultiblock.NAME, new BoilerRollerMultiblock());
    	MultiblockRegistry.register(CastingMultiblock.NAME, new CastingMultiblock());
    }
    

	public void serverStarting(FMLServerStartingEvent event) {
		event.registerServerCommand(new IRCommand());
	}
    
    public abstract World getWorld(int dimension);
    
    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
    	IForgeRegistryModifiable<IRecipe> modRegistry = (IForgeRegistryModifiable<IRecipe>) event.getRegistry();
    	if (!OreDictionary.doesOreNameExist("ingotSteel")) {
    		modRegistry.remove(new ResourceLocation("immersiverailroading:wrench"));
    		modRegistry.remove(new ResourceLocation("immersiverailroading:hook"));
    		modRegistry.remove(new ResourceLocation("immersiverailroading:manual"));
    		modRegistry.remove(new ResourceLocation("immersiverailroading:track blueprint"));
    	} else {
    		modRegistry.remove(new ResourceLocation("immersiverailroading:wrench_iron"));
    		modRegistry.remove(new ResourceLocation("immersiverailroading:hook_iron"));
    		modRegistry.remove(new ResourceLocation("immersiverailroading:manual_iron"));
    		modRegistry.remove(new ResourceLocation("immersiverailroading:track blueprint_iron"));
    	}
    }
    
    @SuppressWarnings("deprecation")
	@SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event)
    {
		event.getRegistry().register(IRBlocks.BLOCK_RAIL_GAG);
		event.getRegistry().register(IRBlocks.BLOCK_RAIL);
		event.getRegistry().register(IRBlocks.BLOCK_RAIL_PREVIEW);
		event.getRegistry().register(IRBlocks.BLOCK_MULTIBLOCK);
    	GameRegistry.registerTileEntity(TileRailGag.class, BlockRailGag.NAME);
    	GameRegistry.registerTileEntity(TileRail.class, BlockRail.NAME);
    	GameRegistry.registerTileEntity(TileRailPreview.class, BlockRailPreview.NAME);
    	GameRegistry.registerTileEntity(TileMultiblock.class, BlockMultiblock.NAME);
    }
    
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
    	event.getRegistry().register(IRItems.ITEM_TRACK_BLUEPRINT);
    	event.getRegistry().register(IRItems.ITEM_ROLLING_STOCK);
    	event.getRegistry().register(IRItems.ITEM_ROLLING_STOCK_COMPONENT);
    	event.getRegistry().register(IRItems.ITEM_LARGE_WRENCH);
    	event.getRegistry().register(IRItems.ITEM_HOOK);
    	event.getRegistry().register(IRItems.ITEM_AUGMENT);
    	event.getRegistry().register(IRItems.ITEM_MANUAL);
    	event.getRegistry().register(IRItems.ITEM_RAIL);
    	event.getRegistry().register(IRItems.ITEM_PLATE);
    	event.getRegistry().register(IRItems.ITEM_CAST_RAIL);
    	event.getRegistry().register(IRItems.ITEM_CONDUCTOR_WHISTLE);
    	event.getRegistry().register(IRItems.ITEM_PAINT_BRUSH);
    	event.getRegistry().register(IRItems.ITEM_GOLDEN_SPIKE);
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
		if (event.phase != Phase.START) {
			return;
		}
		
		
		
		if (!event.world.isRemote) {
			ChunkManager.handleWorldTick(event.world);
			WorldServer world = event.world.getMinecraftServer().getWorld(event.world.provider.getDimension());
			// We do this here as to let all the entities do their tick first.  Otherwise some might be one tick ahead
			// if we did this in the onUpdate method
			List<EntityCoupleableRollingStock> entities = world.getEntities(EntityCoupleableRollingStock.class, EntitySelectors.IS_ALIVE);
			
			// Try locomotives first
			for (EntityCoupleableRollingStock stock : entities) {
				if (stock instanceof Locomotive) {
					stock = stock.findByUUID(stock.getPersistentID());
					stock.tickPosRemainingCheck();
				}
			}
			// Try rest
			for (EntityCoupleableRollingStock stock : entities) {
				stock = stock.findByUUID(stock.getPersistentID());
				stock.tickPosRemainingCheck();
			}
			
			try {
				Thread.sleep(ConfigDebug.lagServer);
			} catch (InterruptedException e) {
				ImmersiveRailroading.catching(e);
			}
		}
	}
	
	public abstract int getTicks();


	public abstract List<InputStream> getResourceStreamAll(ResourceLocation modelLoc) throws IOException;

	public InputStream getResourceStream(ResourceLocation location) throws IOException {
		InputStream chosen = null;
		for (InputStream strm : getResourceStreamAll(location)) {
			if (chosen == null) {
				chosen = strm;
			} else {
				strm.close();
			}
		}
		if (chosen == null) {
			throw new java.io.FileNotFoundException(location.toString());
		}
		return chosen;
	}

    
    protected String pathString(ResourceLocation location, boolean startingSlash) {
    	return (startingSlash ? "/" : "") + "assets/" + location.getResourceDomain() + "/" + location.getResourcePath();
    }
    
    protected List<InputStream> getFileResourceStreams(ResourceLocation location) throws IOException {
    	List<InputStream> streams = new ArrayList<InputStream>();
    	File folder = new File(this.configDir);
    	if (folder.exists()) {
    		if (folder.isDirectory()) {
	    		File[] files = folder.listFiles(new FilenameFilter() {
				    @Override
				    public boolean accept(File dir, String name) {
				        return name.endsWith(".zip");
				    }
				});
	    		for (File file : files) {
	    			ZipFile resourcePack = new ZipFile(file);
	    			ZipEntry entry = resourcePack.getEntry(pathString(location, false));
	    			if (entry != null) {
	    				// Copy the input stream so we can close the resource pack
	    				InputStream stream = resourcePack.getInputStream(entry);
	    				streams.add(new ByteArrayInputStream(IOUtils.toByteArray(stream)));
	    			}
	    			resourcePack.close();
	    		}
    		} else {
    			ImmersiveRailroading.error("Expecting " + this.configDir + " to be a directory");
    		}
    	} else {
			folder.mkdirs();
    	}
		return streams;
    }
	
	public ISound newSound(ResourceLocation oggLocation, boolean repeats, float attenuationDistance, Gauge gauge) {
		return null;
	}

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

	public int getRenderDistance() {
		return 8;
	}
	
	public static double getServerTPS(World world, double sampleSize) {
		long[] ttl = world.getMinecraftServer().tickTimeArray;
		
		sampleSize = Math.min(sampleSize, ttl.length);
		double ttus = 0;
		for (int i = 0; i < sampleSize; i++) {
			ttus += ttl[ttl.length - 1 - i] / sampleSize;
		}
		
		if (ttus == 0) {
			ttus = 0.01;
		}
		
		double ttms = ttus * 1.0E-6D;
		return Math.min(1000.0 / ttms, 20);
	}
}
