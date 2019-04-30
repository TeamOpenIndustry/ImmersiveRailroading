package cam72cam.mod.entity;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.util.Identifier;
import net.minecraftforge.fml.common.registry.EntityRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Registry {
    private static final Map<String, Function<ModdedEntity, Entity>> constructors = new HashMap<>();
    private static final Map<String, EntitySettings> registered = new HashMap<>();
    private Registry() {

    }
    public static void register(String modID, Function<ModdedEntity, Entity> ctr, EntitySettings settings) {
        Entity tmp = ctr.apply(null);
        Class<? extends Entity> type = tmp.getClass();
        Identifier id = new Identifier(modID, type.getSimpleName());

        // This has back-compat for older entity names
        EntityRegistry.registerModEntity(id.internal, ModdedEntity.class, type.getSimpleName(), constructors.size(), ImmersiveRailroading.instance, ImmersiveRailroading.ENTITY_SYNC_DISTANCE, 20, false);

        constructors.put(id.toString(), ctr);
        registered.put(id.toString(), settings);
    }
    public static EntitySettings getSettings(String type) {
        return registered.get(type);
    }

    public static Function<ModdedEntity, Entity> getConstructor(String type) {
        return constructors.get(type);
    }

    public static Entity create(String type, ModdedEntity base) {
        return getConstructor(type).apply(base);
    }
}
