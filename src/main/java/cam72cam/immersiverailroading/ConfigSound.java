package cam72cam.immersiverailroading;

import cam72cam.mod.config.ConfigFile.Comment;
import cam72cam.mod.config.ConfigFile.Name;
import cam72cam.mod.config.ConfigFile.File;
import cam72cam.mod.config.ConfigFile.Range;

@Comment("Configuration File")
@Name("general")
@File("immersiverailroading_sound.cfg")
public class ConfigSound {
	@Comment("Sound Distance Multiplier")
	@Range(min = 0.25, max = 10)
	public static double soundDistanceScale = 1;

	@Comment("Scales sound emitted by rolling stock to the gauge they are on.  Requres restart.")
	public static boolean scaleSoundToGauge = true;

	//@RequiresMcRestart
	@Comment("Re-configure the sound system to use more audo channels (fixes audio cutting out at high speed).  Disabled with a value less than 32")
	public static int customAudioChannels = 128;

	@Name("Categories")
	public static class SoundCategories {
		@Name("Immersive Railroading")
		@Range(min = 0, max = 100)
		public static int immersive_railroading = 100;

		@Name("Controls")
		@Range(min = 0, max = 100)
		public static int controls = 100;

		@Name("Animations")
		@Range(min = 0, max = 100)
		public static int animations = 100;
		@Name("Villager Whistle")
		@Range(min = 0, max = 100)
		public static int passenger_whistle = 100;

		public static float controls() { return immersive_railroading / 100f * controls / 100f; }
		public static float animations() { return immersive_railroading / 100f * animations / 100f; }
		public static float passenger_whistle() { return immersive_railroading / 100f * passenger_whistle / 100f; }

		@Name("Rolling Stock")
		public static class RollingStock {
			@Name("Category Rolling Stock")
			@Range(min = 0, max = 100)
			public static int category = 100;

			@Name("Wheel")
			@Range(min = 0, max = 100)
			public static int wheel = 100;
			@Name("Sliding")
			@Range(min = 0, max = 100)
			public static int sliding = 100;
			@Name("Flange")
			@Range(min = 0, max = 100)
			public static int flange = 100;
			@Name("Clack")
			@Range(min = 0, max = 100)
			public static int clack = 100;
			@Name("Couple")
			@Range(min = 0, max = 100)
			public static int couple = 100;
			@Name("Collision")
			@Range(min = 0, max = 100)
			public static int collision = 100;

			public static float wheel() { return immersive_railroading / 100f * category / 100f * wheel / 100f; }
			public static float sliding() { return immersive_railroading / 100f * category / 100f * sliding / 100f; }
			public static float flange() { return immersive_railroading / 100f * category / 100f * flange / 100f; }
			public static float clack() { return immersive_railroading / 100f * category / 100f * clack / 100f; }
			public static float couple() { return immersive_railroading / 100f * category / 100f * couple / 100f; }
			public static float collision() { return immersive_railroading / 100f * category / 100f * collision / 100f; }
		}
		@Name("Locomotive")
		public static class Locomotive {
			@Name("Category Locomotive")
			@Range(min = 0, max = 100)
			public static int category = 100;

			@Name("Bell")
			@Range(min = 0, max = 100)
			public static int bell = 100;

			public static float bell() { return immersive_railroading / 100f * category / 100f * bell / 100f; }

			@Name("Steam")
			public static class Steam {
				@Name("Category Steam Locomotive")
				@Range(min = 0, max = 100)
				public static int steam_category = 100;
				@Name("Idle")
				@Range(min = 0, max = 100)
				public static int idle = 100;
				@Name("Pressure Valve")
				@Range(min = 0, max = 100)
				public static int pressureValve = 100;
				@Name("Chuff")
				@Range(min = 0, max = 100)
				public static int chuff = 100;
				@Name("Cylinder Drain")
				@Range(min = 0, max = 100)
				public static int cylinder_drain = 100;
				@Name("Whistle")
				@Range(min = 0, max = 100)
				public static int whistle = 100;

				public static float idle() { return immersive_railroading / 100f * category / 100f * steam_category / 100f * idle / 100f; }
				public static float pressureValve() { return immersive_railroading / 100f * category / 100f * steam_category / 100f * pressureValve / 100f; }
				public static float chuff() { return immersive_railroading / 100f * category / 100f * steam_category / 100f * chuff / 100f; }
				public static float cylinder_drain() { return immersive_railroading / 100f * category / 100f * steam_category / 100f * cylinder_drain / 100f; }
				public static float whistle() { return immersive_railroading / 100f * category / 100f * steam_category / 100f * whistle / 100f; }
			}
			public static class Diesel {
				@Name("Category Diesel Locomotive")
				@Range(min = 0, max = 100)
				public static int diesel_category = 100;
				@Name("Idle")
				@Range(min = 0, max = 100)
				public static int idle = 100;
				@Name("Horn")
				@Range(min = 0, max = 100)
				public static int horn = 100;
				@Name("Running")
				@Range(min = 0, max = 100)
				public static int running = 100;

				public static float idle() { return immersive_railroading / 100f * category / 100f * diesel_category / 100f * idle / 100f; }
				public static float horn() { return immersive_railroading / 100f * category / 100f * diesel_category / 100f * horn / 100f; }
				public static float running() { return immersive_railroading / 100f * category / 100f * diesel_category / 100f * running / 100f; }
			}
		}
		@Name("Machines")
		public static class Machines {
			@Name("Category Machines")
			@Range(min = 0, max = 100)
			public static int machine_category = 100;
			public static float category() { return immersive_railroading / 100f * machine_category / 100f; }
		}
	}

}
