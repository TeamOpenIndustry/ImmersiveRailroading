package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import util.Matrix4;

public class BuilderCubicParabola extends BuilderCubicCurve{
    public BuilderCubicParabola(RailInfo info, World world, Vec3i pos) {
        super(info, world, pos);
    }

    @Override
    public CubicCurve getCurve() {
        int radius = info.settings.length -1;//why does Radius of turn is length-1?

        Matrix4 mat = new Matrix4();
        mat.rotate(Math.toRadians(info.placementInfo.yaw-90), 0, 1, 0);
        if (info.placementInfo.direction == TrackDirection.LEFT) {
            mat.scale(1, 1, -1);
        }
        CubicCurve curve;
        if(info.settings.farRadius < 0){
            curve = CubicCurve.cubicParabolaAngle(radius, info.settings.degrees, info.settings.isForward).apply(mat);
        }else{
            curve = CubicCurve.cubicParabolaR1R2Angle(radius, info.settings.degrees, info.settings.farRadius).apply(mat);
        }

        double height = info.customInfo.placementPosition.y - info.placementInfo.placementPosition.y;
        curve = new CubicCurve(curve.p1, curve.ctrl1, curve.ctrl2.add(0, height, 0), curve.p2.add(0, height, 0)).linearize(info.settings.smoothing,info.settings.pitchStart,info.settings.pitchEnd);
        return curve;
    }
}
