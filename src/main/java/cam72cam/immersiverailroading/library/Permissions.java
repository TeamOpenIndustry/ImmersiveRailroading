package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.ConfigPermissions;
import cam72cam.mod.entity.Player;

public class Permissions {
    public static Player.PermissionAction LOCOMOTIVE_CONTROL = Player.registerAction(
            "immersiverailroading.entity.locomotive_control",
            "Control of Locomotives",
            ConfigPermissions.Defaults.LOCOMOTIVE_CONTROL
    );
}
