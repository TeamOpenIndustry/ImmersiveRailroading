package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.util.math.BlockPos;
import util.Matrix4;

public class BuilderTurn extends BuilderCubicCurve {
    public BuilderTurn(RailInfo info, BlockPos pos) {
        super(info, pos);
    }

    @Override
    public CubicCurve getCurve() {
        int radius = info.settings.length - 1;

        Matrix4 mat = new Matrix4();
        mat.rotate(Math.toRadians(info.placementInfo.yaw-90), 0, 1, 0);
        if (info.placementInfo.direction == TrackDirection.LEFT) {
            mat.scale(1, 1, -1);
        }
        CubicCurve curve = CubicCurve.circle(radius, info.settings.quarters/4f*90).apply(mat);

        double height = info.customInfo.placementPosition.y - info.placementInfo.placementPosition.y;
        if (height != 0) {
            curve = new CubicCurve(curve.p1, curve.ctrl1, curve.ctrl2.addVector(0, height, 0), curve.p2.addVector(0, height, 0));
        }
        return curve;
    }
}
