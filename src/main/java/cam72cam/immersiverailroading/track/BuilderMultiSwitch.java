package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.SingleWayInfo;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

import java.util.ArrayList;
import java.util.List;

public class BuilderMultiSwitch extends BuilderBase implements IIterableTrack{
    private int wayCount = 0;
    private List<BuilderIterator> curveList = new ArrayList<>();//get(0)->straightBuilderReal; others->turnBuilder
    private BuilderCubicCurve parentBuilder;//->realStraightBuilder
    private BuilderCubicCurve intersection;//->straightBuilder

    public BuilderMultiSwitch(RailInfo info, World world, Vec3i pos) {
        super(info, world, pos);
        if( info.settings.multiSwitchInfo!=null && info.settings.multiSwitchInfo.typeAsChild==TrackItems.MULTISWITCH && !info.settings.multiSwitchInfo.wayList.isEmpty()){

            wayCount = info.settings.multiSwitchInfo.wayList.size();
            for(int i = 0;i<wayCount;i++){
                SingleWayInfo wayInfo = info.settings.multiSwitchInfo.wayList.get(i);
                RailInfo curveRailInfo = info.with(mutable -> {
                    mutable.settings = wayInfo.settings;
                    mutable.customInfo = wayInfo.customInfo;
                });

                BuilderCubicCurve wayBuilder = constructBuilder(curveRailInfo,curveRailInfo.settings.type);
                curveList.add(wayBuilder);

                if(i>0){//i>0
                    parentBuilder.positions.retainAll(wayBuilder.positions);
                    wayBuilder.overrideFlexible = true;
                    for(TrackBase curve : wayBuilder.tracks) {
                        if (curve instanceof TrackRail) {
                            curve.overrideParent(intersection.getParentPos());
                        }
                    }
                }else{//i=0
                    parentBuilder = constructBuilder(curveRailInfo.withSettings(mutable -> {
                        mutable.type = TrackItems.MULTISWITCH;
                        mutable.multiSwitchInfo.typeAsChild = curveRailInfo.settings.type;
                    }),curveRailInfo.settings.type);
                    intersection = constructBuilder(curveRailInfo.withSettings(mutable -> {
                        mutable.type = TrackItems.MULTISWITCH;
                        mutable.multiSwitchInfo.typeAsChild = curveRailInfo.settings.type;
                    }),curveRailInfo.settings.type);
                }
            }
        }else if( info.settings.multiSwitchInfo!=null && info.settings.multiSwitchInfo.typeAsChild!=TrackItems.MULTISWITCH ){
            intersection = constructBuilder(info,info.settings.multiSwitchInfo.typeAsChild);
            parentBuilder = constructBuilder(info,info.settings.multiSwitchInfo.typeAsChild);
            wayCount = 1;
            curveList.add(constructBuilder(info.withSettings(mutable -> {mutable.type = info.settings.multiSwitchInfo.typeAsChild;}),info.settings.multiSwitchInfo.typeAsChild));
        }else{
            ImmersiveRailroading.warn("Invalid RailInfo:"+info);
        }

        //need catching if intersection is null, assume it doesn't happen in common cases for now
        for (TrackBase curve : intersection.tracks) {
            if (curve instanceof TrackGag) {
                curve.setFlexible();
            }
        }
    }

    private BuilderCubicCurve constructBuilder(RailInfo railInfo, TrackItems type) {
        switch (type) {
            case STRAIGHT:
                return new BuilderStraight(railInfo, world, pos);
            case SLOPE:
                return new BuilderSlope(railInfo, world, pos);
            case TURN:
                return new BuilderTurn(railInfo, world, pos);
            case CUBICPARABOLA:
                return new BuilderCubicParabola(railInfo,world,pos);
            case CUSTOM:
                return new BuilderCubicCurve(railInfo, world, pos);
        }
        ImmersiveRailroading.warn("invalid way type:"+type);
        return null;
    }

    @Override
    public List<BuilderBase> getSubBuilders() {
        List<BuilderBase> res = new ArrayList<>();
        for (int i = 0; i < wayCount; i++) {
            List<BuilderBase> sub = curveList.get(i).getSubBuilders();
            if (sub == null) {
                res.add(curveList.get(i));
            } else {
                res.addAll(sub);
            }
        }
        return res;
    }

    @Override
    public int costTies() {
        int costTiles = 0;
        for(int i = 0;i<wayCount;i++){
            if(i==0){
                costTiles += intersection.costTies();
            }else{
                costTiles += curveList.get(i).costTies();
            }
        }
        return costTiles;
    }

    @Override
    public int costRails() {
        int costRails = 0;
        for(int i = 0;i<wayCount;i++){
            if(i==0){
                costRails += intersection.costRails();
            }else{
                costRails += curveList.get(i).costRails();
            }
        }
        return costRails;
    }

    @Override
    public int costBed() {
        int costBed = 0;
        for(int i = 0;i<wayCount;i++){
            if(i==0){
                costBed += intersection.costBed();
            }else{
                costBed += curveList.get(i).costBed();
            }
        }
        return costBed;
    }

    @Override
    public int costFill() {
        int costFill = 0;
        for(int i = 0;i<wayCount;i++){
            if(i==0){
                costFill += intersection.costFill();
            }else{
                costFill += curveList.get(i).costFill();
            }
        }
        return costFill;
    }

    @Override
    public void setDrops(List<ItemStack> drops) {
        if(intersection!=null) intersection.setDrops(drops);
    }


    @Override
    public boolean canBuild() {
        boolean canBuild = true;
        for(int i = 0;i<wayCount;i++){
            if(i==0){
                if(!intersection.canBuild()){
                    canBuild = false;
                    break;
                }
            }else{
                if(!curveList.get(i).canBuild()){
                    canBuild = false;
                    break;
                }
            }
        }
        return canBuild;
    }

    @Override
    public void build() {
        for(int i = 0;i<wayCount;i++){
            if(i==0){
                intersection.build();
            }else{
                curveList.get(i).build();
            }
        }
    }

    @Override
    public void clearArea() {
        for(int i = 0;i<wayCount;i++){
            if(i==0){
                intersection.clearArea();
            }else{
                curveList.get(i).clearArea();
            }
        }
    }

    @Override
    public List<TrackBase> getTracksForRender() {
        if(curveList!=null&& !curveList.isEmpty()){
            List<TrackBase> data = intersection.getTracksForRender();
            for(int i = 1;i<wayCount;i++){
                data.addAll(curveList.get(i).getTracksForRender());
            }
            return data;
        }
        return null;
    }

    @Override
    public List<VecYPR> getRenderData() {
        if(curveList!=null&& !curveList.isEmpty()){
            List<VecYPR> data = intersection.getRenderData();
            for(int i = 1;i<wayCount;i++){
                data.addAll(curveList.get(i).getRenderData());
            }
            return data;
        }
        return null;
    }

    @Override
    public List<VecYPR> getPath(double stepSize) {
        return parentBuilder.getPath(stepSize);
    }
}
