package cam72cam.mod.entity;

import java.util.HashMap;
import java.util.Map;

public class TempReg {
    private static final Map<Class<? extends EntityBase>, EntitySettings> registered = new HashMap<>();
    private TempReg() {

    }
    public static <T extends EntityBase> void register(Class<T> cls, EntitySettings settings) {
        registered.put(cls, settings);
    }
    public static EntitySettings getSettings(Class<? extends EntityBase> cls) {
        return registered.get(cls);
    }
}
