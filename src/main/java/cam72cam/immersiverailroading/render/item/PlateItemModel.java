package cam72cam.immersiverailroading.render.item;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.Color;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.world.World;
import util.Matrix4;

public class PlateItemModel implements ItemRender.IItemModel {
	@Override
	public StandardModel getModel(World world, ItemStack stack) {
		StandardModel model = new StandardModel();
		model.addColorBlock(Color.GRAY, new Matrix4().translate(0, 0, 0.5).scale(1, 1, 0.03));
		return model;
	}
}
