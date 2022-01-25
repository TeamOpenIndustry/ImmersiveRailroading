package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.HandCar;
import cam72cam.immersiverailroading.gui.overlay.GuiBuilder;
import cam72cam.mod.resource.Identifier;
import com.google.gson.JsonObject;

import java.io.IOException;

public class HandCarDefinition extends LocomotiveDefinition {
    public HandCarDefinition(String defID, JsonObject data) throws Exception {
        super(HandCar.class, defID, data);
    }

    @Override
    protected GuiBuilder getDefaultOverlay(JsonObject data) throws IOException {
        return GuiBuilder.parse(new Identifier(ImmersiveRailroading.MODID, "gui/default/handcar.json"));
    }

    @Override
    protected boolean multiUnitDefault() {
        return false;
    }
}
