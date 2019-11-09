package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.gui.CastingGUI;
import cam72cam.immersiverailroading.gui.PlateRollerGUI;
import cam72cam.immersiverailroading.gui.TrackExchangerGui;
import cam72cam.immersiverailroading.gui.TrackGui;
import cam72cam.immersiverailroading.gui.container.*;
import cam72cam.immersiverailroading.multiblock.CastingMultiblock;
import cam72cam.immersiverailroading.multiblock.PlateRollerMultiblock;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.mod.config.ConfigGui;
import cam72cam.mod.gui.GuiRegistry;
import cam72cam.mod.gui.IScreen;
import cam72cam.mod.gui.GuiRegistry.GUIType;
import cam72cam.mod.resource.Identifier;

public class GuiTypes {
    public static final GUIType FREIGHT = GuiRegistry.INSTANCE.registerEntityContainer(Freight.class, FreightContainer::new);
    public static final GUIType TANK = GuiRegistry.INSTANCE.registerEntityContainer(FreightTank.class, TankContainer::new);
    public static final GUIType TENDER = GuiRegistry.INSTANCE.registerEntityContainer(Tender.class, TenderContainer::new);
    public static final GUIType STEAM_LOCOMOTIVE = GuiRegistry.INSTANCE.registerEntityContainer(LocomotiveSteam.class, SteamLocomotiveContainer::new);
    public static final GUIType DIESEL_LOCOMOTIVE = GuiRegistry.INSTANCE.registerEntityContainer(LocomotiveDiesel.class, TankContainer::new);

    public static final GUIType RAIL = GuiRegistry.INSTANCE.register(new Identifier(ImmersiveRailroading.MODID, "RAIL"), TrackGui::new);
    public static final GUIType RAIL_PREVIEW = GuiRegistry.INSTANCE.registerBlock(TileRailPreview.class, TrackGui::new);
    public static final GUIType TRACK_EXCHANGER = GuiRegistry.INSTANCE.register(new Identifier(ImmersiveRailroading.MODID, "TRACK_EXCHANGER"), TrackExchangerGui::new);

    public static final GUIType STEAM_HAMMER = GuiRegistry.INSTANCE.registerBlockContainer(TileMultiblock.class, SteamHammerContainer::new);
    public static final GUIType CASTING = GuiRegistry.INSTANCE.registerBlock(TileMultiblock.class, GuiTypes::createMultiblockScreen);
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

    public static final GUIType CONFIG = GuiRegistry.INSTANCE.register(new Identifier(ImmersiveRailroading.MODID, "config"), () -> new ConfigGui(Config.class, ConfigGraphics.class, ConfigSound.class));

    public static void register() {
        // loads static classes and ctrs
    }

}
