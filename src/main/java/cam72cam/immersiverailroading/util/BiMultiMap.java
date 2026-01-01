package cam72cam.immersiverailroading.util;

import java.util.*;
import java.util.Collections;

public class BiMultiMap<K, V> {
    private final Map<K, Set<V>> keyToValues = new HashMap<>();
    private final Map<V, Set<K>> valueToKeys = new HashMap<>();

    public void put(K key, V value) {
        keyToValues.computeIfAbsent(key, k -> new HashSet<>()).add(value);
        valueToKeys.computeIfAbsent(value, v -> new HashSet<>()).add(key);
    }

    public Set<V> getValues(K key) {
        Set<V> values = keyToValues.get(key);
        return values != null ? Collections.unmodifiableSet(values) : Collections.emptySet();
    }

    public Set<K> getKeys(V value) {
        Set<K> keys = valueToKeys.get(value);
        return keys != null ? Collections.unmodifiableSet(keys) : Collections.emptySet();
    }

    public boolean containsKey(K key) {
        return keyToValues.containsKey(key);
    }

    public boolean containsValue(V value) {
        return valueToKeys.containsKey(value);
    }

    public boolean containsEntry(K key, V value) {
        Set<V> values = keyToValues.get(key);
        return values != null && values.contains(value);
    }

    public void remove(K key, V value) {
        // Remove from key->values mapping
        Set<V> values = keyToValues.get(key);
        if (values != null) {
            values.remove(value);
            if (values.isEmpty()) {
                keyToValues.remove(key);
            }
        }

        // Remove from value->keys mapping
        Set<K> keys = valueToKeys.get(value);
        if (keys != null) {
            keys.remove(key);
            if (keys.isEmpty()) {
                valueToKeys.remove(value);
            }
        }
    }

    public void removeKey(K key) {
        Set<V> values = keyToValues.remove(key);
        if (values != null) {
            for (V value : values) {
                Set<K> keys = valueToKeys.get(value);
                if (keys != null) {
                    keys.remove(key);
                    if (keys.isEmpty()) {
                        valueToKeys.remove(value);
                    }
                }
            }
        }
    }

    public void removeValue(V value) {
        Set<K> keys = valueToKeys.remove(value);
        if (keys != null) {
            for (K key : keys) {
                Set<V> values = keyToValues.get(key);
                if (values != null) {
                    values.remove(value);
                    if (values.isEmpty()) {
                        keyToValues.remove(key);
                    }
                }
            }
        }
    }

    public Set<K> keySet() {
        return Collections.unmodifiableSet(keyToValues.keySet());
    }

    public Set<V> valueSet() {
        return Collections.unmodifiableSet(valueToKeys.keySet());
    }

    public int size() {
        return keyToValues.size();
    }

    public boolean isEmpty() {
        return keyToValues.isEmpty();
    }

    public void clear() {
        keyToValues.clear();
        valueToKeys.clear();
    }

    @Override
    public String toString() {
        return keyToValues.toString();
    }
}