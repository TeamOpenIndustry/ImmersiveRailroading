package cam72cam.immersiverailroading.gui.util;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.track.CubicCurve;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.BlendMode;
import cam72cam.mod.render.opengl.DirectDraw;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;

import java.util.List;

public class BezierRenderer {
    private final RenderState state;
    private final List<CubicCurve> curves;
    private static final float LINE_WIDTH = 1f;
    private static final Identifier lineImg = new Identifier(ImmersiveRailroading.MODID, "textures/line.png");
    private static final Identifier pointImg = new Identifier(ImmersiveRailroading.MODID, "textures/point.png");

    public BezierRenderer(RenderState state, List<CubicCurve> curves) {
        this.state = state;
        this.curves = curves;
    }

    public void drawLine(Vec3d start, Vec3d end, Color color, double xScale, double yScale, double width) {
        yScale = - yScale;
        start = new Vec3d(start.x * xScale, start.y * yScale, start.z);
        end = new Vec3d(end.x * xScale, end.y * yScale, end.z);

        Vec3d dir = end.subtract(start).normalize();
        Vec3d normal = new Vec3d(-dir.y, dir.x, 0).normalize().scale(width / 2);

        Vec3d p4 = start.add(normal);
        Vec3d p3 = start.subtract(normal);
        Vec3d p2 = end.subtract(normal);
        Vec3d p1 = end.add(normal);

        DirectDraw draw = new DirectDraw();

        draw.vertex(p1).color(color.r(), color.g(), color.b(), color.a()).uv(0, 0);
        draw.vertex(p2).color(color.r(), color.g(), color.b(), color.a()).uv(0, 1);
        draw.vertex(p3).color(color.r(), color.g(), color.b(), color.a()).uv(1, 1);
        draw.vertex(p4).color(color.r(), color.g(), color.b(), color.a()).uv(1, 0);

        draw.draw(state.clone().texture(Texture.wrap(lineImg))
                .alpha_test(false)
                .blend(new BlendMode(BlendMode.GL_SRC_ALPHA, BlendMode.GL_ONE_MINUS_SRC_ALPHA)));
    }

    public void drawBeziers(Color curveColor, Color pointColor, Color handlePointColor, Color handleLineColor, int iterations,  double xScale, double yScale) {
        double width = Math.abs(curves.get(curves.size() - 1).p2.x - curves.get(0).p1.x);
        for(CubicCurve curve : curves) {
            int localIteration = (int) Math.abs(iterations * (curve.p2.x - curve.p1.x) / width);
            drawBezier(curve, curveColor, localIteration, xScale, yScale);
            drawHandle(curve.p1,curve.ctrl1, pointColor, handlePointColor, handleLineColor, xScale, yScale);
            drawHandle(curve.p2,curve.ctrl2, pointColor, handlePointColor, handleLineColor, xScale, yScale);
        }
    }

    public void drawBezier(CubicCurve curve, Color color, int iterations,  double xScale, double yScale) {
        Vec3d prev = curve.position(0);
        for (int i = 1; i <= iterations; i++) {
            double t = i / (double) iterations;
            Vec3d curr = curve.position(t);
            drawLine(prev, curr, color, xScale, yScale, LINE_WIDTH);
            prev = curr;
        }
    }

    public void drawHandle(Vec3d point, Vec3d handlePoint, Color pointColor, Color handlePointColor, Color handleLineColor,  double xScale, double yScale) {
        drawLine(point, handlePoint, handleLineColor, xScale, yScale, LINE_WIDTH);
        drawPoint(point, handlePointColor, 4, xScale, yScale);
        drawPoint(handlePoint, pointColor, 4, xScale, yScale);
    }

    public void drawPoint(Vec3d pos, Color color, double size,  double xScale, double yScale) {
        yScale = - yScale;
        pos = new Vec3d(pos.x * xScale, pos.y * yScale, pos.z);

        double half = size / 2;

        DirectDraw draw = new DirectDraw();

        draw.vertex(pos.x - half, pos.y - half, pos.z).color(color.r(), color.g(), color.b(), color.a()).uv(0, 0);
        draw.vertex(pos.x - half, pos.y + half, pos.z).color(color.r(), color.g(), color.b(), color.a()).uv(0, 1);
        draw.vertex(pos.x + half, pos.y + half, pos.z).color(color.r(), color.g(), color.b(), color.a()).uv(1, 1);
        draw.vertex(pos.x + half, pos.y - half, pos.z).color(color.r(), color.g(), color.b(), color.a()).uv(1, 0);

        draw.draw(state.clone().texture(Texture.wrap(pointImg))
                .alpha_test(false)
                .blend(new BlendMode(BlendMode.GL_SRC_ALPHA, BlendMode.GL_ONE_MINUS_SRC_ALPHA)));
    }

    public void drawArrow(Vec3d pos, Color color, double size,  double xScale, double yScale) {
        pos = new Vec3d(pos.x * xScale, pos.y * yScale, pos.z);
        double half = size / 2;
        Vec3d corner1 = new Vec3d(-half, -half * 2, 0);
        Vec3d corner2 = new Vec3d(-half, half * 2, 0);
        Vec3d corner3 = new Vec3d(0, half * (2 + 1.732), 0);
        Vec3d corner4 = new Vec3d(half, half * 2, 0);
        Vec3d corner5 = new Vec3d(half, -half * 2, 0);

        drawLine(pos.add(corner1), pos.add(corner2), color, 1, 1, LINE_WIDTH / 2);
        drawLine(pos.add(corner2), pos.add(corner3), color, 1, 1, LINE_WIDTH / 2);
        drawLine(pos.add(corner3), pos.add(corner4), color, 1, 1, LINE_WIDTH / 2);
        drawLine(pos.add(corner4), pos.add(corner5), color, 1, 1, LINE_WIDTH / 2);
        drawLine(pos.add(corner5), pos.add(corner1), color, 1, 1, LINE_WIDTH / 2);
    }

    public void drawDashLine(Vec3d start, Vec3d end, Color color, double xScale, double yScale,
                             double width, double dashLength, double gapLength, double phaseOffset) {
        Vec3d worldStart = new Vec3d(start.x, start.y, start.z);
        Vec3d worldEnd = new Vec3d(end.x, end.y, end.z);
        double totalLength = worldStart.distanceTo(worldEnd);

        if (totalLength < 0.001) return;

        Vec3d dir = worldEnd.subtract(worldStart).normalize();
        double cycleLength = dashLength + gapLength;

        // Apply offset
        double currentDist = phaseOffset % cycleLength;
        if (currentDist < 0) currentDist += cycleLength;

        // firstSegment may get truncated
        boolean firstSegment = true;

        while (currentDist < totalLength) {
            double segStartDist = currentDist;
            double segEndDist;

            if (firstSegment && phaseOffset != 0) {

                segEndDist = Math.min(currentDist + dashLength - (phaseOffset % cycleLength), totalLength);
                firstSegment = false;
            } else {
                segEndDist = Math.min(currentDist + dashLength, totalLength);
            }

            if (segEndDist > segStartDist && segStartDist < totalLength) {
                Vec3d segStart = worldStart.add(dir.scale(segStartDist));
                Vec3d segEnd = worldStart.add(dir.scale(Math.min(segEndDist, totalLength)));
                drawLine(segStart, segEnd, color, xScale, yScale, width);
            }

            currentDist += cycleLength;
        }
    }
}
