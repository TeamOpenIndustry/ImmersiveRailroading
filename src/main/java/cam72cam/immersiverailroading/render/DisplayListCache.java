package cam72cam.immersiverailroading.render;

import org.lwjgl.opengl.GL11;

public class DisplayListCache extends ExpireableList<String, Integer> {
	@Override
	public void onRemove(String key, Integer val) {
		GL11.glDeleteLists(val, 1);
	}
}
