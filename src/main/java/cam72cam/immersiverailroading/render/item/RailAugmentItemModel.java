package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.items.ItemRailAugment;
import cam72cam.umc.api.item.ItemStack;
import cam72cam.umc.api.render.Color;
import cam72cam.umc.api.render.ItemRender;
import cam72cam.umc.api.render.StandardModel;
import cam72cam.umc.api.world.World;
import cam72cam.umc.api.util.Matrix4;

public class RailAugmentItemModel implements ItemRender.IItemModel {
	@Override
	public StandardModel getModel(World world, ItemStack stack) {
		Color color = new ItemRailAugment.Data(stack).augment.color();
		return new StandardModel().addColorBlock(color, new Matrix4().translate(0, 0.4, 0).scale(1, 0.2f, 1));
	}
}
