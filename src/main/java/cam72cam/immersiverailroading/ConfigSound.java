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

	@Comment("Scales sound emitted by rolling stock to the gauge they are on.  Requres restart.")
	public static boolean scaleSoundToGauge = true;

	//@RequiresMcRestart
	@Comment("Re-configure the sound system to use more audo channels (fixes audio cutting out at high speed).  Disabled with a value less than 32")
	public static int customAudioChannels = 128;

}
