package cam72cam.immersiverailroading;

import cam72cam.immersiverailroading.library.SpeedDisplayType;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.RequiresMcRestart;

@net.minecraftforge.common.config.Config(modid = ImmersiveRailroading.MODID, name = ImmersiveRailroading.MODID + "_graphics")
public class ConfigGraphics {
	@Comment({ "Place to draw the Train GUI as a % from the left of the screen" })
	public static int GUIPositionHorizontal = 2;

	@Comment({ "Place to draw the Train GUI as a % from the top of the screen" })
	public static int GUIPositionVertical = 95;

	@Comment("Enable Particles")
	public static boolean particlesEnabled = true;

	@Comment("Enable priming of item render cache.  Disable this if you keep crashing in the world loading screen")
	public static boolean enableItemRenderPriming = true;

	@Comment({ "Override GPU Max texture settings !DEV USE ONLY! (-1 == disable)" })
	public static int overrideGPUTexSize = -1;

	@RequiresMcRestart
	@Comment("Generate static icons.  Drastically reduces render overhead for IR items")
	public static boolean enableIconCache = false;

	@Comment("If you are having render problems in game, try setting this to false")
	public static boolean useShaderFriendlyRender = true;

	@Comment("Limit GPU load while models/world is loading.  1 == slow load, 3 == reasonable load, 10 = fast load, 100 = as fast as possible")
	public static int limitGraphicsLoadMS = 3;

	@Comment({ "Scale the textures to reduce GPU memory usage (1 == normal, 1.5 == low, 2 == super low" })
	public static double textureScale = 1;

	@Comment({ "Self explanitory" })
	public static boolean trainsOnTheBrain = true;
	
	@Comment({ "What unit to use for speedometer. (kmh, mph or ms)" })
	public static SpeedDisplayType speedUnit = SpeedDisplayType.kmh;

}
