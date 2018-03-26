package cam72cam.immersiverailroading.render;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ConfigGraphics;

public class RenderCacheTimeLimiter {
	
	private int nsRemaining;

	public RenderCacheTimeLimiter() {
		this.reset();
	}
	
	public boolean canRender() {
		return nsRemaining > 0;
	}
	
	public int newList(Runnable draw) {
		int displayList = GL11.glGenLists(1);
		GL11.glNewList(displayList, GL11.GL_COMPILE);
		long start = System.nanoTime();
		draw.run();
		long stop = System.nanoTime();
		GL11.glEndList();
		
		nsRemaining -= stop - start;
		
		return displayList;
	}

	public void reset() {
		// allow 3ms per frame
		this.nsRemaining = ConfigGraphics.limitGraphicsLoadMS * 1000000; 
	}

}
