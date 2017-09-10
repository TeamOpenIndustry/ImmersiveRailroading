package cam72cam.immersiverailroading;

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
	
	@Comment({"Self explanitory"})
	public static boolean trainsOnTheBrain = true;
	
	@Comment({"How heavy is a single block in Kg"})
	public static int blockWeight = 100;
	
	@Comment({"MilliBuckets per Liter"})
	public static int MB_PER_LITER = 1;

	@Comment({"DEBUG: Buckets infinite fill/empty tanks"})
	public static boolean debugInfiniteLiquids = true;
	
	@Comment({"Print extra chunk loading info"})
	public static boolean debugChunkLoading = true;

	@Comment({"Max length of a train"})
	public static int maxTrainLength = 100;
}
