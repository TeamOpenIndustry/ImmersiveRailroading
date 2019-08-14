package cam72cam.immersiverailroading.render.multiblock;

import cam72cam.immersiverailroading.multiblock.Multiblock;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.StandardModel;

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
}
