package cam72cam.immersiverailroading;

import cam72cam.immersiverailroading.library.SpeedDisplayType;
import cam72cam.mod.config.ConfigFile.Comment;
import cam72cam.mod.config.ConfigFile.Name;

@Comment("Configuration File")
@Name("general")
public class ConfigGraphics {
	@Comment( "Place to draw the Train GUI as a % from the left of the screen" )
	public static int GUIPositionHorizontal = 2;

	@Comment( "Place to draw the Train GUI as a % from the top of the screen" )
	public static int GUIPositionVertical = 95;

	@Comment("Enable Particles")
	public static boolean particlesEnabled = true;

	@Comment( "Self explanatory" )
	public static boolean trainsOnTheBrain = true;
	
	@Comment( "What unit to use for speedometer. (kmh, mph or ms)" )
	public static SpeedDisplayType speedUnit = SpeedDisplayType.kmh;
}
