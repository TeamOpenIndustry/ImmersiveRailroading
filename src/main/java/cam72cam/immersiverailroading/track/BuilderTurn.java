package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.util.math.BlockPos;
import util.Matrix4;

import java.util.List;

public class BuilderTurn extends BuilderCubicCurve {
    public BuilderTurn(RailInfo info, BlockPos pos) {
        super(info, pos);
    }

    @Override
    public List<PosStep> getPath(double stepSize) {
        int radius = info.settings.length - 1;

        float angle = info.placementInfo.rotationQuarter / 4f * 90;

        Matrix4 mat = new Matrix4();
        mat.rotate(Math.toRadians(angle-90), 0, 1, 0);
        if (info.placementInfo.direction == TrackDirection.LEFT) {
            mat.scale(1, 1, -1);
        }
        CubicCurve curve = CubicCurve.circle(radius, info.settings.quarters/4f*90).apply(mat);
        return getPath(curve, stepSize);
    }
}
