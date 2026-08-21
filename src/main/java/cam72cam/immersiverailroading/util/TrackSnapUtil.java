package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.TrackSmoothing;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.track.VecYPR;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

import java.util.*;

//TODO: existing snapped tile preview issues: 1.very high bed thickness 2.very big roll
// so tile preview is not 100% accurate yet...
public class TrackSnapUtil {
    public static VecYPR getNeighborNode(Player player, World world, Vec3i pos, Vec3d hit, ItemStack stack, boolean isNear) {
        RailSettings stackInfo = RailSettings.from(stack);
        EndPointData endPointData = isNear ? stackInfo.nearPointData : stackInfo.farPointData;

        Vec3d worldPos = new Vec3d(pos).add(hit);
        Vec3d minPos = worldPos;
        double min = Double.MAX_VALUE;

        int hori = Math.max((int) (stackInfo.gauge.scale() * 2), 1);
        int vert = 1;

        boolean succeeded = false;

        float yaw = player.getRotationYaw();
        float yawHead = (540 - yaw) % 360;
        float pitch = 0;
        float roll = 0;
        float rotationYawHead = (player.getRotationYawHead() + 360f) % 360f;

        for (int x = -hori; x <= hori; x++) {
            for (int y = -vert; y <= vert; y++) {
                for (int z = -hori; z <= hori; z++) {
                    Vec3i offset = pos.add(x, y, z);
                    TileRailBase tile = world.getBlockEntity(offset, TileRailBase.class);

                    // Use identity map because overlapping rails may have identical positions.
                    Set<TileRailBase> visited = Collections.newSetFromMap(new IdentityHashMap<>());

                    while (tile != null) {
                        // Prevent infinite loops due to cyclic replaced chains.
                        if (!visited.add(tile)) {
                            break;
                        }

                        /*
                         * Do not overwrite 'tile' directly.
                         * If tile is a Gag, get its parent Rail; otherwise use tile itself.
                         * This preserves the replaced chain for later traversal.
                         */
                        TileRail rail = tile instanceof TileRail
                                ? (TileRail) tile
                                : tile.getParentTile();

                        if (rail != null
                                && rail.info != null
                                && Math.abs(rail.getTrackGauges()[0] - stackInfo.gauge.value()) <= 1.0E-6) {

                            BuilderBase builder = rail.info.getBuilder(world);
                            List<VecYPR> renderData = builder.getRenderData();

                            if (!renderData.isEmpty()) {
                                // Offset applied to all endpoints from this rail's origin.
                                Vec3d railOffset = rail.info.placementInfo.placementPosition.add(rail.getPos());

                                if (renderData.size() > 1) {
                                    // First endpoint (near side, reverse roll to match facing)
                                    VecYPR first = renderData.getFirst();
                                    Vec3d p1 = first.add(railOffset);
                                    float yaw1 = first.getYaw();
                                    float pitch1 = first.getPitch();
                                    float roll1 = first.getRoll();
                                    double dist1 = p1.distanceTo(worldPos);

                                    if (dist1 < min ||
                                            (endPointData.trackSnapSettings().snapYaw()
                                                    && dist1 - 0.5 < min
                                                    && succeeded
                                                    && VecUtil.delta(yaw1, yawHead) < VecUtil.delta(yaw, yawHead))) {
                                        min = dist1;
                                        minPos = p1;
                                        yaw = yaw1;
                                        pitch = pitch1;
                                        roll = -roll1;
                                        succeeded = true;
                                    }

                                    // Last endpoint (far side, reverse yaw and pitch)
                                    VecYPR last = renderData.getLast();
                                    Vec3d p2 = last.add(railOffset);
                                    float yaw2 = last.getYaw();
                                    float pitch2 = last.getPitch();
                                    float roll2 = last.getRoll();
                                    double dist2 = p2.distanceTo(worldPos);

                                    if (dist2 < min ||
                                            (endPointData.trackSnapSettings().snapYaw()
                                                    && dist2 - 0.5 < min
                                                    && succeeded
                                                    && VecUtil.delta(yaw2, yawHead) < VecUtil.delta(yaw, yawHead))) {
                                        min = dist2;
                                        minPos = p2;
                                        yaw = yaw2 + 180;
                                        pitch = -pitch2;
                                        roll = roll2;
                                        succeeded = true;
                                    }
                                } else {
                                    // Single endpoint track: no inherent direction; pick the orientation closer to player's facing.
                                    VecYPR data = renderData.getFirst();
                                    Vec3d p = data.add(railOffset);
                                    float currentYaw = data.getYaw();
                                    float currentPitch = data.getPitch();
                                    float currentRoll = data.getRoll();

                                    if (VecUtil.delta(currentYaw, rotationYawHead) > 90) {
                                        currentYaw += 180;
                                        currentRoll = -currentRoll;
                                    }

                                    double dist = p.distanceTo(worldPos);
                                    if (dist < min ||
                                            (endPointData.trackSnapSettings().snapYaw()
                                                    && dist - 0.5 < min
                                                    && succeeded
                                                    && VecUtil.delta(currentYaw, yawHead) < VecUtil.delta(yaw, yawHead))) {
                                        min = dist;
                                        minPos = p;
                                        yaw = currentYaw + 180;
                                        pitch = -currentPitch;
                                        roll = currentRoll;
                                        succeeded = true;
                                    }
                                }
                            }
                        }

                        // Continue along the replaced chain for historical overlays.
                        tile = tile.getReplacedTile();
                    }
                }
            }
        }

        if (min <= hori) {
            yaw = endPointData.trackSnapSettings().snapYaw()
                    ? (540 - yaw) % 360
                    : rotationYawHead;
            return new VecYPR(minPos, yaw, pitch, roll);
        }

        return null;
    }

