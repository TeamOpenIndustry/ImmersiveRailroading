package cam72cam.immersiverailroading.util;

import org.lwjgl.opengl.GL11;

public class GLBoolTracker {
	private int opt;
	private boolean oldState;
	private boolean newState;

	public GLBoolTracker(int opt, boolean newState) {
		this.opt = opt;
		this.oldState = GL11.glGetBoolean(opt);
		this.newState = newState;
		if (newState != oldState) {
			apply(newState);
		}
	}
	
	public void restore() {
		if (newState != oldState) {
			apply(oldState);
		}
	}
	
	private void apply(boolean currState) {
		if (currState) {
			GL11.glEnable(opt);
		} else {
			GL11.glDisable(opt);
		}
		}
}
