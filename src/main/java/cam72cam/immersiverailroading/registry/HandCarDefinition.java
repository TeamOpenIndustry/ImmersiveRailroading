package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.HandCar;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.immersiverailroading.gui.overlay.GuiBuilder;
import cam72cam.mod.resource.Identifier;

import java.io.IOException;

public class HandCarDefinition extends LocomotiveDefinition {
    public HandCarDefinition(String defID, DataBlock data) throws Exception {
        super(HandCar.class, defID, data);
    }

    @Override
    protected GuiBuilder getDefaultOverlay(DataBlock data) throws IOException {
        return GuiBuilder.parse(new Identifier(ImmersiveRailroading.MODID, "gui/default/handcar.caml"));
    }

    @Override
    protected boolean multiUnitDefault() {
        return false;
    }

    @Override
    protected boolean pressureBrakeDefault() {
        return false;
    }
}
