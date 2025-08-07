package cam72cam.immersiverailroading;

import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.mod.config.ConfigFile.Comment;
import cam72cam.mod.config.ConfigFile.File;
import cam72cam.mod.config.ConfigFile.Name;
import cam72cam.mod.config.ConfigFile.Range;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

@Comment("Configuration File")
@Name("general")
@File("immersiverailroading.cfg")
public class Config {
	public static void init() {
		if (ConfigBalance.dieselFuels.isEmpty()) {
			// BC
			ConfigBalance.dieselFuels.put("oil", 100);
			ConfigBalance.dieselFuels.put("oil_heavy", 70);
			ConfigBalance.dieselFuels.put("oil_dense", 110);
			ConfigBalance.dieselFuels.put("oil_distilled", 50);
			ConfigBalance.dieselFuels.put("fuel_dense", 110);
			ConfigBalance.dieselFuels.put("fuel_mixed_heavy", 130);
			ConfigBalance.dieselFuels.put("fuel_light", 150);
			ConfigBalance.dieselFuels.put("fuel_mixed_light", 100);
			// IE/IP
			ConfigBalance.dieselFuels.put("diesel", 200);
			ConfigBalance.dieselFuels.put("biodiesel", 170);
			ConfigBalance.dieselFuels.put("biofuel", 170);
			ConfigBalance.dieselFuels.put("ethanol", 170);
			ConfigBalance.dieselFuels.put("gasoline", 100);
			//Thermal Foundation
			ConfigBalance.dieselFuels.put("refined_fuel", 150);
			ConfigBalance.dieselFuels.put("refined_oil", 100);
			//PneumaticCraft
			ConfigBalance.dieselFuels.put("lpg", 150);
			ConfigBalance.dieselFuels.put("kerosene", 180);
			ConfigBalance.dieselFuels.put("fuel", 180);

			// Other
			ConfigBalance.dieselFuels.put("olive_oil", 40);
		}
	}

	@Name("damage")
	public static class ConfigDamage {
		@Comment( "Enable Boiler Explosions" )
		public static boolean explosionsEnabled = true;

		@Comment( "Enable environmental damage of Boiler Explosions")
		public static boolean explosionEnvDamageEnabled = true;
		
		@Comment( "km/h to damage 1 heart on collision" )
		@Range(min = 1, max = 100)
		public static double entitySpeedDamage = 10;

		@Comment("Trains should break block")
		public static boolean TrainsBreakBlocks = true;

		@Comment("How hard are blocks to break by rolling stock?")
		@Range(min = 0, max = 500)
		public static int blockHardness = 50;

		@Comment("Break block around the border of the tracks in creative")
		public static boolean enableSideBlockClearing = true;

		@Comment("Clear blocsk in creative mode when placing tracks")
		public static boolean creativePlacementClearsBlocks = true;

		@Comment("Requires solid block to be placed under the rails")
		public static boolean requireSolidBlocks = true;

		@Comment("Drop snowballs when the train can't push a block out of the way")
		public static boolean dropSnowBalls = false;
		
		@Comment("Trains getContents destroyed by Mob explosions")
		public static boolean trainMobExplosionDamage = true;
	}

	@Name("Immersion Level")
	public static class ImmersionConfig {
		@Comment("Old style throttle/reverser control which uses the throttle as the reverser")
		@Name("Disable Independent Throttle")
		public static boolean disableIndependentThrottle = true;

		@Comment("Old style brake control")
		@Name("Instant Brake Pressure")
		public static boolean instantBrakePressure = false;

		@Comment("Enable coupler slack")
		public static boolean slackEnabled = true;
    }

	@Name("balance")
	public static class ConfigBalance {
		
		@Comment("Models require fuel")
		public static boolean ModelFuelRequired = true;

		@Comment("All gauges require fuel")
		public static boolean FuelRequired = true;

