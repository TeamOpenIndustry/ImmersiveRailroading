package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.ConfigPermissions;
import cam72cam.mod.entity.Player;

public class Permissions {
    public static Player.PermissionAction LOCOMOTIVE_CONTROL = Player.registerAction(
            "immersiverailroading.entity.locomotive_control",
            "Control of Locomotives",
            ConfigPermissions.Defaults.LOCOMOTIVE_CONTROL
    );
    public static Player.PermissionAction BRAKE_CONTROL = Player.registerAction(
            "immersiverailroading.entity.brake_control",
            "Control of Independent Brakes",
            ConfigPermissions.Defaults.BRAKE_CONTROL
    );
    public static Player.PermissionAction COUPLING_HOOK = Player.registerAction(
            "immersiverailroading.entity.coupling_hook",
            "Change stock coupler status using the coupling hook",
            ConfigPermissions.Defaults.COUPLING_HOOK
    );
    public static Player.PermissionAction FREIGHT_INVENTORY = Player.registerAction(
            "immersiverailroading.entity.freight_inventory",
            "Interact with Freight Inventory",
            ConfigPermissions.Defaults.FREIGHT_INVENTORY
    );
    public static Player.PermissionAction PAINT_BRUSH = Player.registerAction(
            "immersiverailroading.stock.paint",
            "Paint stock",
            ConfigPermissions.Defaults.PAINT_BRUSH
    );
    public static Player.PermissionAction STOCK_ASSEMBLY = Player.registerAction(
            "immersiverailroading.stock.assembly",
            "Assemble/Disassemble stock",
            ConfigPermissions.Defaults.STOCK_ASSEMBLY
    );

    public static Player.PermissionAction BOARD_LOCOMOTIVE = Player.registerAction(
            "immersiverailroading.entity.board_locomotive",
            "Boarding Locomotives",
            ConfigPermissions.Defaults.BOARD_LOCOMOTIVE
    );
    public static Player.PermissionAction BOARD_STOCK = Player.registerAction(
            "immersiverailroading.entity.board_stock",
            "Boarding Stock",
            ConfigPermissions.Defaults.BOARD_STOCK
    );
    public static Player.PermissionAction BOARD_WITH_LEAD = Player.registerAction(
            "immersiverailroading.entity.board_with_lead",
            "Boarding lead entities on Stock",
            ConfigPermissions.Defaults.BOARD_WITH_LEAD
    );
    public static Player.PermissionAction CONDUCTOR = Player.registerAction(
            "immersiverailroading.entity.conductor",
            "Villager Conductor",
            ConfigPermissions.Defaults.CONDUCTOR
    );

    public static Player.PermissionAction AUGMENT_TRACK = Player.registerAction(
            "immersiverailroading.track.augment",
            "Track augment control",
            ConfigPermissions.Defaults.AUGMENT_TRACK
    );
    public static Player.PermissionAction SWITCH_CONTROL = Player.registerAction(
            "immersiverailroading.track.switch_control",
            "Switch Key",
            ConfigPermissions.Defaults.SWITCH_CONTROL
    );
    public static Player.PermissionAction EXCHANGE_TRACK = Player.registerAction(
            "immersiverailroading.track.exchanger",
            "Exchange Track",
            ConfigPermissions.Defaults.EXCHANGE_TRACK
    );
    public static Player.PermissionAction BUILD_TRACK = Player.registerAction(
            "immersiverailroading.track.build",
            "Build Track",
            ConfigPermissions.Defaults.BUILD_TRACK
    );
    public static Player.PermissionAction BREAK_TRACK = Player.registerAction(
            "immersiverailroading.track.break",
            "Break Track",
            ConfigPermissions.Defaults.BREAK_TRACK
    );

    public static Player.PermissionAction MACHINIST = Player.registerAction(
            "immersiverailroading.machine.interact",
            "Machine interactions",
            ConfigPermissions.Defaults.MACHINIST
    );

    public static void register() {
        // loads static classes and ctrs
    }
}
