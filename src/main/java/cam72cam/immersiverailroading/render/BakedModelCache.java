package cam72cam.immersiverailroading.render;

import net.minecraft.client.renderer.block.model.IBakedModel;

public class BakedModelCache extends ExpireableList<String, IBakedModel> {
	@Override
	public int lifespan() {
		return 10*60;
	}
}
