package cam72cam.immersiverailroading;

import org.apache.logging.log4j.Logger;

import cam72cam.immersiverailroading.blocks.BlockRail;
import cam72cam.immersiverailroading.blocks.BlockRailGag;
import cam72cam.immersiverailroading.items.ItemRail;
import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.proxy.CommonProxy;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;

@Mod(modid = ImmersiveRailroading.MODID, name="ImmersiveRailroading", version = ImmersiveRailroading.VERSION, acceptedMinecraftVersions = "[1.12,1.13)")
public class ImmersiveRailroading
{
    public static final String MODID = "immersiverailroading";
    public static final String VERSION = "0.1";
	public static final int ENTITY_SYNC_DISTANCE = 512;
    
	@ObjectHolder(BlockRailGag.NAME)
	public static final BlockRailGag BLOCK_RAIL_GAG = new BlockRailGag();
	@ObjectHolder(BlockRail.NAME)
	public static BlockRail BLOCK_RAIL = new BlockRail();
	
	@ObjectHolder(ItemRollingStock.NAME)
	public static ItemRollingStock ITEM_ROLLING_STOCK = new ItemRollingStock();
	
	public static Item ITEM_RAIL_BLOCK = new ItemRail(ImmersiveRailroading.BLOCK_RAIL).setRegistryName(ImmersiveRailroading.BLOCK_RAIL.getRegistryName());
	
	public static Logger logger;
	public static ImmersiveRailroading instance;
	
	public static final SimpleNetworkWrapper net = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
	
	@SidedProxy(clientSide="cam72cam.immersiverailroading.proxy.ClientProxy", serverSide="cam72cam.immersiverailroading.proxy.ServerProxy")
	public static CommonProxy proxy;
	
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        instance = this;
        
    	proxy.preInit(event);
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    	proxy.init(event);
    }
}
