package cam72cam.immersiverailroading.items.nbt;

import cam72cam.mod.serialization.TagCompound;

@FunctionalInterface
public interface NbtMigrator {
    void migrate(TagCompound data);
}
