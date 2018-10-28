package cam72cam.immersiverailroading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cam72cam.immersiverailroading.library.Gauge;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Config.Comment;

@net.minecraftforge.common.config.Config(modid = ImmersiveRailroading.MODID)
public class Config {
	public static ConfigDamage damage;

	public static class ConfigDamage {
		@Comment({ "Enable Boiler Explosions" })
		public static boolean explosionsEnabled = true;

		@Comment({ "Enable environmental damage of Boiler Explosions"})
		public static boolean explosionEnvDamageEnabled = true;
		
		@Comment({ "km/h to damage 1 heart on collision" })
		public static double entitySpeedDamage = 10;

		@Comment("Trains should break blocks")
		public static boolean TrainsBreakBlocks = true;

		@Comment("Break blocks around the border of the tracks in creative")
		public static boolean enableSideBlockClearing = true;

		@Comment("Clear blocsk in creative mode when placing tracks")
		public static boolean creativePlacementClearsBlocks = true;

		@Comment("Requires solid blocks to be placed under the rails")
		public static boolean requireSolidBlocks = true;

		@Comment("Drop snowballs when the train can't push a block out of the way")
		public static boolean dropSnowBalls = false;
		
		@Comment("Trains get destroyed by Mob explosions")
		public static boolean trainMobExplosionDamage = true;
	}

	public static ConfigBalance balance;

	public static class ConfigBalance {
		
		@Comment("Models require fuel")
		public static boolean ModelFuelRequired = true;

		@Comment("All gauges require fuel")
		public static boolean FuelRequired = true;

		@Comment("Slope Multiplier: Higher numbers increase slowdown, lower numbers decrease slowdown")
		public static double slopeMultiplier = 1.0;
		
		@Comment("Brake Multiplier: Higher numbers increase slowdown, lower numbers decrease slowdown")
		public static double brakeMultiplier = 1.0;
		
		@Comment("Traction Multiplier: Higher numbers decreases wheel slip, lower numders increase wheel slip")
		public static double tractionMultiplier = 1.0;
		
		@Comment({ "How heavy is a single block in Kg" })
		public static int blockWeight = 10;

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
		
		@Comment("Diesel Fuel Efficiency")
		public static int locoDieselFuelEfficiency = 100;
		
		@Comment("Steam Fuel Efficiency")
		public static int locoSteamFuelEfficiency = 100;

		@Comment("How fast the steam locomotive should heat up.  1 is real world (slow), 72 is scaled to minecraft time")
		public static int locoHeatTimeScale = 72;
		
		@Comment("How fast the diesel locomotive should heat up. 1 is real world (slow), 72 is scaled to minecraft time")
		public static int dieselLocoHeatTimeScale = 72;

		@Comment("How much water the locomotive should use")
		public static float locoWaterUsage = 10;

		@Comment("How much you get payed per meter the villager traveled (default 1 emerald per km)")
		public static double villagerPayoutPerMeter = 0.001;

		@Comment("Distance the villagers will hear the conductor's whistle")
		public static double villagerConductorDistance = 50;
		
		@Comment("Villager payout items")
		public static String[] villagerPayoutItems = new String[] {
			Items.EMERALD.getRegistryName().toString()
		};
		
		@Comment("Fuels for diesel Locomotives" + 
				"\nNote: Naphtha of Thermal Foundation is internally registered as 'refined oil'.")
		public static Map<String, Integer> dieselFuels = new HashMap<String, Integer>();
		static {
			dieselFuels.clear();
			// BC
			dieselFuels.put("oil", 100);
			dieselFuels.put("oil_heavy", 70);
			dieselFuels.put("oil_dense", 110);
			dieselFuels.put("oil_distilled", 50);
			dieselFuels.put("fuel_dense", 110);
			dieselFuels.put("fuel_mixed_heavy", 130);
			dieselFuels.put("fuel_light", 150);
			dieselFuels.put("fuel_mixed_light", 100);
			// IE/IP
			dieselFuels.put("diesel", 200);
			dieselFuels.put("biodiesel", 170);
			dieselFuels.put("biofuel", 170);
			dieselFuels.put("ethanol", 170);
			dieselFuels.put("gasoline", 100);
			//Thermal Foundation
			dieselFuels.put("refined_fuel", 150);
			dieselFuels.put("refined_oil", 100);
			//PneumaticCraft
			dieselFuels.put("lpg", 150);
			dieselFuels.put("kerosene", 180);
			dieselFuels.put("fuel", 180);
			
			// Other
			dieselFuels.put("olive_oil", 40);
		};
		
		@Comment("Water Substitutes")
		public static String[] waterTypes = new String[] {
			"water",
			"dist_water",
			"hot_spring_water",
			"purified_water"
		};

		@Comment("Allow diesel locomotive engine overheating")
		public static boolean canDieselEnginesOverheat = true;
		
		public static List<Item> getVillagerPayout() {
			List<Item> items = new ArrayList<Item>();
			for (String irl : villagerPayoutItems) {
				Item item = Item.REGISTRY.getObject(new ResourceLocation(irl));
				if(item != null) {
					items.add(item);
				}
			}
			return items;
		}
	}

	public static ConfigDebug debug;

	public static class ConfigDebug {
		
		@Comment({ "Speed up IR stock server tick stepping to compensate for tps lag" })
		public static boolean serverTickCompensation = true;

		@Comment({ "Range between couplers to try coupling" })
		public static double couplerRange = 0.3;

		@Comment({ "Deep Snow on tracks" })
		public static boolean deepSnow = false;

		@Comment({ "How fast deep snow should melt, 0 = disabled, 20 = fast, 400 = slow" })
		public static int snowMeltRate = 0;

		@Comment("Keep rolling stock loaded even when it is not moving")
		public static boolean keepStockLoaded = false;

		@Comment({ "Print extra chunk loading info" })
		public static boolean debugLog = false;

		@Comment({ "DEBUG: Buckets infinite fill/empty tanks" })
		public static boolean debugInfiniteLiquids = false;

		@Comment({"Time between open computers poll ticks for augments"})
		public static int ocPollDelayTicks = 1;
		
		@Comment({"DEV ONLY: How much to artifically lag the server (per world)"})
		public static int lagServer = 0;

	}

	public static boolean isFuelRequired(Gauge gauge) {
		return !(!ConfigBalance.FuelRequired || (!ConfigBalance.ModelFuelRequired && gauge.isModel()));
	}

}
