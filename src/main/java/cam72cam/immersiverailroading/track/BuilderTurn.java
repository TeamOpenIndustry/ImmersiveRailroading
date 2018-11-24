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
        if (info.placementInfo.direction == TrackDirection.LEFT) {
            mat.rotate(Math.toRadians(90 - angle + 180), 0, 1, 0);
            mat.translate(radius, 0, radius);
            mat.rotate(Math.toRadians(90), 0, 1, 0);
        } else {
            mat.rotate(Math.toRadians(angle-90), 0, 1, 0);
        }
        CubicCurve curve = new CubicCurve(radius).apply(mat);
        switch(info.settings.quarters) {
            case 1:
                curve = curve.truncate(0.5).truncate(0.5);
                break;
            case 2:
                curve = curve.truncate(0.5);
                break;
            case 3:
                curve = curve.truncate(0.75);
                break;
            case 4:
                break;
        }
        return getPath(curve, stepSize);
    }
}
