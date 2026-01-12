package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.util.MultiSwitchInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.SingleWayInfo;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class BuilderMultiSwitch extends BuilderBase implements IIterableTrack{
    //"Straight" is just a word from BuilderSwitch, can be 5 kinds of curves, it is used to mark the parent builders
    private BuilderCubicCurve realStraightBuilder;
    private BuilderIterator straightBuilder;
    private BuilderCubicCurve straightBuilderReal;
    private List<BuilderIterator> turnBuilders = new ArrayList<>();

    public BuilderMultiSwitch(RailInfo info, World world, Vec3i pos) {
        super(info, world, pos);

        List<Vec3i> childParentPosList = new ArrayList<>();

        @Nullable
        MultiSwitchInfo multiSwitchInfo = info.multiSwitchInfo;

        int wayAmount = multiSwitchInfo != null ? multiSwitchInfo.wayList.size() : 0;
        TrackItems realShapeOfStraight = multiSwitchInfo != null ? multiSwitchInfo.realShapeType:null;//需要保证，只要不是null，里面的内容就不是null

        straightBuilder = constructBuilder(info,realShapeOfStraight);//子级别也是相同的info，只要父级正常构建子级就能正常构建
        realStraightBuilder = constructBuilder(info,realShapeOfStraight);
        straightBuilderReal = constructBuilder(info.withSettings(mutable -> mutable.type = realShapeOfStraight),realShapeOfStraight);

        for(int i = 0 ; i<wayAmount; i++){
            //Only STRAIGHT,SLOPE,TURN,CUBICPARABOLA,CUSTOM are valid
            RailInfo turnInfo = fromSingleWayInfo(multiSwitchInfo.wayList.get(i));
            BuilderIterator turnBuilder = (BuilderIterator) turnInfo.getBuilder(world,pos);

            childParentPosList.add(turnBuilder.getParentPos());

            turnBuilder.overrideFlexible = true;
            for(TrackBase turn : turnBuilder.tracks) {
                if (turn instanceof TrackRail) {
                    turn.overrideParent(straightBuilder.getParentPos());
                }
            }
            straightBuilder.positions.retainAll(turnBuilder.positions);
            turnBuilders.add(turnBuilder);
        }

        for (TrackBase straight : straightBuilder.tracks) {
            if (straight instanceof TrackGag) {
                straight.setFlexible();
            }

            Vec3i pos1 = straight.getPos();
            Vec3i pos2 = straightBuilder.getParentPos();
            if(pos1.equals(pos2)){
                straight.setChildList(childParentPosList);//需要检查pos对不对,不知道有没有用了相对位置的
            }
        }
    }

    private RailInfo fromSingleWayInfo(SingleWayInfo singleWayInfo) {
        return new RailInfo(singleWayInfo.settings,singleWayInfo.placementInfo,singleWayInfo.customInfo,new MultiSwitchInfo(null,singleWayInfo.settings.type,singleWayInfo.wayOrder,true), SwitchState.NONE,SwitchState.NONE,0);
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

        for (BuilderIterator turn : turnBuilders) {
            List<BuilderBase> subTurns = turn.getSubBuilders();
            if (subTurns == null) {
                res.add(turn);
            } else {
                res.addAll(subTurns);
            }
        }

        List<BuilderBase> subStraights = straightBuilderReal.getSubBuilders();
        if (subStraights == null) {
            res.add(straightBuilderReal);
        } else {
            res.addAll(subStraights);
        }

        return res.isEmpty() ? null : res;
    }

    @Override
    public int costTies() {
        int costTiles = straightBuilder.costTies();
        for(BuilderIterator turn : turnBuilders)costTiles += turn.costTies();
        return costTiles;
    }

    @Override
    public int costRails() {
        int costRails = straightBuilder.costRails();
        for(BuilderIterator turn : turnBuilders)costRails += turn.costRails();
        return costRails;
    }

    @Override
    public int costBed() {
        int costBed = straightBuilder.costBed();
        for(BuilderIterator turn : turnBuilders)costBed += turn.costBed();
        return costBed;
    }

    @Override
    public int costFill() {
        int costFill = straightBuilder.costFill();
        for(BuilderIterator turn : turnBuilders)costFill += turn.costFill();
        return costFill;
    }

    @Override
    public void setDrops(List<ItemStack> drops) {
        if(straightBuilder !=null) straightBuilder.setDrops(drops);
    }


    @Override
    public boolean canBuild() {
        boolean canBuild = straightBuilder.canBuild();
        for(BuilderIterator turn : turnBuilders){
            if(!turn.canBuild()){
                canBuild = false;
                break;
            }
        }
        return canBuild;
    }

    @Override
    public void build() {
        straightBuilder.build();
        for(BuilderIterator turn : turnBuilders)turn.build();
    }

    @Override
    public void clearArea() {
        straightBuilder.clearArea();
        for(BuilderIterator turn : turnBuilders)turn.clearArea();
    }

    @Override
    public List<TrackBase> getTracksForRender() {
        List<TrackBase> data = straightBuilder.getTracksForRender();
        for(BuilderIterator turn : turnBuilders)data.addAll(turn.getTracksForRender());
        return data;
    }

    @Override
    public List<VecYPR> getRenderData() {
        List<VecYPR> data = straightBuilder.getRenderData();
        for(BuilderIterator turn : turnBuilders)data.addAll(turn.getRenderData());
        return data;
    }

    @Override
    public List<VecYPR> getPath(double stepSize) {
        return realStraightBuilder.getPath(stepSize);
    }
}
