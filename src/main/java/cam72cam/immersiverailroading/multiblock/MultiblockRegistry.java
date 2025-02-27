package cam72cam.immersiverailroading.multiblock;

import java.util.*;

public class MultiblockRegistry {
	private static final Map<String, Multiblock> entries = new HashMap<>();
	
	private MultiblockRegistry() {
		
	}
	
	public static void register(String name, Multiblock mb) {
		entries.put(name, mb);
	}
	
	public static Multiblock get(String name) {
		return entries.get(name);
	}

	public static List<String> keys() {
		return new ArrayList<>(entries.keySet());
	}

	public static Map<String, Multiblock> entries(){
		return entries;
	}

	public static List<Multiblock> registered() {
		return new ArrayList<>(entries.values());
	}
}
