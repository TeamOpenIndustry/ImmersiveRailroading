package cam72cam.immersiverailroading.items.nbt;

import cam72cam.immersiverailroading.library.TrackPositionType;
import cam72cam.mod.serialization.TagCompound;

public class NbtMigrators {
    public static void migrateTrackAlignment(TagCompound data) {
        if (data.hasKey("pos_type")) {
            if (!data.hasKey("pos_type")) {
                return;
            }

            TrackPositionType oldPosType = data.getEnum("pos_type", TrackPositionType.class);

            TagCompound nearEnd = data.get("nearPointData");
            if (nearEnd == null) {
                nearEnd = new TagCompound();
                data.set("nearPointData", nearEnd);
            }
            if (!nearEnd.hasKey("pos_type")) {
                nearEnd.setEnum("pos_type", oldPosType);
            }

            TagCompound farEnd = data.get("farPointData");
            if (farEnd == null) {
                farEnd = new TagCompound();
                data.set("farPointData", farEnd);
            }
            if (!farEnd.hasKey("pos_type")) {
                farEnd.setEnum("pos_type", oldPosType);
            }

            data.remove("pos_type");
        }
    }
}
