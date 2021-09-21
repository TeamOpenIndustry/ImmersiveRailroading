package cam72cam.immersiverailroading;

import cam72cam.immersiverailroading.library.SpeedDisplayType;
import cam72cam.mod.config.ConfigFile.Comment;
import cam72cam.mod.config.ConfigFile.Name;

import static cam72cam.mod.config.ConfigFile.*;

@Comment("Configuration File")
@Name("general")
@File("immersiverailroading_graphics.cfg")
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

	@Comment( "How long to keep textures in memory after they have left the screen (higher numbers = smoother game play, lower numbers = less GPU memory used)")
    public static int textureCacheSeconds = 30;

	@Comment( "Show text tooltips over interactable components" )
	public static boolean interactiveComponentsOverlay = true;

	@Comment("Show stock variants in JEI/NEI/Creative search")
	public static boolean stockItemVariants = false;
}
