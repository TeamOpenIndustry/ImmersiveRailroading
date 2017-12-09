package cam72cam.immersiverailroading.multiblock;

import java.util.HashMap;
import java.util.Map;

public class MultiblockRegistry {
	private static final Map<String, Multiblock> entries = new HashMap<String, Multiblock>(); 
	
	private MultiblockRegistry() {
		
	}
	
	public static void register(String name, Multiblock mb) {
		entries.put(name, mb);
	}
	
	public static Multiblock get(String name) {
		return entries.get(name);
	}
}
