package cam72cam.immersiverailroading.net.offline;

import cam72cam.immersiverailroading.track.graph.TrackRegionUtil;
import cam72cam.immersiverailroading.track.graph.WorldData;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;

import java.util.List;

/**
 * 局部增量同步包：只包含单个 region 内若干个 block 的 PUT/REMOVE 操作。
 *
 * 注意：raw 字段继承自 Packet 基类（protected byte[] raw），由 Packet.Message
 * 的 toBytes/fromBytes 单独处理，绝不能加 @TagField，否则 TagSerializer 会
 * 尝试反射构造 byte[] 并抛 NoSuchMethodException: [B.<init>()。
 */
public class TrackRegionDeltaPacket extends Packet {
    @TagField
    private World world;
    @TagField
    private long id;

    public TrackRegionDeltaPacket() {
        // Reflection
    }

    public TrackRegionDeltaPacket(World world, long id, List<WorldData.DeltaOp> ops) {
        this.world = world;
        this.id = id;
        // 使用基类的 protected byte[] raw，走 Packet 的特殊序列化通道
        this.raw = WorldData.serializeDeltaOps(ops);
    }

    @Override
    protected void handle() {
        WorldData data = WorldData.getOrCreate(world);
        if (data == null) return;

        if (raw == null || raw.length == 0) return;

        // 关键：region 尚未加载时丢弃，避免凭空创建与全量数据不一致的空 region
        if (!data.hasRegion(id)) return;

        List<WorldData.DeltaOp> ops = WorldData.deserializeDeltaOps(raw, world, id);
        for (WorldData.DeltaOp op : ops) {
            Vec3i absPos = TrackRegionUtil.toBlockPos(id, op.regionBlockPos);
            switch (op.kind) {
                case PUT -> data.setTrackBlock(absPos, op.block);
                case REMOVE -> data.removeTrackBlock(absPos);
                case SWITCH_STATE -> data.clientApplySwitchStateDelta(id, op.regionBlockPos, op.switchState, op.offline, op.currentTime);
            }
        }
    }
}