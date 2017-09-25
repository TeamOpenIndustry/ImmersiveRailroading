package cam72cam.immersiverailroading;

import java.io.IOException;

import org.apache.logging.log4j.Logger;

import cam72cam.immersiverailroading.blocks.BlockRail;
import cam72cam.immersiverailroading.blocks.BlockRailGag;
import cam72cam.immersiverailroading.blocks.BlockRailPreview;
import cam72cam.immersiverailroading.blocks.BlockSteamHammer;
import cam72cam.immersiverailroading.items.ItemHook;
import cam72cam.immersiverailroading.items.ItemLargeWrench;
import cam72cam.immersiverailroading.items.ItemRail;
import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.items.ItemRollingStockComponent;
import cam72cam.immersiverailroading.items.ItemSteamHammer;
import cam72cam.immersiverailroading.proxy.ChunkManager;
import cam72cam.immersiverailroading.proxy.CommonProxy;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;

@Mod(modid = ImmersiveRailroading.MODID, name="ImmersiveRailroading", version = ImmersiveRailroading.VERSION, acceptedMinecraftVersions = "[1.12,1.13)")
public class ImmersiveRailroading
{
    public static final String MODID = "immersiverailroading";
    public static final String VERSION = "0.3.2";
	public static final int ENTITY_SYNC_DISTANCE = 512;
    public static final String ORE_RAIL_BED = "railBed";
    
	@ObjectHolder(BlockRailGag.NAME)
	public static final BlockRailGag BLOCK_RAIL_GAG = new BlockRailGag();
	@ObjectHolder(BlockRail.NAME)
	public static final BlockRail BLOCK_RAIL = new BlockRail();
	@ObjectHolder(BlockRailPreview.NAME)
	public static final BlockRailPreview BLOCK_RAIL_PREVIEW = new BlockRailPreview();
	
	@ObjectHolder(ItemRollingStock.NAME)
	public static ItemRollingStock ITEM_ROLLING_STOCK = new ItemRollingStock();
	
	@ObjectHolder(ItemRollingStockComponent.NAME)
	public static ItemRollingStockComponent ITEM_ROLLING_STOCK_COMPONENT = new ItemRollingStockComponent();
	
	@ObjectHolder(ItemLargeWrench.NAME)
	public static ItemLargeWrench ITEM_LARGE_WRENCH = new ItemLargeWrench();
	
	@ObjectHolder(ItemHook.NAME)
	public static ItemHook ITEM_HOOK = new ItemHook();
	
	public static Item ITEM_RAIL_BLOCK = new ItemRail(ImmersiveRailroading.BLOCK_RAIL).setRegistryName(ImmersiveRailroading.BLOCK_RAIL.getRegistryName());
	
	@ObjectHolder(BlockSteamHammer.NAME)
	public static BlockSteamHammer BLOCK_STEAM_HAMMER = new BlockSteamHammer();
	
	public static Item ITEM_STEAM_HAMMER = new ItemSteamHammer().setRegistryName(ImmersiveRailroading.BLOCK_STEAM_HAMMER.getRegistryName());
	
	public static Logger logger;
	public static ImmersiveRailroading instance;
	
	public static final SimpleNetworkWrapper net = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
	
	@SidedProxy(clientSide="cam72cam.immersiverailroading.proxy.ClientProxy", serverSide="cam72cam.immersiverailroading.proxy.ServerProxy")
	public static CommonProxy proxy;
	
	private ChunkManager chunker;
	
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) throws IOException {
        logger = event.getModLog();
        instance = this;
        
        World.MAX_ENTITY_RADIUS = 32;
        
    	proxy.preInit(event);
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    	proxy.init(event);
    }
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event) throws IOException {
		chunker = new ChunkManager();
		chunker.init();
    }
}
