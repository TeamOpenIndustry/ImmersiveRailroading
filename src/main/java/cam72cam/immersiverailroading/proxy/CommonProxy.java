package cam72cam.immersiverailroading.proxy;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.gui.*;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.multiblock.*;
import cam72cam.immersiverailroading.net.*;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.sound.ISound;
import cam72cam.immersiverailroading.thirdparty.CompatLoader;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailGag;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.OreHelper;
import cam72cam.mod.world.World;
import cam72cam.mod.block.IBreakCancelable;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.Registry;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import cam72cam.mod.util.Identifier;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
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
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistryModifiable;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@EventBusSubscriber(modid = ImmersiveRailroading.MODID)
public abstract class CommonProxy implements IGuiHandler {
	protected static List<Function<ModdedEntity, Entity>> entityClasses = new ArrayList<>();
	protected String configDir;
	private static String cacheDir;
    static {
    	entityClasses.add(LocomotiveSteam::new);
    	entityClasses.add(LocomotiveDiesel::new);
    	entityClasses.add(CarPassenger::new);
    	entityClasses.add(CarFreight::new);
    	entityClasses.add(CarTank::new);
    	entityClasses.add(Tender::new);
    	entityClasses.add(HandCar::new);
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
    	Config.init();
    	OreHelper.IR_RAIL_BED.add(Blocks.BRICK_BLOCK);
    	OreHelper.IR_RAIL_BED.add(Blocks.COBBLESTONE);
    	OreHelper.IR_RAIL_BED.add(new cam72cam.mod.item.ItemStack(Blocks.CONCRETE, 1, OreDictionary.WILDCARD_VALUE));
    	OreHelper.IR_RAIL_BED.add(Blocks.DIRT);
    	OreHelper.IR_RAIL_BED.add(Blocks.GRAVEL);
    	OreHelper.IR_RAIL_BED.add(new cam72cam.mod.item.ItemStack(Blocks.HARDENED_CLAY, 1, OreDictionary.WILDCARD_VALUE));
    	OreHelper.IR_RAIL_BED.add(new cam72cam.mod.item.ItemStack(Blocks.LOG, 1, OreDictionary.WILDCARD_VALUE));
    	OreHelper.IR_RAIL_BED.add(new cam72cam.mod.item.ItemStack(Blocks.LOG2, 1, OreDictionary.WILDCARD_VALUE));
    	OreHelper.IR_RAIL_BED.add(Blocks.NETHER_BRICK);
    	OreHelper.IR_RAIL_BED.add(new cam72cam.mod.item.ItemStack(Blocks.PLANKS, 1, OreDictionary.WILDCARD_VALUE));
    }
    
    public void init(FMLInitializationEvent event) {
    	Packet.register(MRSSyncPacket.class, PacketDirection.ServerToClient);
    	Packet.register(KeyPressPacket.class, PacketDirection.ClientToServer);
    	Packet.register(MousePressPacket.class, PacketDirection.ClientToServer);
    	Packet.register(ItemRailUpdatePacket.class, PacketDirection.ClientToServer);
    	Packet.register(BuildableStockSyncPacket.class, PacketDirection.ServerToClient);
    	Packet.register(MultiblockSelectCraftPacket.class, PacketDirection.ClientToServer);
    	Packet.register(SoundPacket.class, PacketDirection.ServerToClient);
    	Packet.register(PaintSyncPacket.class, PacketDirection.ServerToClient);
        Packet.register(PreviewRenderPacket.class, PacketDirection.ServerToClient);
        Packet.register(ModdedEntity.PassengerPositionsPacket.class, PacketDirection.ServerToClient);

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
    
    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
    	IForgeRegistryModifiable<IRecipe> modRegistry = (IForgeRegistryModifiable<IRecipe>) event.getRegistry();
    	if (!OreDictionary.doesOreNameExist("ingotSteel")) {
    		modRegistry.remove(new ResourceLocation("immersiverailroading:wrench"));
    		modRegistry.remove(new ResourceLocation("immersiverailroading:hook"));
    		modRegistry.remove(new ResourceLocation("immersiverailroading:manual"));
    		modRegistry.remove(new ResourceLocation("immersiverailroading:track blueprint"));
    		modRegistry.remove(new ResourceLocation("immersiverailroading:switch key"));
    	} else {
    		modRegistry.remove(new ResourceLocation("immersiverailroading:wrench_iron"));
    		modRegistry.remove(new ResourceLocation("immersiverailroading:hook_iron"));
    		modRegistry.remove(new ResourceLocation("immersiverailroading:manual_iron"));
    		modRegistry.remove(new ResourceLocation("immersiverailroading:track blueprint_iron"));
    		modRegistry.remove(new ResourceLocation("immersiverailroading:switch key_iron"));
    	}
    }
    
