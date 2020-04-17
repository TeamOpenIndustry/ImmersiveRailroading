package cam72cam.immersiverailroading.render.multiblock;

import cam72cam.immersiverailroading.items.ItemManual;
import cam72cam.immersiverailroading.multiblock.Multiblock;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.GLTransparencyHelper;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.render.StandardModel;
import org.lwjgl.opengl.GL11;

import java.util.Map;

public class MBBlueprintRender {
	public static void draw(Multiblock mb) {
		float scale = 0.8f;
		Map<Vec3i, ItemStack> bp = mb.blueprint();
        StandardModel model = new StandardModel();
        for (Vec3i pos : bp.keySet()) {

            ItemStack state = bp.get(pos);
            if (state == null || state.isEmpty()) {
                continue;
            }

            Vec3i rPos = pos.subtract(mb.placementPos());
            model.addItem(state, new Vec3d(rPos.x, rPos.y, rPos.z), new Vec3d(scale,scale,scale));
        }
        model.render();
	}

    public static void renderMouseover(Player player, ItemStack stack, Vec3i pos, Vec3d hit, float partialTicks) {
        pos = pos.up();

        GL11.glPushMatrix();
        {
            GLTransparencyHelper transparency = new GLTransparencyHelper(1,1,1, 0.3f);

            Vec3d cameraPos = GlobalRender.getCameraPos(partialTicks);
            Vec3d playerPos = player.getPosition();
            Vec3d lastPos = player.getLastTickPos();
            Vec3d offset = new Vec3d(pos).add(0.5, 0.5, 0.5).subtract(cameraPos);
            GL11.glTranslated(offset.x, offset.y, offset.z);

            GL11.glRotated(-(int)(((player.getRotationYawHead()%360+360)%360+45) / 90) * 90, 0, 1, 0);

            MBBlueprintRender.draw(new ItemManual.Data(stack).multiblock);

            transparency.restore();
        }
        GL11.glPopMatrix();
    }
}
