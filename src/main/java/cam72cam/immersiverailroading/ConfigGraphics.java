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

	@Comment("Limit GPU load while models/internal is loading.  1 == slow load, 3 == reasonable load, 10 = fast load, 100 = as fast as possible")
	public static int limitGraphicsLoadMS = 3;

	@Comment({ "Scale the textures to reduce GPU memory usage (1 == highest, 1.5 == medium, 2 == low" })
	public static double scaleTexture = 1.5;

	@Comment({"Point at which textures start being scaled.  Any textures larger than this will have scaleTexture applied"})
	public static int scaleTextureCutoff=2048;

	@Comment({ "Self explanitory" })
	public static boolean trainsOnTheBrain = true;
	
	@Comment({ "What unit to use for speedometer. (kmh, mph or ms)" })
	public static SpeedDisplayType speedUnit = SpeedDisplayType.kmh;
}
