package cam72cam.immersiverailroading;

import cam72cam.mod.config.ConfigFile.Comment;
import cam72cam.mod.config.ConfigFile.Name;
import cam72cam.mod.config.ConfigFile.File;

@Comment("Configuration File")
@Name("general")
@File("immersiverailroading_sound.cfg")
public class ConfigSound {
	//@RequiresMcRestart
	@Comment("Enable Sounds")
	public static boolean soundEnabled = true;

	@Comment("Enable Pressure Valve Sound")
	public static boolean soundPressureValve = true;

	@Comment("Sound Distance Multiplier")
	public static double soundDistanceScale = 1;

	//@RequiresMcRestart
	@Comment("Re-configure the sound system to use more audo channels (fixes audio cutting out at high speed)")
	public static boolean overrideSoundChannels = true;

}
