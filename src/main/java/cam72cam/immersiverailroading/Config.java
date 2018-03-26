package cam72cam.immersiverailroading;

import java.util.ArrayList;
import java.util.List;

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

		@Comment("How fast the locomotive should heat up.  1 is real world (slow), 72 is scaled to minecraft time")
		public static int locoHeatTimeScale = 72;

		@Comment("How much water the locomotive should use")
		public static float locoWaterUsage = 10;

		@Comment("How much you get payed per meter the villager traveled (default 1 emerald per km)")
		public static double villagerPayoutPerMeter = 0.001;

		@Comment("Distance the villagers will hear the conductor's whistle")
		public static double villagerConductorDistance = 50;
		
		@Comment("Villager payout items")
		public static String[] villagerPayoutItems = new String[] {
			Items.EMERALD.getRegistryName().toString(),
		};
		
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

		@Comment({"Time between open computers poll ticks for augments"})
		public static int ocPollDelayTicks = 1;

	}

	public static boolean isFuelRequired(Gauge gauge) {
		return !(!ConfigBalance.FuelRequired || (!ConfigBalance.ModelFuelRequired && gauge.equals(Gauge.MODEL)));
	}

}
