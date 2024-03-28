package com.goldenfield192.ire;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.LocomotiveElectric;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.render.item.TrackBlueprintItemModel;
import cam72cam.mod.ModCore;
import cam72cam.mod.ModEvent;
import cam72cam.mod.entity.EntityRegistry;
import cam72cam.mod.render.*;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import com.goldenfield192.ire.init.DefinitionManager;
import com.goldenfield192.ire.tiles.TIleBattery;
import com.goldenfield192.ire.tiles.TileConnector;
import com.goldenfield192.ire.init.BlocksInit;
import com.goldenfield192.ire.init.ItemsInit;
import com.goldenfield192.ire.init.TabsInit;
import com.goldenfield192.ire.renderer.ConnectorRenderer;
import com.goldenfield192.ire.renderer.SimpleBlockRenderer;
import com.goldenfield192.ire.util.graph.GraphHandler;

public class IRE extends ModCore.Mod{
    public static final String MODID = "ir_extension";

    static {
        try {
            ModCore.register(new IRE());
        } catch (Exception e) {
            throw new RuntimeException("Could not load mod " + MODID, e);
        }
    }

    public IRE(){

    }

    @Override
    public String modID() {
        return MODID;
    }


    @Override
    public void commonEvent(ModEvent event) {
        switch (event){
            case CONSTRUCT:
                EntityRegistry.register(this, LocomotiveElectric::new, 512);

                BlocksInit.register();
                TabsInit.register();
                ItemsInit.register();
                break;
            case INITIALIZE:
                DefinitionManager.initDefinitions();
            case SETUP:
                GraphHandler.init();
            default:
                break;
        }
    }

    @Override
    public void clientEvent(ModEvent event) {
        switch (event){
            case CONSTRUCT:
                BlockRender.register(BlocksInit.CONNECTOR_BLOCK, ConnectorRenderer::render, TileConnector.class);
                BlockRender.register(BlocksInit.BATTERY_BLOCK, SimpleBlockRenderer::render, TIleBattery.class);

                ItemRender.register(ItemsInit.CONNECTOR_ITEM,new Identifier(MODID,"items/guanmu"));
                ItemRender.register(ItemsInit.WIRE_ITEM,new Identifier(MODID,"items/guanmu"));
                ItemRender.register(ItemsInit.BATTERY,new Identifier(MODID,"items/guanmu"));
                ItemRender.register(ItemsInit.IRE_TRACK_BLUEPRINT, new TrackBlueprintItemModel());

                IEntityRender<EntityMoveableRollingStock> stockRender = new IEntityRender<EntityMoveableRollingStock>() {
                    public void render(EntityMoveableRollingStock entity, RenderState state, float partialTicks) {
                        StockModel<?, ?> renderer = entity.getDefinition().getModel();
                        if (renderer != null) {
                            renderer.renderEntity(entity, state, partialTicks);
                        }

                    }

                    public void postRender(EntityMoveableRollingStock entity, RenderState state, float partialTicks) {
                        StockModel<?, ?> renderer = entity.getDefinition().getModel();
                        if (renderer != null) {
                            renderer.postRenderEntity(entity, state, partialTicks);
                        }

                    }
                };
                EntityRenderer.register(LocomotiveElectric.class, stockRender);
                break;
            case INITIALIZE:
            case SETUP:
                GlobalRender.registerItemMouseover(ItemsInit.IRE_TRACK_BLUEPRINT, TrackBlueprintItemModel::renderMouseover);
            case RELOAD:
                DefinitionManager.initDefinitions();
            default:
                break;
        }
    }

    @Override
    public void serverEvent(ModEvent event) {
        //Do nothing here
    }
}
