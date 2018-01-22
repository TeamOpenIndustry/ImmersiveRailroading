package cam72cam.immersiverailroading.render;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.lwjgl.opengl.GL11;

public class DisplayListCache {
	private static final int lifespan = 10;  
	
	private static long timeS() {
		return System.currentTimeMillis() / 1000L;
	}
	
	private Map<String, Integer> displayLists = new HashMap<String, Integer>();
	private Map<String, Long> displayListsUsage = new HashMap<String, Long>();
	private long lastTime = timeS();
	
	public Integer get(String key) {
		synchronized(this) {
			if (lastTime + lifespan < timeS()) {
				// clear unused
				Set<String> ks = new HashSet<String>();
				ks.addAll(displayLists.keySet());
				for (String dk : ks) {
					if (dk != key && displayListsUsage.get(dk) + lifespan < timeS()) {
						GL11.glDeleteLists(displayLists.get(dk), 1);
						displayLists.remove(dk);
						displayListsUsage.remove(dk);
					}
				}
				lastTime = timeS();
			}
			
			
			if (displayLists.containsKey(key)) {
				displayListsUsage.put(key, timeS());
				return displayLists.get(key);
			}
			return null;
		}
	}

	public boolean containsKey(String key) {
		synchronized(this) {
			return displayLists.containsKey(key);
		}
	}

	public void put(String key, int displayList) {
		synchronized(this) {
			displayListsUsage.put(key, timeS());
			displayLists.put(key, displayList);
		}
	}
}
