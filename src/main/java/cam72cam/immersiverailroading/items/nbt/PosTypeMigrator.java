package cam72cam.immersiverailroading.items.nbt;

import cam72cam.immersiverailroading.library.TrackPositionType;
import cam72cam.mod.serialization.TagCompound;

public class PosTypeMigrator implements NbtMigrator {
    @Override
    public void migrate(TagCompound data) {
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
    }
}
