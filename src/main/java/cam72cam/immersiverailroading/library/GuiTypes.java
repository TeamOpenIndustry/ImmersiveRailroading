package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.*;
import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.gui.*;
import cam72cam.immersiverailroading.gui.container.*;
import cam72cam.immersiverailroading.multiblock.*;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.mod.config.ConfigGui;
import cam72cam.mod.gui.GuiRegistry;
import cam72cam.mod.gui.GuiRegistry.BlockGUI;
import cam72cam.mod.gui.GuiRegistry.EntityGUI;
import cam72cam.mod.gui.GuiRegistry.GUI;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.resource.Identifier;

public class GuiTypes {
    public static final EntityGUI<Freight> FREIGHT = GuiRegistry.registerEntityContainer(Freight.class, FreightContainer::new);
    public static final EntityGUI<FreightTank> TANK = GuiRegistry.registerEntityContainer(FreightTank.class, TankContainer::new);
    public static final EntityGUI<Tender> TENDER = GuiRegistry.registerEntityContainer(Tender.class, TenderContainer::new);
    public static final EntityGUI<LocomotiveSteam> STEAM_LOCOMOTIVE = GuiRegistry.registerEntityContainer(LocomotiveSteam.class, SteamLocomotiveContainer::new);
    public static final EntityGUI<LocomotiveDiesel> DIESEL_LOCOMOTIVE = GuiRegistry.registerEntityContainer(LocomotiveDiesel.class, TankContainer::new);

    public static final GUI RAIL = GuiRegistry.register(new Identifier(ImmersiveRailroading.MODID, "RAIL"), TrackGui::new);
    public static final BlockGUI RAIL_PREVIEW = GuiRegistry.registerBlock(TileRailPreview.class, TrackGui::new);
    public static final GUI TRACK_EXCHANGER = GuiRegistry.register(new Identifier(ImmersiveRailroading.MODID, "TRACK_EXCHANGER"), TrackExchangerGui::new);
    public static final GUI PAINT_BRUSH = GuiRegistry.register(new Identifier(ImmersiveRailroading.MODID, "PAINT_BRUSH"), PaintBrushPicker::new);

    public static final BlockGUI STEAM_HAMMER = GuiRegistry.registerBlockContainer(TileMultiblock.class, SteamHammerContainer::new);
    public static final BlockGUI CASTING = GuiRegistry.registerBlock(TileMultiblock.class, GuiTypes::createMultiblockScreen);
    public static final BlockGUI PLATE_ROLLER = CASTING;
    public static final BlockGUI CUSTOM_TRANSPORT_MB_GUI = GuiRegistry.registerBlock(TileMultiblock.class, CustomTransportMultiblockScreen::new);
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

    public static final GUI CONFIG = GuiRegistry.register(new Identifier(ImmersiveRailroading.MODID, "config"), () -> new ConfigGui(Config.class, ConfigGraphics.class, ConfigSound.class, ConfigPermissions.class));

    public static void register() {
        // loads static classes and ctrs
    }

}
