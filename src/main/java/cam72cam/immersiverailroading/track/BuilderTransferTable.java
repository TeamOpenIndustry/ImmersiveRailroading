package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

import java.util.ArrayList;
import java.util.List;

public class BuilderTransferTable extends BuilderBase {
    public BuilderTransferTable(RailInfo info, World world, Vec3i pos) {
        super(info.withSettings(b -> b.length = Math.min(info.settings.length, BuilderTurnTable.maxLength(info.settings.gauge))), world, pos);

        int vertMin = -info.settings.length / 2;
        int vertMax = info.settings.length % 2 == 0 ? info.settings.length/2 : info.settings.length/2 + 1;

        int halfGauge = (int) Math.floor((info.settings.gauge.value() * 1.1 + 0.5) / 2);
        int width = info.settings.transfertableEntrySpacing * (info.settings.transfertableEntryCount - 1) + halfGauge + 2;

        Vec3i mainOffset = new Vec3i(-width / 2, 1, info.settings.length / 2);
        mainOffset = mainOffset.rotate(Rotation.from(info.placementInfo.facing()));

        this.setParentPos(mainOffset.down());
        TrackRail main = new TrackRail(this, mainOffset.down());
        tracks.add(main);
        for(int i = vertMin; i < vertMax; i++){
            for(int j = -halfGauge - 1 - width / 2; j < width - width / 2; j++){
                TrackGag gag1 = new TrackGag(this, mainOffset.add(
                        new Vec3i(-j, -1, i).rotate(Rotation.from(info.placementInfo.facing()))));
                if(i == vertMin || i == vertMax - 1 || j == -halfGauge -1 -width / 2 || j == width - 1 -width / 2) {
                    gag1.setBedHeight(1);
                    gag1.setFlexible();
                }
                tracks.add(gag1);
            }
        }

        for(int i = vertMin; i < vertMax; i++){
            for(int j = -halfGauge - 1 - width / 2; j < width - width / 2; j++){
                TrackGag gag = new TrackGag(this, mainOffset.add(
                        new Vec3i(-j, 0, i).rotate(Rotation.from(info.placementInfo.facing()))));
                gag.solidNotRequired = true;
                gag.setHeight(0.000001f);
                if(i == vertMin || i == vertMax - 1 || j == -halfGauge -1 -width / 2 || j == width - 1 -width / 2) {
                    gag.setHeight(0);
                    gag.setFlexible();
                }
                tracks.add(gag);
            }
        }

//        this.first = new Vec3i(info.placementInfo.placementPosition);
//        this.rot = Rotation.from(info.placementInfo.facing().getOpposite());
    }

    @Override
    public List<VecYawPitch> getRenderData() {
        List<VecYawPitch> list = new ArrayList<>();

        if (info.itemHeld) {
            for (int i = 0; i < info.settings.length; i += info.settings.length - 1) {
                for (int j = 0; j < info.settings.transfertableEntryCount; j++) {
                    Vec3i vec = new Vec3i(-j * info.settings.transfertableEntrySpacing, 1, i)
                            .rotate(Rotation.from(info.placementInfo.facing()));
                    list.add(new VecYawPitch(vec.x, vec.y, vec.z, info.placementInfo.facing().getAngle()));
                }
            }
        }

        Vec3d vec = new Vec3d(-info.tablePos, 1, info.settings.length / 2d - 0.5).rotateYaw(-info.placementInfo.facing().getAngle() + 180);
        list.add(new VecYawPitch(vec.x, vec.y, vec.z, info.placementInfo.facing().getAngle(), 0, info.settings.length, "RAIL_RIGHT", "RAIL_LEFT"));
        return list;
    }

    public int costTies() {
        return (int)Math.ceil(this.info.settings.length * Config.ConfigBalance.TieCostMultiplier);
    }

    public int costRails() {
        return (int)Math.ceil(this.info.settings.length + 8*2/3 * Config.ConfigBalance.RailCostMultiplier);
    }

    public int costBed() {
        //TODO suit transfer table
        return (int)Math.ceil(this.tracks.size()/2.0 * 0.1 * Config.ConfigBalance.BedCostMultiplier);
    }

    public int costFill() {
        int fillCount = 0;
        for (TrackBase track : tracks) {
            if (track.rel.y == 1) {
                continue;
            }
            if (BlockUtil.canBeReplaced(world, track.getPos().down(), false)) {
                fillCount += 1;
            }
        }
        return (int)Math.ceil(!this.info.settings.railBedFill.isEmpty() ? fillCount : 0);
    }

    @Override
    public List<TrackBase> getTracksForRender() {
        return this.tracks;
    }
}