		@Comment("Slope Multiplier: Higher numbers increase slowdown, lower numbers decrease slowdown")
		@Range(min = 0, max = 2)
		public static double slopeMultiplier = 1.0;
		
		@Comment("Brake Multiplier: Higher numbers increase slowdown, lower numbers decrease slowdown")
		@Range(min = 0, max = 10)
		public static double brakeMultiplier = 1.0;
		
		@Comment("Traction Multiplier: Higher numbers decreases wheel slip, lower numders increase wheel slip")
		@Range(min = 0, max = 10)
		public static double tractionMultiplier = 1.0;
		
		@Comment( "How heavy is a single block in Kg" )
		@Range(min = 0, max = 100)
		public static int blockWeight = 10;

		@Comment( "MilliBuckets per Liter" )
		public static int MB_PER_LITER = 1;

		@Comment("Cost to place down a tie")
		@Range(min = 0, max = 10)
		public static double TieCostMultiplier = 0.25;

		@Comment("Cost to place down a rail")
		@Range(min = 0, max = 10)
		public static double RailCostMultiplier = 0.25;

		@Comment("Cost to place down rail bed")
		@Range(min = 0, max = 10)
		public static double BedCostMultiplier = 0.25;

		@Comment("If more than X% of the tracks are above non solid block, break the track")
		@Range(min = 0, max = 1)
		public static double trackFloatingPercent = 0.05;
		
		@Comment("Diesel Fuel Efficiency")
		@Range(min = 1, max = 500)
		public static int locoDieselFuelEfficiency = 100;
		
		@Comment("Steam Fuel Efficiency")
		@Range(min = 1, max = 500)
		public static int locoSteamFuelEfficiency = 100;

		@Comment("How fast the steam locomotive should heat up.  1 is real internal (slow), 72 is scaled to minecraft time")
		@Range(min = 0, max = 500)
		public static int locoHeatTimeScale = 72;
		
		@Comment("How fast the diesel locomotive should heat up. 1 is real internal (slow), 72 is scaled to minecraft time")
		@Range(min = 0, max = 500)
		public static int dieselLocoHeatTimeScale = 72;

		@Comment("How much water the locomotive should use")
		@Range(min = 0, max = 100)
		public static float locoWaterUsage = 10;

		@Comment("How much you getContents payed per meter the villager traveled (default 1 emerald per km)")
		public static double villagerPayoutPerMeter = 0.001;

		@Comment("Distance the villagers will hear the conductor's whistle")
		@Range(min = 0, max = 200)
		public static double villagerConductorDistance = 50;
		
		@Comment("Villager payout items (ore dict)")
		public static Fuzzy[] villagerPayoutItems = new Fuzzy[] {
				Fuzzy.EMERALD
		};
		
		@Comment("Fuels for diesel Locomotives" + 
				"\nNote: Naphtha of Thermal Foundation is internally registered as 'refined oil'.")
		public static Map<String, Integer> dieselFuels = new HashMap<>();

		@Comment("Water Substitutes")
		public static String[] waterTypes = new String[] {
			"water",
			"dist_water",
			"hot_spring_water",
			"purified_water"
		};

		@Comment("Allow diesel locomotive engine overheating")
		public static boolean canDieselEnginesOverheat = true;

        public static List<ItemStack> getVillagerPayout() {
			return Arrays.stream(villagerPayoutItems).map(Fuzzy::example).collect(Collectors.toList());
		}
		
		@Comment("Only select Locomotives with suitable equipment can be radio-controlled")
		public static boolean RadioEquipmentRequired = true;
		
		@Comment("Range of radio-control, positive integer")
		@Range(min = 0, max = 1000)
		public static int RadioRange = 500;
		
		@Comment("IEnergy cost (RF) per radio transmission per metre")
		@Range(min = 0, max = 1000)
		public static int RadioCostPerMetre = 0;
		
