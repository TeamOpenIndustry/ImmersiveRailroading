package cam72cam.immersiverailroading.items.nbt;

import cam72cam.immersiverailroading.library.TrackPositionType;
import cam72cam.mod.serialization.TagCompound;

import java.util.ArrayList;
import java.util.List;

public class NbtMigratorRegistry {
    private static final List<NbtMigrator> migrations = new ArrayList<>();

    static {
        register(data -> {
            if (data.hasKey("pos_type")) {
                TrackPositionType oldPosType = data.getEnum("pos_type", TrackPositionType.class);
                TagCompound nearData = data.get("nearPointData");
                if (nearData == null) {
                    nearData = new TagCompound();
                    data.set("nearPointData", nearData);
                }
                if (!nearData.hasKey("pos_type")) {
                    nearData.setEnum("pos_type", oldPosType);
                }
                data.remove("pos_type");
            }
        });
    }

    public static void register(NbtMigrator migration) {
        migrations.add(migration);
    }

    public static void applyAll(TagCompound data) {
        for (NbtMigrator migration : migrations) {
            migration.migrate(data);
        }
    }

    @FunctionalInterface
    public static interface NbtMigrator {
        void migrate(TagCompound data);
    }
}
