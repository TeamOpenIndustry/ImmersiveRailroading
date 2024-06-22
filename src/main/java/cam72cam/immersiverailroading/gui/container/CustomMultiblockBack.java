package cam72cam.immersiverailroading.gui.container;

import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.render.opengl.RenderState;

public class CustomMultiblockBack implements IScreen {
    private TileMultiblock tile;

    public CustomMultiblockBack(TileMultiblock tile) {
        this.tile = tile;
    }

    @Override
    public void init(IScreenBuilder screen) {

    }

    @Override
    public void onEnterKey(IScreenBuilder builder) {

    }

    @Override
    public void onClose() {

    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        builder.drawImage(EntityRollingStockDefinition.LightDefinition.default_light_tex, 0, 0, 64, 64);
    }
}
