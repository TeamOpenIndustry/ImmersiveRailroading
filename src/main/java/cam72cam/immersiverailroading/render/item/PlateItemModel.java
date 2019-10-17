package cam72cam.immersiverailroading.render.item;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.Color;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.world.World;

public class PlateItemModel implements ItemRender.IItemModel {
	@Override
	public StandardModel getModel(World world, ItemStack stack) {
		StandardModel model = new StandardModel();
		model.addColorBlock(Color.GRAY, new Vec3d(0, 0, 0.5), new Vec3d(1, 1, 0.03));
		return model;
	}
}
