package cam72cam.immersiverailroading.items.nbt;

import cam72cam.mod.serialization.TagCompound;

import java.util.ArrayList;
import java.util.List;

public class NbtMigratorRegistry {
    private static final List<NbtMigrator> migrations = new ArrayList<>();

    static {
        register(new PosTypeMigrator());
    }

    public static void register(NbtMigrator migration) {
        migrations.add(migration);
    }

    public static void applyAll(TagCompound data) {
        for (NbtMigrator migration : migrations) {
            migration.migrate(data);
        }
    }
}
