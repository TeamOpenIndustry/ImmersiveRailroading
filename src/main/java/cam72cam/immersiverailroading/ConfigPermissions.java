package cam72cam.immersiverailroading;

import cam72cam.mod.config.ConfigFile.*;

@Comment("Configuration File")
@Name("Permissions")
@File("immersiverailroading_permissions.cfg")
public class ConfigPermissions {
    @Comment("If op should be required by default for these permissions, set the entry to true")
    public static class Defaults {
        public static boolean LOCOMOTIVE_CONTROL = false;
    }
}
