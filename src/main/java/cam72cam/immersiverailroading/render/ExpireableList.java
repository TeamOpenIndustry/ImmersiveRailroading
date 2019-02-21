package cam72cam.immersiverailroading.render;

import java.util.*;

public class ExpireableList<K,V> {
	
	public int lifespan() {
		return 10;
	}
	public boolean sliding() {
		return true;
	}
	
	public void onRemove(K key, V value) {
		
	}
	
	private static long timeS() {
		return System.currentTimeMillis() / 1000L;
	}
	
	private Map<K, V> map = new HashMap<K, V>();
	private Map<K, Long> mapUsage = new HashMap<K, Long>();
	private long lastTime = timeS();
	
	public V get(K key) {
		synchronized(this) {
			if (lastTime + lifespan() < timeS()) {
				// clear unused
				Set<K> ks = new HashSet<K>();
				ks.addAll(map.keySet());
				for (K dk : ks) {
					if (dk != key && mapUsage.get(dk) + lifespan() < timeS()) {
						onRemove(dk, map.get(dk));
						map.remove(dk);
						mapUsage.remove(dk);
					}
				}
				lastTime = timeS();
			}
			
			
			if (map.containsKey(key)) {
				if (sliding()) {
					mapUsage.put(key, timeS());
				}
				return map.get(key);
			}
			return null;
		}
	}

	public void put(K key, V displayList) {
		synchronized(this) {
			mapUsage.put(key, timeS());
			map.put(key, displayList);
		}
	}

	public Collection<V> values() {
		synchronized(this) {
			if (lastTime + lifespan() < timeS()) {
				// clear unused
				Set<K> ks = new HashSet<K>();
				ks.addAll(map.keySet());
				for (K dk : ks) {
					if (mapUsage.get(dk) + lifespan() < timeS()) {
						onRemove(dk, map.get(dk));
						map.remove(dk);
						mapUsage.remove(dk);
					}
				}
				lastTime = timeS();
			}

			return map.values();
		}
	}
}
