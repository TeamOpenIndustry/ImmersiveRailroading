package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.ConfigPermissions;
import cam72cam.mod.entity.Player;

public class Permissions {
    public static Player.PermissionAction LOCOMOTIVE_CONTROL = Player.registerAction(
            "immersiverailroading.entity.locomotive_control",
            "Control of Locomotives",
            ConfigPermissions.Defaults.LOCOMOTIVE_CONTROL
    );

    // BOARD_LOCOMOTIVE
    // BOARD_ANY_STOCK
    // BOARD_WITH_LEAD
    // CONDUCTOR
    // STOCK_INVENTORY
    // INDEPENDENT_BRAKE
    // COUPLING_ROD

    // AUGMENT_TRACK
    // SWITCH_KEY
    // PAINT_BRUSH
    // WRENCH/BREAK
    // EXCHANGE_TRACK
    // BUILDING_TRACK
    // BREAK_TRACK

    // MACHINE_INTERACTIONS
}
