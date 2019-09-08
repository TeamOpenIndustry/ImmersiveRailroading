package cam72cam.immersiverailroading.render.multiblock;

import cam72cam.immersiverailroading.items.nbt.ItemMultiblockType;
import cam72cam.immersiverailroading.multiblock.Multiblock;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.StandardModel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;

import java.util.Map;

public class MBBlueprintRender {
	public static void draw(String name) {
		Multiblock mb = MultiblockRegistry.get(name);
		if (mb == null) {
			// Some wrappers (Akashic Tome) remove the metadata
			return;
		}
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
            GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, true);

            GL11.glBlendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE);
            if (GLContext.getCapabilities().OpenGL14) {
                GL14.glBlendColor(1, 1, 1, 0.3f);
            }

            Vec3d playerPos = player.getPosition();
            Vec3d lastPos = player.getLastTickPos();
            Vec3d offset = new Vec3d(pos).add(0.5, 0.5, 0.5).subtract(lastPos.add(playerPos.subtract(lastPos).scale(partialTicks)));
            GL11.glTranslated(offset.x, offset.y, offset.z);

            GL11.glRotated(-(int)(((player.getRotationYawHead()%360+360)%360+45) / 90) * 90, 0, 1, 0);

            MBBlueprintRender.draw(ItemMultiblockType.get(stack));

            blend.restore();
        }
        GL11.glPopMatrix();
    }
}
