package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;

public class MultiblockControlChangePacket extends Packet {
    @TagField
    private Vec3i tilePos;

    @TagField
    private String groupName;

    @TagField
    private float value;

    public MultiblockControlChangePacket(Vec3i tilePos, String groupName, float value) {
        this.tilePos = tilePos;
        this.groupName = groupName;
        this.value = value;
    }

    public MultiblockControlChangePacket() {

    }

    @Override
    protected void handle() {
        TileMultiblock tile = this.getWorld().getBlockEntity(tilePos, TileMultiblock.class);
        tile.setControlPosition(groupName, value);
        tile.markDirty();
    }
}