    @SuppressWarnings("deprecation")
	@SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event)
    {
		event.getRegistry().register(IRBlocks.BLOCK_RAIL_GAG.internal);
		event.getRegistry().register(IRBlocks.BLOCK_RAIL.internal);
		event.getRegistry().register(IRBlocks.BLOCK_RAIL_PREVIEW.internal);
		event.getRegistry().register(IRBlocks.BLOCK_MULTIBLOCK.internal);
    	GameRegistry.registerTileEntity(TileRailGag.class, IRBlocks.BLOCK_RAIL_GAG.getName());
    	GameRegistry.registerTileEntity(TileRail.class, IRBlocks.BLOCK_RAIL.getName());
    	GameRegistry.registerTileEntity(TileRailPreview.class, IRBlocks.BLOCK_RAIL_PREVIEW.getName());
    	GameRegistry.registerTileEntity(TileMultiblock.class, IRBlocks.BLOCK_MULTIBLOCK.getName());
    }
    
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
    	event.getRegistry().register(IRItems.ITEM_TRACK_BLUEPRINT.internal);
    	event.getRegistry().register(IRItems.ITEM_ROLLING_STOCK.internal);
    	event.getRegistry().register(IRItems.ITEM_ROLLING_STOCK_COMPONENT.internal);
    	event.getRegistry().register(IRItems.ITEM_LARGE_WRENCH.internal);
    	event.getRegistry().register(IRItems.ITEM_HOOK.internal);
    	event.getRegistry().register(IRItems.ITEM_AUGMENT.internal);
    	event.getRegistry().register(IRItems.ITEM_MANUAL.internal);
    	event.getRegistry().register(IRItems.ITEM_RAIL.internal);
    	event.getRegistry().register(IRItems.ITEM_PLATE.internal);
    	event.getRegistry().register(IRItems.ITEM_CAST_RAIL.internal);
    	event.getRegistry().register(IRItems.ITEM_CONDUCTOR_WHISTLE.internal);
    	event.getRegistry().register(IRItems.ITEM_PAINT_BRUSH.internal);
    	event.getRegistry().register(IRItems.ITEM_GOLDEN_SPIKE.internal);
      event.getRegistry().register(IRItems.ITEM_RADIO_CONTROL_CARD.internal);
    	event.getRegistry().register(IRItems.ITEM_SWITCH_KEY.internal);
    }
    
    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
    	for (Function<ModdedEntity, Entity> type : entityClasses) {
			Registry.register(ImmersiveRailroading.MODID, type, EntityRollingStock.settings);
    	}
    }
	
	@SubscribeEvent
	public static void onBlockBreakEvent(BreakEvent event) {
		Block block = event.getWorld().getBlockState(event.getPos()).getBlock();
		if (block instanceof IBreakCancelable) {
			IBreakCancelable cancelable = (IBreakCancelable) block;
			if (!cancelable.tryBreak(World.get(event.getWorld()), new Vec3i(event.getPos()), new Player(event.getPlayer()))) {
				event.setCanceled(true);
				//TODO updateListeners?
			}
		}
	}
	
	@SubscribeEvent
	public static void onWorldTick(WorldTickEvent event) {
		if (event.phase != Phase.START) {
			return;
		}
		
		
		
		if (!event.world.isRemote) {
			ChunkManager.handleWorldTick(event.world);
			World world = World.get(event.world);
			// We do this here as to let all the entities do their onTick first.  Otherwise some might be one onTick ahead
			// if we did this in the onUpdate method
			List<EntityCoupleableRollingStock> entities = world.getEntities(EntityCoupleableRollingStock.class);
			
			// Try locomotives first
			for (EntityCoupleableRollingStock stock : entities) {
				if (stock instanceof Locomotive) {
					stock = stock.findByUUID(stock.getUUID());
					stock.tickPosRemainingCheck();
				}
			}
			// Try rest
			for (EntityCoupleableRollingStock stock : entities) {
				stock = stock.findByUUID(stock.getUUID());
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


	public abstract List<InputStream> getResourceStreamAll(Identifier modelLoc) throws IOException;

	public InputStream getResourceStream(Identifier location) throws IOException {
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

    
    protected String pathString(Identifier location, boolean startingSlash) {
    	return (startingSlash ? "/" : "") + "assets/" + location.getDomain() + "/" + location.getPath();
    }
    
    protected List<InputStream> getFileResourceStreams(Identifier location) throws IOException {
    	List<InputStream> streams = new ArrayList<InputStream>();
    	File folder = new File(this.configDir);
    	if (folder.exists()) {
    		if (folder.isDirectory()) {
	    		File[] files = folder.listFiles((dir, name) -> name.endsWith(".zip"));
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
	
	public ISound newSound(Identifier oggLocation, boolean repeats, float attenuationDistance, Gauge gauge) {
		return null;
	}

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, net.minecraft.world.World worldIn, int entityIDorX, int y, int z) {
		World world = World.get(worldIn);

    	switch(GuiTypes.values()[ID]) {
		case FREIGHT:
	    	return new FreightContainer(player.inventory, world.getEntity(entityIDorX, CarFreight.class));
		case TANK:
		case DIESEL_LOCOMOTIVE:
	    	return new TankContainer(player.inventory, world.getEntity(entityIDorX, FreightTank.class));
		case TENDER:
			return new TenderContainer(player.inventory, world.getEntity(entityIDorX, Tender.class));
		case STEAM_LOCOMOTIVE:
			return new SteamLocomotiveContainer(player.inventory, world.getEntity(entityIDorX, LocomotiveSteam.class));
		case STEAM_HAMMER:
			TileMultiblock te = world.getTileEntity(new Vec3i(entityIDorX, y, z), TileMultiblock.class);
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

    public abstract void addPreview(int dimension, TileRailPreview preview);
}
