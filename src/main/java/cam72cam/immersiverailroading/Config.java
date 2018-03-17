package cam72cam.immersiverailroading;

import cam72cam.immersiverailroading.library.Gauge;
import net.minecraftforge.common.config.Config.Comment;

@net.minecraftforge.common.config.Config(modid=ImmersiveRailroading.MODID)
public class Config {
	@Comment({"Place to draw the Train GUI as a % from the left of the screen"})
	public static int GUIPositionHorizontal = 2;
	
	@Comment({"Place to draw the Train GUI as a % from the top of the screen"})
	public static int GUIPositionVertical = 95;

	@Comment({"Range between couplers to try coupling"})
	public static double couplerRange = 0.3;

	@Comment({"km/h to damage 1 heart on collision"})
	public static double entitySpeedDamage = 10;
	
	@Comment({"Deep Snow on tracks"})
	public static boolean deepSnow = false;
	
	@Comment({"Enable Boiler Explosions"})
	public static boolean explosionsEnabled = true;
	
	@Comment({"Self explanitory"})
	public static boolean trainsOnTheBrain = true;
	
	@Comment({"How heavy is a single block in Kg"})
	public static int blockWeight = 100;
	
	@Comment({"MilliBuckets per Liter"})
	public static int MB_PER_LITER = 1;

	@Comment({"DEBUG: Buckets infinite fill/empty tanks"})
	public static boolean debugInfiniteLiquids = false;
	
	@Comment({"Print extra chunk loading info"})
	public static boolean debugLog = false;
	
	@Comment({"Override GPU Max texture settings !DEV USE ONLY! (-1 == disable)"})
	public static int overrideGPUTexSize = -1;

	@Comment("Cost to place down a tie")
	public static double TieCostMultiplier = 0.25;

	@Comment("Cost to place down a rail")
	public static double RailCostMultiplier = 0.25;

	@Comment("Cost to place down rail bed")
	public static double BedCostMultiplier = 0.25;
	
	@Comment("Models require fuel")
	public static boolean ModelFuelRequired = true;
	
	@Comment("All gauges require fuel")
	public static boolean FuelRequired = true;
	
	@Comment("Trains should break blocks")
	public static boolean TrainsBreakBlocks = true;

	@Comment("Enable Particles")
	public static boolean particlesEnabled = true;

	@Comment("Enable Sounds")
	public static boolean soundEnabled = true;
	
	@Comment("Enable Pressure Valve Sound")
	public static boolean soundPressureValve = true;

	@Comment("Use Icon Cache (experimental)")
	public static boolean enableIconCache = false;
	
	@Comment("Break blocks around the border of the tracks in creative")
	public static boolean enableSideBlockClearing = true;

	@Comment("Keep rolling stock loaded even when it is not moving")
	public static boolean keepStockLoaded = false;
	
	@Comment("Sound Distance Multiplier")
	public static double soundDistanceScale = 1;

	@Comment("Enable priming of item render cache.  Disable this if you keep crashing right before the loading screen")
	public static boolean enableItemRenderPriming = true;

	@Comment("Re-configure the sound system to use more audo channels (fixes audio cutting out at high speed)")
	public static boolean overrideSoundChannels = true;

	@Comment("Clear blocsk in creative mode when placing tracks")
	public static boolean creativePlacementClearsBlocks = true;

	@Comment("If more than X% of the tracks are above non solid blocks, break the track")
	public static double trackFloatingPercent = 0.05;

	public static boolean isFuelRequired(Gauge gauge) {
		return !(!FuelRequired || (!ModelFuelRequired && gauge.equals(Gauge.MODEL)));
	}
}
