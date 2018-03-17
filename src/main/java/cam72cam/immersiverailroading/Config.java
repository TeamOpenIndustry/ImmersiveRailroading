package cam72cam.immersiverailroading;

import cam72cam.immersiverailroading.library.Gauge;
import net.minecraftforge.common.config.Config.Comment;

@net.minecraftforge.common.config.Config(modid = ImmersiveRailroading.MODID)
public class Config {
	public static ConfigDamage damage;

	public static class ConfigDamage {
		@Comment({ "Enable Boiler Explosions" })
		public static boolean explosionsEnabled = true;

		@Comment({ "km/h to damage 1 heart on collision" })
		public static double entitySpeedDamage = 10;

		@Comment("Trains should break blocks")
		public static boolean TrainsBreakBlocks = true;

		@Comment("Break blocks around the border of the tracks in creative")
		public static boolean enableSideBlockClearing = true;

		@Comment("Clear blocsk in creative mode when placing tracks")
		public static boolean creativePlacementClearsBlocks = true;
	}

	public static ConfigBalance balance;

	public static class ConfigBalance {
		@Comment("Models require fuel")
		public static boolean ModelFuelRequired = true;

		@Comment("All gauges require fuel")
		public static boolean FuelRequired = true;

		@Comment({ "How heavy is a single block in Kg" })
		public static int blockWeight = 100;

		@Comment({ "MilliBuckets per Liter" })
		public static int MB_PER_LITER = 1;

		@Comment("Cost to place down a tie")
		public static double TieCostMultiplier = 0.25;

		@Comment("Cost to place down a rail")
		public static double RailCostMultiplier = 0.25;

		@Comment("Cost to place down rail bed")
		public static double BedCostMultiplier = 0.25;

		@Comment("If more than X% of the tracks are above non solid blocks, break the track")
		public static double trackFloatingPercent = 0.05;
	}

	public static ConfigDebug debug;

	public static class ConfigDebug {

		@Comment({ "Range between couplers to try coupling" })
		public static double couplerRange = 0.3;

		@Comment({ "Deep Snow on tracks" })
		public static boolean deepSnow = false;

		@Comment("Keep rolling stock loaded even when it is not moving")
		public static boolean keepStockLoaded = false;

		@Comment({ "Print extra chunk loading info" })
		public static boolean debugLog = false;

		@Comment({ "DEBUG: Buckets infinite fill/empty tanks" })
		public static boolean debugInfiniteLiquids = false;

	}

	public static boolean isFuelRequired(Gauge gauge) {
		return !(!ConfigBalance.FuelRequired || (!ConfigBalance.ModelFuelRequired && gauge.equals(Gauge.MODEL)));
	}

}
