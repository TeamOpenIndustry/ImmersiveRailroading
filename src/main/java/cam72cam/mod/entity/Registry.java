package cam72cam.mod.entity;

import cam72cam.mod.world.World;
import cam72cam.mod.resource.Identifier;
import net.minecraftforge.fml.common.registry.EntityRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Registry {
    private static final Map<Class<? extends Entity>, String> identifiers = new HashMap<>();
    private static final Map<String, Function<ModdedEntity, Entity>> constructors = new HashMap<>();
    private static final Map<String, EntitySettings> registered = new HashMap<>();

    private Registry() {

    }
    public static void register(String modID, Function<ModdedEntity, Entity> ctr, EntitySettings settings, Object mod, int distance) {
        Entity tmp = ctr.apply(null);
        Class<? extends Entity> type = tmp.getClass();
        Identifier id = new Identifier(modID, type.getSimpleName());

        // This has back-compat for older entity names
        // TODO expose updateFreq and vecUpdates
        EntityRegistry.registerModEntity(id.internal, ModdedEntity.class, type.getSimpleName(), constructors.size(), mod, distance, 20, false);

        identifiers.put(type, id.toString());
        constructors.put(id.toString(), ctr);
        registered.put(id.toString(), settings);
    }
    public static EntitySettings getSettings(String type) {
        return registered.get(type);
    }

    public static Function<ModdedEntity, Entity> getConstructor(String type) {
        return constructors.get(type);
    }

    protected static Entity create(String type, ModdedEntity base) {
        return getConstructor(type).apply(base);
    }

    public static Entity create(World world, Class<? extends Entity> cls) {
        //TODO null checks
        ModdedEntity ent = new ModdedEntity(world.internal);
        String id = identifiers.get(cls);
        ent.init(id);
        return ent.getSelf();
    }
}
