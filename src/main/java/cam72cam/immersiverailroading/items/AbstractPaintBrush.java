package cam72cam.immersiverailroading.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;

import java.util.List;

public abstract class AbstractPaintBrush extends Item {

	protected AbstractPaintBrush() {
		super();
	}

	public abstract String selectNewTexture(List<String> texNames, String currentTexture, EntityPlayer player);
}
