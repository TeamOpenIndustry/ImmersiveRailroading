package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.gui.components.ListSelector;
import cam72cam.immersiverailroading.items.ItemMultiblockBlueprint;
import cam72cam.immersiverailroading.multiblock.*;
import cam72cam.immersiverailroading.net.ItemMultiblockUpdatePacket;
import cam72cam.immersiverailroading.render.multiblock.CustomMultiblockRender;
import cam72cam.immersiverailroading.render.multiblock.TileMultiblockRender;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.VBO;

public class MultiBlockSelectionGui implements IScreen {
    private Multiblock multiblock;

    private ListSelector<Multiblock> mbSelector;

    public MultiBlockSelectionGui() {
        multiblock = new ItemMultiblockBlueprint.Data(MinecraftClient.getPlayer().getHeldItem(Player.Hand.PRIMARY)).multiblock;
    }

    @Override
    public void init(IScreenBuilder iScreenBuilder) {
        mbSelector = new ListSelector<Multiblock>(iScreenBuilder, 20, 120, 20, multiblock, MultiblockRegistry.entries()) {
            @Override
            public void onClick(Multiblock m) {
                multiblock = m;
                new ItemMultiblockUpdatePacket(m.getName()).sendToServer();
            }
        };
    }

    @Override
    public void onEnterKey(IScreenBuilder iScreenBuilder) {

    }

    @Override
    public void onClose() {

    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        IScreen.super.draw(builder, state);
        GUIHelpers.drawRect(0,0, builder.getWidth(), builder.getHeight(), 0x99000000);

        mbSelector.setVisible(true);

        OBJModel model;
        if(multiblock instanceof CustomTransporterMultiblock){
            model = CustomMultiblockRender.refer.get(multiblock.getName()).model;
        } else {
            model = TileMultiblockRender.getRendererByName(multiblock.getName()).getModel();
        }

        Vec3d vec3d = model.centerOfGroups(model.groups());
        state.translate((GUIHelpers.getScreenWidth()) / 2d, builder.getHeight() / 2d + 50, 400);
        int scale = GUIHelpers.getScreenWidth() / 40;
        double degrees = (System.currentTimeMillis() / 1000d) % 72 * 5;
        state.rotate(degrees, 0, 1, 0);
        state.scale(-scale, -scale, -scale);
        //TODO optimization
        state.translate(- vec3d.z * Math.tan(Math.toRadians(degrees)), 0, 0);
        state.lightmap(1, 1);
        try(VBO.Binding binding = model.binder().bind(state)){
            binding.draw();
        }
    }
}