    //TODO: keep stack info so that we wont lost it after applying snapping

    public static SnappedResult applySnapAndAdjust(Player player, World world, Vec3i pos, Vec3d hit, ItemStack stack, boolean isNear, boolean isPreView) {
        RailSettings stackInfo = RailSettings.from(stack);
        boolean succeeded = false;
        float yaw = player.getRotationYawHead();
        VecYPR snapped = null;

        EndPointData pointData = isNear ? stackInfo.nearPointData : stackInfo.farPointData;

        if (pointData.trackSnapSettings().snapPos()) {
            snapped = TrackSnapUtil.getNeighborNode(player, world, pos, hit, stack, isNear);
            if (snapped != null) {
                succeeded = true;

                if(Math.abs(Math.round(snapped.y) - snapped.y) < 1e-4) {
                    snapped = snapped.add(new Vec3d(0, -snapped.y + Math.round(snapped.y), 0));
                }

                pos = new Vec3i(snapped.x, snapped.y, snapped.z);
                hit = snapped.subtract(pos);

                if (pointData.trackSnapSettings().snapYaw()) {
                    yaw = snapped.getYaw();
                }

                if (pointData.trackSnapSettings().snapRoll() && stackInfo.rollAndOffsetInfo != null) {
                    double newRoll = stackInfo.rollAndOffsetInfo.degreeMode()
                            ? snapped.getRoll()
                            : stackInfo.gauge.value() * 100 * Math.sin(Math.toRadians(snapped.getRoll()));
                    if (!isNear) newRoll = -newRoll;

                    RollAndOffsetInfo.Mutable rollMutable = stackInfo.rollAndOffsetInfo.mutable();
                    rollMutable.tryDeltaValue(isNear ? 0.0 : 1.0, newRoll, RollAndOffsetInfo.ExtraInfoType.ROLL);
                    stackInfo = stackInfo.with(mutable -> mutable.rollAndOffsetInfo = rollMutable.immutable());
                }

                if (pointData.trackSnapSettings().snapHeight()) {
                    Vec3d offset = new Vec3d(0, hit.y, 0);
                    if(stackInfo.rollAndOffsetInfo != null) {// Must be done after roll is confirmed
                        RollAndOffsetInfo.RollAndVertOffsetAlignType type = stackInfo.rollAndOffsetInfo.rollOffsetType();
                        double rawRoll = stackInfo.rollAndOffsetInfo.getRawRoll(isNear ? 0.0 : 1.0);
                        if(!isNear) rawRoll = -rawRoll;
                        double rollOffset = stackInfo.rollAndOffsetInfo.degreeMode()
                                ? Math.sin(Math.toRadians(rawRoll)) * Gauge.STANDARD * stackInfo.gauge.scale()
                                : rawRoll * 0.01 * stackInfo.gauge.scale();
                        if(!isNear) rollOffset = -rollOffset;

                        if(type == RollAndOffsetInfo.RollAndVertOffsetAlignType.LEFT) {
                            offset = offset.add(0, rollOffset / 2, 0);
                        } else if(type == RollAndOffsetInfo.RollAndVertOffsetAlignType.RIGHT) {
                            offset = offset.subtract(0, rollOffset / 2, 0);
                        }
                    }

                    Vec3d finalOffset = offset;
                    EndPointData updated = pointData.with(mutable -> mutable.offset = finalOffset);

                    pointData = updated;
                    stackInfo = isNear
                            ? stackInfo.with(mutable -> mutable.nearPointData = updated)
                            : stackInfo.with(mutable -> mutable.farPointData = updated);
                }

                if (pointData.trackSnapSettings().snapPitch() && stackInfo.smoothing == TrackSmoothing.PITCH_SPECIFIED) {
                    float newPitch = pointData.pitchDegreeMode()
                            ? snapped.getPitch()
                            : (float) (Math.tan(Math.toRadians(snapped.getPitch())) * 1000);
                    EndPointData updated = pointData.with(mutable -> mutable.pitch = isNear ? newPitch : -newPitch);

                    pointData = updated;
                    stackInfo = isNear
                            ? stackInfo.with(mutable -> mutable.nearPointData = updated)
                            : stackInfo.with(mutable -> mutable.farPointData = updated);
                }

                stackInfo.write(stack);
            }
        }

        if (succeeded) {
            if(!isPreView) {
                pos = new Vec3i(snapped);
                hit = snapped.subtract(pos);
            } else {// This logic is strongly bind to TilePreview
                pos = new Vec3i(snapped);
                pos = pos.up().up();

                if (BlockUtil.canBeReplaced(world, pos.down(), false)) {
                    if (!BlockUtil.isIRRail(world, pos.down()) || world.getBlockEntity(pos.down(), TileRailBase.class).getRailHeight() <= 0.5) {
                        pos = pos.down();
                    }
                }
                if (BlockUtil.canBeReplaced(world, pos.down(), false)) {
                    if (!BlockUtil.isIRRail(world, pos.down()) || world.getBlockEntity(pos.down(), TileRailBase.class).getRailHeight() <= 0.5) {
                        pos = pos.down();
                    }
                }

                hit = snapped.subtract(pos);
            }
        } else {
            pos = pos.up().up();

            if (BlockUtil.canBeReplaced(world, pos.down(), false)) {
                if (!BlockUtil.isIRRail(world, pos.down()) || world.getBlockEntity(pos.down(), TileRailBase.class).getRailHeight() <= 0.5) {
                    pos = pos.down();
                    hit = hit.add(0, -1, 0);
                }
            }
            if (BlockUtil.canBeReplaced(world, pos.down(), false)) {
                if (!BlockUtil.isIRRail(world, pos.down()) || world.getBlockEntity(pos.down(), TileRailBase.class).getRailHeight() <= 0.5) {
                    pos = pos.down();
                    hit = hit.add(0, -1, 0);
                }
            }

            if(hit.y - Math.floor(hit.y) >= 0.5) hit = hit.add(0, 1, 0);
        }

        return new SnappedResult(pos, hit, yaw, succeeded);
    }

    public record SnappedResult(Vec3i pos, Vec3d hit, float yaw, boolean succeeded) {}
}