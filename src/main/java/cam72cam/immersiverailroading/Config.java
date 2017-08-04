package cam72cam.immersiverailroading;

import net.minecraftforge.common.config.Config.Comment;

@net.minecraftforge.common.config.Config(modid=ImmersiveRailroading.MODID)
public class Config {
	@Comment({"Range between couplers to try coupling"})
	public static double couplerRange = 0.3;

	@Comment({"A list of locomotives to load"})
	public static String[] locomotives = new String[] {"gp-7"};
	
	@Comment({"A list of passenger cars to load"})
	public static String[] passenger_cars = new String[] { };
	
	@Comment({"A list of freight cars to load"})
	public static String[] freight_cars = new String[] { "boxcar_us", "flatcar_us" };
	
	@Comment({"A list of tank cars to load"})
	public static String[] tank_cars = new String[] { "tank_us" };

	@Comment({"km/h to damage 1 heart on collision"})
	public static double entitySpeedDamage = 10;
	
	@Comment({"Self explanitory"})
	public static boolean trainsOnTheBrain = true;
}
