package cam72cam.immersiverailroading.render.multiblock;

import cam72cam.immersiverailroading.multiblock.Multiblock;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import org.lwjgl.opengl.GL11;

import java.util.Map;

public class MBBlueprintRender {
	private static RenderItem render;

	public static void draw(String name) {
		if (render == null) {
			render = Minecraft.getMinecraft().getRenderItem();
		}
		
		Multiblock mb = MultiblockRegistry.get(name);
		if (mb == null) {
			// Some wrappers (Akashic Tome) remove the metadata
			return;
		}
		Map<Vec3i, ItemStack> bp = mb.blueprint();
        for (Vec3i pos : bp.keySet()) {

            ItemStack state = bp.get(pos);
            if (state == null || state.isEmpty()) {
                continue;
            }

            Vec3i rPos = pos.subtract(mb.placementPos());
            GL11.glTranslated(rPos.x, rPos.y, rPos.z);
            render.renderItem(state.internal, ItemCameraTransforms.TransformType.NONE);
            GL11.glTranslated(-rPos.x, -rPos.y, -rPos.z);
        }
	}
}
