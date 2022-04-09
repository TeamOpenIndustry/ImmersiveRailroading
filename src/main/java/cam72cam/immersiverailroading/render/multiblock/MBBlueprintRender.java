package cam72cam.immersiverailroading.render.multiblock;

import cam72cam.immersiverailroading.items.ItemManual;
import cam72cam.immersiverailroading.multiblock.Multiblock;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.opengl.BlendMode;
import cam72cam.mod.render.opengl.RenderState;
import org.lwjgl.opengl.GL11;
import util.Matrix4;

import java.util.Map;

public class MBBlueprintRender {
    public static void renderMouseover(Player player, ItemStack stack, Vec3i pos, Vec3d hit, RenderState state, float partialTicks) {
        pos = pos.up();
        Multiblock mb = new ItemManual.Data(stack).multiblock;
        if (mb == null) {
            return;
        }

        state.blend(new BlendMode(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE).constantColor(1, 1, 1, 0.3f)).lightmap(1, 1);
        Vec3d cameraPos = GlobalRender.getCameraPos(partialTicks);
        Vec3d playerPos = player.getPosition();
        Vec3d lastPos = player.getLastTickPos();
        Vec3d offset = new Vec3d(pos).add(0.5, 0.5, 0.5).subtract(cameraPos);
        state.translate(offset);

        state.rotate(-(int)(((player.getRotationYawHead()%360+360)%360+45) / 90) * 90, 0, 1, 0);

        float scale = 0.8f;
        Map<Vec3i, ItemStack> bp = mb.blueprint();
        StandardModel model = new StandardModel();
        for (Vec3i bpos : bp.keySet()) {

            ItemStack renderstack = bp.get(bpos);
            if (renderstack == null || renderstack.isEmpty()) {
                continue;
            }

            Vec3i rPos = bpos.subtract(mb.placementPos());
            model.addItem(renderstack, new Matrix4().translate(rPos.x, rPos.y, rPos.z).scale(scale,scale,scale));
        }
        model.render(state);
    }
}