		@Comment("Prevent stock from being built outside the recommended and model gauges")
		public static boolean DesignGaugeLock = false;

		@Comment("Angle Placement Segmentation")
		@Range(min = 1, max = 90)
		public static int AnglePlacementSegmentation = 4;

		@Comment("Machine power factor (0 means no power required)")
		@Range(min = 0, max = 10)
		public static float machinePowerFactor = 1.0f;

		@Comment("Angles per tick to rotate turntables (used server side)")
		@Range(min = 0, max = 5)
		public static double TurnTableSpeed = 0.4;

		@Comment("Meters per tick to move transfer tables (used server side)")
		@Range(min = 0, max = 1)
		public static double TransferTableSpeed = 0.03;

		@Comment("Diesel locomotive capacity multiplier, set to 10 for old functionality")
		@Range(min = 1, max = 10)
		public static int DieselLocomotiveTankMultiplier = 1;

		@Comment("Round to nearest bucket")
		public static boolean RoundStockTankToNearestBucket = true;
	}

	@Name("performance")
	public static class ConfigPerformance {
		@Comment("Use multiple threads when loading stock. This is used on Minecraft's initial load or when manually reloading stocks.")
		public static boolean multithreadedStockLoading = true;

		@Comment("How many MB of memory to reserve for stock loading per thread, higher numbers = safer but slower")
		public static int megabytesReservedPerStockLoadingThread = 1024;
	}

	@Name("debug")
	public static class ConfigDebug {

		@Comment( "Range between couplers to try coupling" )
		@Range(min = 0, max = 1)
		public static double couplerRange = 0.3;

		@Comment( "Deep Snow on tracks" )
		public static boolean deepSnow = false;

		@Comment( "How fast snow should accumulate, 0 = disabled, 20 = fast, 400 = slow" )
		@Range(min = 0, max = 1000)
		public static int snowAccumulateRate = 400;

		@Comment( "How fast snow should melt, 0 = disabled, 20 = fast, 400 = slow" )
		@Range(min = 0, max = 1000)
		public static int snowMeltRate = 0;

		@Comment("Keep rolling stock loaded even when it is not moving")
		public static boolean keepStockLoaded = true;

		@Comment( "Print extra chunk loading info" )
		public static boolean debugLog = false;

		@Comment( "Announce the new livery in Chat when the paint brush is used" )
		public static boolean debugPaintBrush = false;

		@Comment( "DEBUG: Buckets infinite fill/empty tanks" )
		public static boolean debugInfiniteLiquids = false;

		@Comment("Time between open computers poll ticks for augments")
		@Range(min = 0, max = 10)
		public static int ocPollDelayTicks = 1;
		
		@Comment("DEV ONLY: How much to artifically lag the server (per internal)")
		@Range(min = 0, max = 100)
		public static int lagServer = 0;

		@Comment("Old Narrow track placement (single width instead of 3)")
        public static boolean oldNarrowWidth = false;

		@Comment("Default Augments to Computer Mode")
		public static boolean defaultAugmentComputer = false;
		@Comment("Warn if a physics tick takes more than the given time in ms")
		@Range(min = 1, max = 100)
		public static int physicsWarnThresholdMs = 20;

		@Comment("Warn if a physics total iteration takes more than the given time in ms")
		@Range(min = 1, max = 100)
		public static int physicsWarnTotalThresholdMs = 40;

		@Comment("Number of physics steps to cache for future movement / send in packets.  DO NOT CHANGE UNLESS YOU KNOW WHAT YOU ARE DOING")
		@Range(min = 10, max = 60)
		public static int physicsFutureTicks = 10;

		@Comment("Does stock drops itself/components when player is in creative mode?")
		public static boolean stockDropInCreativeMode = true;
	}

	public static boolean isFuelRequired(Gauge gauge) {
		return !(!ConfigBalance.FuelRequired || (!ConfigBalance.ModelFuelRequired && gauge.isModel()));
	}

}
