package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.gui.CastingGUI;
import cam72cam.immersiverailroading.gui.PlateRollerGUI;
import cam72cam.immersiverailroading.gui.container.*;
import cam72cam.immersiverailroading.multiblock.CastingMultiblock;
import cam72cam.immersiverailroading.multiblock.PlateRollerMultiblock;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.gui.IScreen;
import cam72cam.mod.gui.Registry.GUIType;

public class GuiTypes {
    public static final GUIType FREIGHT = ImmersiveRailroading.proxy.GUI_REGISTRY.registerEntityContainer(Freight.class, FreightContainer::new);
    public static final GUIType TANK = ImmersiveRailroading.proxy.GUI_REGISTRY.registerEntityContainer(FreightTank.class, TankContainer::new);
    public static final GUIType TENDER = ImmersiveRailroading.proxy.GUI_REGISTRY.registerEntityContainer(Tender.class, TenderContainer::new);
    public static final GUIType STEAM_LOCOMOTIVE = ImmersiveRailroading.proxy.GUI_REGISTRY.registerEntityContainer(LocomotiveSteam.class, SteamLocomotiveContainer::new);
    public static final GUIType DIESEL_LOCOMOTIVE = ImmersiveRailroading.proxy.GUI_REGISTRY.registerEntityContainer(LocomotiveDiesel.class, TankContainer::new);

    public static final GUIType RAIL = null;
    public static final GUIType RAIL_PREVIEW = null;

    public static final GUIType STEAM_HAMMER = ImmersiveRailroading.proxy.GUI_REGISTRY.registerBlockContainer(TileMultiblock.class, SteamHammerContainer::new);
    public static final GUIType CASTING = ImmersiveRailroading.proxy.GUI_REGISTRY.registerBlock(TileMultiblock.class, GuiTypes::createMultiblockScreen);
    public static final GUIType PLATE_ROLLER = CASTING;
    private static IScreen createMultiblockScreen(TileMultiblock mb) {
        if (!mb.isLoaded()) {
            return null;
        }
        if (mb.getName().equals(CastingMultiblock.NAME)) {
            return new CastingGUI(mb);
        }
        if (mb.getName().equals(PlateRollerMultiblock.NAME)) {
            return new PlateRollerGUI(mb);
        }
        return null;
    }
}
