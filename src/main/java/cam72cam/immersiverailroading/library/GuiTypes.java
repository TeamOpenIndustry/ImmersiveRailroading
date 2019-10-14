package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.gui.CastingGUI;
import cam72cam.immersiverailroading.gui.PlateRollerGUI;
import cam72cam.immersiverailroading.gui.TrackGui;
import cam72cam.immersiverailroading.gui.container.*;
import cam72cam.immersiverailroading.multiblock.CastingMultiblock;
import cam72cam.immersiverailroading.multiblock.PlateRollerMultiblock;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.mod.gui.IScreen;
import cam72cam.mod.gui.GuiRegistry.GUIType;

public class GuiTypes {
    public static final GUIType FREIGHT = ImmersiveRailroading.GUI_REGISTRY.registerEntityContainer(Freight.class, FreightContainer::new);
    public static final GUIType TANK = ImmersiveRailroading.GUI_REGISTRY.registerEntityContainer(FreightTank.class, TankContainer::new);
    public static final GUIType TENDER = ImmersiveRailroading.GUI_REGISTRY.registerEntityContainer(Tender.class, TenderContainer::new);
    public static final GUIType STEAM_LOCOMOTIVE = ImmersiveRailroading.GUI_REGISTRY.registerEntityContainer(LocomotiveSteam.class, SteamLocomotiveContainer::new);
    public static final GUIType DIESEL_LOCOMOTIVE = ImmersiveRailroading.GUI_REGISTRY.registerEntityContainer(LocomotiveDiesel.class, TankContainer::new);

    public static final GUIType RAIL = ImmersiveRailroading.GUI_REGISTRY.register("RAIL", TrackGui::new);
    public static final GUIType RAIL_PREVIEW = ImmersiveRailroading.GUI_REGISTRY.registerBlock(TileRailPreview.class, TrackGui::new);
    public static final GUIType TRACK_EXCHANGER = ImmersiveRailroading.GUI_REGISTRY.register("TRACK_EXCHANGER", TrackExchangerGui::new);

    public static final GUIType STEAM_HAMMER = ImmersiveRailroading.GUI_REGISTRY.registerBlockContainer(TileMultiblock.class, SteamHammerContainer::new);
    public static final GUIType CASTING = ImmersiveRailroading.GUI_REGISTRY.registerBlock(TileMultiblock.class, GuiTypes::createMultiblockScreen);
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
