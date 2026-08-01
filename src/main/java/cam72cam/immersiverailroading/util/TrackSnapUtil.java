package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.items.nbt.RailSettings;
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

import java.util.List;

public class TrackSnapUtil {
    public static VecYPR getNeighborNode(Player player, World world, Vec3i pos, Vec3d hit, ItemStack stack) {
        RailSettings stackInfo = RailSettings.from(stack);
        EndPointData endPointData = stackInfo.nearPointData;
        Vec3d worldPos = new Vec3d(pos).add(hit);
        Vec3d minPos = worldPos;
        double min = Double.MAX_VALUE;
        int hori = Math.max((int) (stackInfo.gauge.scale() * 2), 1);
        int vert = 1;
        float yaw = player.getRotationYaw();
        float pitch = 0;
        float roll = 0;
        float rotationYawHead = (player.getRotationYawHead() + 360f) % 360f;

        for (int x = -hori; x <= hori; x++) {
            for (int y = -vert; y <= vert; y++) {
                for (int z = -hori; z <= hori; z++) {
                    Vec3i offset = pos.add(x, y, z);
                    TileRailBase tile = world.getBlockEntity(offset, TileRailBase.class);
                    while (tile != null){
                        if (!(tile instanceof TileRail)) {
                            tile = tile.getParentTile();
                        }

                        TileRail rail = (TileRail) tile;
                        if (rail == null || rail.info == null ||
                                Math.abs(rail.getTrackGauges()[0] - stackInfo.gauge.value()) > 1.0E-6) continue;

                        BuilderBase builder = rail.info.getBuilder(world);
                        List<VecYPR> renderData = builder.getRenderData();
                        if (renderData.isEmpty()) continue;

                        if (renderData.size() > 1) {
                            Vec3d p1 = renderData.get(0)
                                    .add(rail.info.placementInfo.placementPosition)
                                    .add(tile.getPos());
                            float yaw1 = renderData.get(0).getYaw();
                            float pitch1 = renderData.get(0).getPitch();
                            float roll1 = renderData.get(0).getRoll();
                            double dist1 = p1.distanceTo(worldPos);
                            if (dist1 < min) {
                                min = dist1;
                                minPos = p1;
                                yaw = yaw1;
                                pitch = pitch1;
                                roll = -roll1;
                            }

                            Vec3d p2 = renderData.get(renderData.size() - 1).add(rail.info.placementInfo.placementPosition).add(tile.getPos());
                            float yaw2 = renderData.get(renderData.size() - 1).getYaw();
                            float pitch2 = renderData.get(renderData.size() - 1).getPitch();
                            float roll2 = renderData.get(renderData.size() - 1).getRoll();
                            double dist2 = p2.distanceTo(worldPos);
                            if (dist2 < min) {
                                min = dist2;
                                minPos = p2;
                                yaw = yaw2 + 180;
                                pitch = -pitch2;
                                roll = roll2;
                            }
                        } else {
                            Vec3d p = renderData.get(0)
                                    .add(rail.info.placementInfo.placementPosition)
                                    .add(tile.getPos());
                            float currentYaw = renderData.get(0).getYaw();
                            float currentPitch = renderData.get(0).getPitch();
                            if (Math.abs(currentYaw - rotationYawHead) > 90) {
                                currentYaw += 180;
                            }
                            double dist = p.distanceTo(worldPos);
                            if (dist < min) {
                                min = dist;
                                minPos = p;
                                yaw = currentYaw;
                                pitch = currentPitch;
                            }
                        }

                        tile = tile.getReplacedTile();
                    }
                }
            }
        }

        if (min <= hori) {
            yaw = endPointData.trackSnapSettings().snapYaw() ? (540 - yaw) % 360 : rotationYawHead;
            return new VecYPR(minPos, yaw, pitch, roll);
        }
        return null;
    }

    //TODO: keep stack info so that we wont lost it after applying snapping

