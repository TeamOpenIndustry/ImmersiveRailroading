package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.items.nbt.ItemAugmentType;
import cam72cam.immersiverailroading.library.Augment;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.Color;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.world.World;

public class RailAugmentItemModel {
	public static StandardModel getModel(ItemStack stack, World world) {
		Augment augment = ItemAugmentType.get(stack);
		if (augment == null) {
			return null;
		}
		Color color = augment.color();
		return new StandardModel().addColorBlock(color, new Vec3d(0, 0.4, 0), new Vec3d(1, 0.2f, 1));
	}
}
