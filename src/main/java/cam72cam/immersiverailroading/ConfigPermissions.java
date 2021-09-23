package cam72cam.immersiverailroading;

import cam72cam.mod.config.ConfigFile.*;

@Comment("Configuration File")
@Name("Permissions")
@File("immersiverailroading_permissions.cfg")
public class ConfigPermissions {
    @Comment("If op should be required by default for these permissions, set the entry to true")
    public static class Defaults {
        public static boolean LOCOMOTIVE_CONTROL = false;
        public static boolean BRAKE_CONTROL = false;
        public static boolean FREIGHT_INVENTORY = false;
        public static boolean COUPLING_HOOK = false;
        public static boolean PAINT_BRUSH = false;
        public static boolean STOCK_ASSEMBLY = false;

        public static boolean BOARD_LOCOMOTIVE = false;
        public static boolean BOARD_STOCK = false;
        public static boolean BOARD_WITH_LEAD = false;
        public static boolean CONDUCTOR = false;

        public static boolean AUGMENT_TRACK = false;
        public static boolean SWITCH_CONTROL = false;
        public static boolean EXCHANGE_TRACK = false;
        public static boolean BUILD_TRACK = false;
        public static boolean BREAK_TRACK = false;

        public static boolean MACHINIST = false;
    }
}