    public static SnappedResult applyNearSnapAndAdjust(
            Player player, World world, Vec3i pos, Vec3d hit,
            ItemStack stack, RailSettings stackInfo
    ) {
        boolean succeeded = false;
        float yaw = player.getRotationYawHead();
        VecYPR snapped = null;
        if(stackInfo.nearPointData.trackSnapSettings().snapPos()) {
            snapped = TrackSnapUtil.getNeighborNode(player, player.getWorld(), pos, hit, stack);
            if(snapped != null) {
                succeeded = true;
                pos = new Vec3i(snapped.x, snapped.y, snapped.z);
                hit = snapped.subtract(pos);

                if(stackInfo.nearPointData.trackSnapSettings().snapYaw()) yaw = snapped.getYaw();
                if(stackInfo.nearPointData.trackSnapSettings().snapPitch() && stackInfo.smoothing == TrackSmoothing.PITCH_LOCKED) {
                    float newPitch = stackInfo.nearPointData.pitchDegreeMode() ? snapped.getPitch() : (float) (Math.tan(Math.toRadians(snapped.getPitch())) * 1000);
                    EndPointData nearPointData = stackInfo.nearPointData.with(mutable -> mutable.pitch = newPitch);
                    stackInfo = stackInfo.with(mutable -> mutable.nearPointData = nearPointData);
                }
                if(stackInfo.nearPointData.trackSnapSettings().snapRoll() && stackInfo.rollAndOffsetInfo != null) {
                    double newRoll = stackInfo.rollAndOffsetInfo.degreeMode() ? snapped.getRoll() : stackInfo.gauge.value() * 100 * Math.sin(Math.toRadians(snapped.getRoll()));
                    RollAndOffsetInfo.Mutable rollAndOffsetInfo = stackInfo.rollAndOffsetInfo.mutable();
                    rollAndOffsetInfo.tryDeltaValue(0, newRoll, RollAndOffsetInfo.ExtraInfoType.ROLL);
                    stackInfo = stackInfo.with(mutable -> mutable.rollAndOffsetInfo = rollAndOffsetInfo.immutable());
                }
                stackInfo.write(stack);
            }
        }

        pos = pos.up();

        if (BlockUtil.canBeReplaced(world, pos.down(), true)) {
            if (!BlockUtil.isIRRail(world, pos.down()) || world.getBlockEntity(pos.down(), TileRailBase.class).getRailHeight() < 0.5) {
                pos = pos.down();
            }
        }

        return new SnappedResult(pos, hit, yaw, succeeded);
    }

    public static SnappedResult applyFarSnapAndAdjust(
            Player player, World world, Vec3i pos, Vec3d hit,
            ItemStack stack, RailSettings stackInfo
    ) {
        boolean succeeded = false;
        float yaw = player.getRotationYawHead();
        VecYPR snapped = null;
        if(stackInfo.farPointData.trackSnapSettings().snapPos()) {
            snapped = TrackSnapUtil.getNeighborNode(player, player.getWorld(), pos, hit, stack);
            if(snapped != null) {
                succeeded = true;
                pos = new Vec3i(snapped.x, snapped.y, snapped.z);
                hit = snapped.subtract(pos);

                if(stackInfo.farPointData.trackSnapSettings().snapYaw()) yaw = snapped.getYaw();
                if(stackInfo.farPointData.trackSnapSettings().snapPitch() && stackInfo.smoothing == TrackSmoothing.PITCH_LOCKED) {
                    float newPitch = stackInfo.farPointData.pitchDegreeMode() ? snapped.getPitch() : (float) (Math.tan(Math.toRadians(snapped.getPitch())) * 1000);
                    float finalNewPitch = -newPitch;// Invert as direction is on the opposite
                    EndPointData farPointData = stackInfo.farPointData.with(mutable -> mutable.pitch = finalNewPitch);
                    stackInfo = stackInfo.with(mutable -> mutable.farPointData = farPointData);
                }
                if(stackInfo.farPointData.trackSnapSettings().snapRoll() && stackInfo.rollAndOffsetInfo != null) {
                    double newRoll = stackInfo.rollAndOffsetInfo.degreeMode() ? snapped.getRoll() : stackInfo.gauge.value() * 100 * Math.sin(Math.toRadians(snapped.getRoll()));
                    RollAndOffsetInfo.Mutable rollAndOffsetInfo = stackInfo.rollAndOffsetInfo.mutable();
                    newRoll = -newRoll;// Invert as direction is on the opposite
                    rollAndOffsetInfo.tryDeltaValue(1.0, newRoll, RollAndOffsetInfo.ExtraInfoType.ROLL);
                    stackInfo = stackInfo.with(mutable -> mutable.rollAndOffsetInfo = rollAndOffsetInfo.immutable());
                }
                stackInfo.write(stack);
            }
        }

        pos = pos.up();

        if (BlockUtil.canBeReplaced(world, pos.down(), true)) {
            if (!BlockUtil.isIRRail(world, pos.down()) || world.getBlockEntity(pos.down(), TileRailBase.class).getRailHeight() < 0.5) {
                pos = pos.down();
            }
        }

        return new SnappedResult(pos, hit, yaw, succeeded);
    }

    public record SnappedResult(Vec3i pos, Vec3d hit, float yaw, boolean succeeded) {}
}