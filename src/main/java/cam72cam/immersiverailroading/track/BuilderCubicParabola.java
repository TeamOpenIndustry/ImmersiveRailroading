package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import util.Matrix4;

public class BuilderCubicParabola extends BuilderCubicCurve{
    public BuilderCubicParabola(RailInfo info, World world, Vec3i pos) {
        super(info, world, pos);
    }

    @Override
    public CubicCurve getCurve() {
        Matrix4 mat = new Matrix4();
        mat.rotate(Math.toRadians(info.placementInfo.yaw - 90), 0, 1, 0);
        if (info.placementInfo.direction == TrackDirection.LEFT) {
            mat.scale(1, 1, -1);
        }

        if(info.settings.degrees > CubicCurve.cubicParabolaMaxAngle) {
            ImmersiveRailroading.error("Invalid cubicParabola degree %s, convert to a safe value", info.settings.degrees);
            info = info.withSettings(b -> b.degrees = 22.5f);
        }

        CubicCurve curve;
        float nearRadius = info.settings.nearPointData.radius();
        float farRadius = info.settings.farPointData.radius();
        if(Math.abs(farRadius) < 1e-6){
            curve = CubicCurve.cubicParabolaByAngle(nearRadius, info.settings.degrees, false, 0, 1).apply(mat);
        } else if(Math.abs(nearRadius) < 1e-6) {
            curve = CubicCurve.cubicParabolaByAngle(farRadius, info.settings.degrees, true, 0, 1).apply(mat);
        } else if(nearRadius > 0.5 && farRadius > 0.5){
            if(Math.abs(nearRadius - farRadius) < 1e-6) curve = CubicCurve.circleClassic(nearRadius, info.settings.degrees, 0, 1);// Fallback: Turn
            else curve = CubicCurve.cubicParabolaByAngle(nearRadius, farRadius, info.settings.degrees, 0, 1).apply(mat);
        } else {// Fallback: Straight
            curve = new CubicCurve(
                    Vec3d.ZERO,
                    VecUtil.fromYaw(info.settings.length * 0.25, info.placementInfo.yaw),
                    VecUtil.fromYaw(info.settings.length * 0.75, info.placementInfo.yaw),
                    VecUtil.fromYaw(info.settings.length, info.placementInfo.yaw),
                    0,1
            );
        }

        double height = info.customInfo.placementPosition.y - info.placementInfo.placementPosition.y;
        curve = new CubicCurve(curve.p1, curve.ctrl1, curve.ctrl2.add(0, height, 0), curve.p2.add(0, height, 0), 0, 1)
                .linearize(info.settings.smoothing, info.settings.nearPointData, info.settings.farPointData);
        return curve;
    }
}