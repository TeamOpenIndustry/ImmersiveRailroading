package com.goldenfield192.ire.renderer;

import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import com.goldenfield192.ire.IRE;
import com.goldenfield192.ire.tiles.TileConnector;

import static com.goldenfield192.ire.renderer.RenderUtils.renderStraightLine;
import static com.goldenfield192.ire.util.MathUtil.*;

//TODO 在做完资源包加载或者线缆渲染之前不要来重构
public class ConnectorRenderer{

    static OBJModel model,wire;
    static {
        try {
            model = new OBJModel(new Identifier(IRE.MODID,"models/block/jiaxiangan1.obj"),0);
            wire = new OBJModel(new Identifier(IRE.MODID,"models/block/wirebase.obj"),0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static StandardModel render(TileConnector gbe) {
        return new StandardModel().addCustom((state, partialTicks) -> BlockRenderer(gbe,state));
    }

    //史
    private static void BlockRenderer(TileConnector cbe, RenderState state){
        state.smooth_shading(true);
        state.translate(0.5,0,0.5);
        RenderState baseState = state.clone();
        baseState.rotate(cbe.getRotation(),0,1,0);
        //渲染base
        try (OBJRender.Binding vbo = model.binder().bind(baseState)) {
            vbo.draw();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //渲染网
        Vec3d offset = cbe.inBlockOffset.rotateYaw(cbe.getRotation());

        cbe.getConnection().keySet().forEach(vec3i -> {
            TileConnector cbe2 = cbe.getWorld()
                    .getBlockEntity(cbe.getPos().add(vec3i), TileConnector.class);//目标节点
            if(cbe.getConnection().get(vec3i).isFirst() && cbe2 != null){
                Vec3d offset2 = cbe2.inBlockOffset.rotateYaw(cbe2.getRotation());
                RenderState storage = state.clone();
                storage.translate(offset);
                Vec3d rotation = toVec3d(vec3i).add(offset2)
                        .subtract(offset);
                storage.scale(rotation.length()/2, 1, rotation.length()/2);
//                storage.rotate();
                storage.rotate(vecToDegreeXZ(rotation),0,1,0);
                storage.cull_face(false);
                try (OBJRender.Binding vbo = wire.binder().bind(storage)) {
                    vbo.draw();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

//        cbe.getConnection().keySet().forEach(vec3i -> {
//            TileConnector cbe2 = cbe.getWorld()
//                    .getBlockEntity(cbe.getPos().add(vec3i), TileConnector.class);//目标节点
//            if(cbe.getConnection().get(vec3i).isFirst() && cbe2 != null){
//                Vec3d offset2 = cbe2.inBlockOffset.rotateYaw(cbe2.getRotation());
//                RenderState storage = state.clone();
//                storage.translate(offset);
//                Vec3d rotation = cbe.getConnection().get(vec3i).getRelativePos();
//                storage.rotate(vecToDegreeXZ(rotation),0,1,0);
//                storage.scale(vecToLengthXZ(rotation),1,vecToLengthXZ(rotation));
//                storage.cull_face(false);
//                try (OBJRender.Binding vbo = wire.binder().bind(storage)) {
//                    vbo.draw();
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//                double rotXZ = vecToDegreeXZ(new Vec3d(rotation.x,0,rotation.z)),
//                       rotY = vecToDegreeY(rotation),
//                       length = rotation.length();
//                renderStraightLine(storage,rotXZ,rotY,length);
//            }
//        });
    }
}
