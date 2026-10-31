package cam72cam.immersiverailroading.render.util;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.track.BuilderCubicCurve;
import cam72cam.immersiverailroading.track.CubicCurve;
import cam72cam.immersiverailroading.track.VecYPR;
import cam72cam.immersiverailroading.track.graph.*;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.math.Matrix3;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.opengl.*;
import cam72cam.mod.world.World;

import java.util.List;
import java.util.Map;

import static cam72cam.immersiverailroading.render.util.FlatCurveRenderer.lineImg;

public class VolumetricCurveRenderer {

    private static final Vec3d HANDLE_Y_OFFSET = new Vec3d(0, -1 / 32f, 0);
    private static final Vec3d CURVE_Y_OFFSET = new Vec3d(0, 1, 0);
    private static final Vec3d HANDLE_TOP_OFFSET = new Vec3d(0, 1 + 1 / 16f, 0);
    private static final Vec3d GAP_Y_OFFSET = new Vec3d(0, 1 + 1 / 32f, 0);

    private static final BlendMode BLEND_ALPHA =
            new BlendMode(BlendMode.GL_SRC_ALPHA, BlendMode.GL_ONE_MINUS_SRC_ALPHA);

    // ==================== 主入口（唯一外部接口） ====================

    public static void renderWireFrame(RenderState state, float partialTicks) {
        World world = MinecraftClient.getPlayer().getWorld();
        Vec3d playerPosition = MinecraftClient.getPlayer().getPosition();

        WorldData data = WorldData.get(world);
        if (data == null) return;

        state.blend(new BlendMode(BlendMode.GL_CONSTANT_ALPHA, BlendMode.GL_ONE).constantColor(1, 1, 1, 1f)).lightmap(1, 1);

        DirectDraw draw = new DirectDraw();
        double maxBlockDist = ConfigGraphics.TrackRenderDistance;
        double maxBlockDistSq = maxBlockDist * maxBlockDist;

        // 玩家当前所在 Region 的坐标
        long currentRegionId = TrackRegionUtil.vecToRegion(new Vec3i(playerPosition));
        int playerRx = TrackRegionUtil.regionX(currentRegionId);
        int playerRz = TrackRegionUtil.regionZ(currentRegionId);

        // 全量遍历所有 Region
        for (Map.Entry<Long, TrackRegion> region : data.regions.entrySet()) {
            for (Map.Entry<Vec3i, TrackMultiGeometrySegment> segmentEntry : region.getValue().trackBlocks.entrySet()) {
                TrackMultiGeometrySegment segment = segmentEntry.getValue();
                Vec3d segmentPos = new Vec3d(segment.getAbsPos());
                double ddx = playerPosition.x - segmentPos.x;
                double ddy = playerPosition.y - segmentPos.y;
                double ddz = playerPosition.z - segmentPos.z;
                if (ddx * ddx + ddy * ddy + ddz * ddz > maxBlockDistSq) continue;

                for (Map<Gauge, TrackSingleGeometrySegment> branch : segment.paths) {
                    for (Map.Entry<Gauge, TrackSingleGeometrySegment> singleEntry : branch.entrySet()) {
                        TrackSingleGeometrySegment single = singleEntry.getValue();
                        BuilderCubicCurve builder = single.getAndUpdateBuilder(world);
                        RailInfo info = builder.info;
                        CubicCurve curve = builder.getCurve();

                        Vec3d blockOffset = new Vec3d(single.getAbsPos()).add(info.placementInfo.placementPosition);

                        // 1. 方块线框（黄色）
                        Vec3d wireCenter = blockOffset
                                .add(info.placementInfo.placementPosition.scale(-1))
                                .add(0.5, 0.5, 0.5);
                        appendBlockWireFrame(draw, new VecYPR(wireCenter, 0), Color.YELLOW, 1, 1 / 16f);

                        // 2. 曲线（绿色）
                        appendCurve(draw, single.pointsCache, blockOffset.add(CURVE_Y_OFFSET), Color.LIME, 1 / 16f);

                        // 3. 控制柄（品红/青色/红色）
                        appendHandles(draw, curve, blockOffset.add(HANDLE_TOP_OFFSET),
                                Color.MAGENTA, Color.CYAN, Color.RED, 1 / 16f, 1 / 4f);
                    }
                }
            }
        }

        // 4. GAP 边（橙色）- 全量
        for (TrackEdge edge : data.edges) {
            if (edge.edgeType != TrackEdge.EdgeType.GAP) continue;

            TrackNode from = data.nodes.get(edge.from);
            TrackNode to = data.nodes.get(edge.to);
            if (from == null || to == null) continue;

            Vec3d f = new Vec3d(from.point.x, from.point.y, from.point.z).add(GAP_Y_OFFSET);
            Vec3d t = new Vec3d(to.point.x, to.point.y, to.point.z).add(GAP_Y_OFFSET);
            appendLineQuad(draw, f, t, Color.ORANGE, 1 / 16f);
        }

        draw.draw(state.clone().texture(Texture.wrap(lineImg))
                .alpha_test(false)
                .blend(BLEND_ALPHA));

        // 5. Region 边界框（全量，满足条件的画 16x16 网格）
        DirectDraw regionDraw = new DirectDraw();
        boolean anyRegion = false;

        for (long regionId : data.regions.keySet()) {
            int rx = TrackRegionUtil.regionX(regionId);
            int rz = TrackRegionUtil.regionZ(regionId);

            // 判断是否在玩家所在 Region 的 16x16 范围内（即 X/Z 各 ±8）
            boolean detailed = Math.abs(rx - playerRx) <= 8 && Math.abs(rz - playerRz) <= 8;

            int minX = rx << 9;
            int minZ = rz << 9;
            int maxX = minX + 512;
            int maxZ = minZ + 512;

            appendRegionBounds(regionDraw, minX, minZ, maxX, maxZ, detailed);
            anyRegion = true;
        }

        if (anyRegion) {
            regionDraw.draw(state.clone().texture(Texture.wrap(lineImg))
                    .alpha_test(false)
                    .blend(BLEND_ALPHA));
        }
    }

    // ==================== 私有绘制原语 ====================

    private static void appendBlockWireFrame(DirectDraw draw, VecYPR pos, Color color, double size, double lineWidth) {
        double inset = size * 0.002;
        double half = size / 2.0;
        double h0 = -half + inset;
        double h1 = half - inset;

        Vec3d[] local = {
                new Vec3d(h0, h0, h0),
                new Vec3d(h1, h0, h0),
                new Vec3d(h1, h0, h1),
                new Vec3d(h0, h0, h1),
                new Vec3d(h0, h1, h0),
                new Vec3d(h1, h1, h0),
                new Vec3d(h1, h1, h1),
                new Vec3d(h0, h1, h1),
        };

        Matrix3 m = pos.toMatrix3();
        Vec3d[] c = new Vec3d[8];
        for (int i = 0; i < 8; i++) {
            Vec3d r = m.apply(local[i]);
            c[i] = new Vec3d(pos.x + r.x, pos.y + r.y, pos.z + r.z);
        }

        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7},
        };

        for (int[] e : edges) {
            appendLineQuad(draw, c[e[0]], c[e[1]], color, lineWidth);
        }
    }

    private static void appendLineQuad(DirectDraw draw, Vec3d start, Vec3d end, Color color, double width) {
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double dz = end.z - start.z;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6) return;

        double ux = dx / len;
        double uy = dy / len;
        double uz = dz / len;

        double upx, upy, upz;
        if (Math.abs(uy) > 0.9) {
            upx = 1; upy = 0; upz = 0;
        } else {
            upx = 0; upy = 1; upz = 0;
        }

        double n1x = uy * upz - uz * upy;
        double n1y = uz * upx - ux * upz;
        double n1z = ux * upy - uy * upx;
        double n1Len = Math.sqrt(n1x * n1x + n1y * n1y + n1z * n1z);
        if (n1Len < 1e-6) return;
        n1x /= n1Len; n1y /= n1Len; n1z /= n1Len;

        double n2x = uy * n1z - uz * n1y;
        double n2y = uz * n1x - ux * n1z;
        double n2z = ux * n1y - uy * n1x;

        double half = width / 2;

        appendQuad(draw, start, end, n1x * half, n1y * half, n1z * half, color);
        appendQuad(draw, start, end, n2x * half, n2y * half, n2z * half, color);
    }

    private static void appendQuad(DirectDraw draw, Vec3d start, Vec3d end,
                                   double ox, double oy, double oz, Color color) {
        Vec3d p1 = new Vec3d(start.x + ox, start.y + oy, start.z + oz);
        Vec3d p2 = new Vec3d(start.x - ox, start.y - oy, start.z - oz);
        Vec3d p3 = new Vec3d(end.x - ox, end.y - oy, end.z - oz);
        Vec3d p4 = new Vec3d(end.x + ox, end.y + oy, end.z + oz);

        double r = color.r(), g = color.g(), b = color.b(), a = color.a();
        draw.vertex(p1).color(r, g, b, a).uv(0, 0);
        draw.vertex(p2).color(r, g, b, a).uv(0, 1);
        draw.vertex(p3).color(r, g, b, a).uv(1, 1);
        draw.vertex(p4).color(r, g, b, a).uv(1, 0);
    }

    private static void appendCurve(DirectDraw draw, List<VecYPR> points, Vec3d offset, Color color, double width) {
        if (points == null || points.size() < 2) return;
        for (int i = 0; i < points.size() - 1; i++) {
            VecYPR a = points.get(i);
            VecYPR b = points.get(i + 1);
            appendLineQuad(draw,
                    new Vec3d(a.x + offset.x, a.y + offset.y, a.z + offset.z),
                    new Vec3d(b.x + offset.x, b.y + offset.y, b.z + offset.z),
                    color, width);
        }
    }

    private static void appendHandles(DirectDraw draw, CubicCurve curve, Vec3d offset,
                                      Color handleColor, Color pointColor, Color controlColor,
                                      double width, double pointSize) {
        Vec3d p1 = curve.p1.add(offset);
        Vec3d ctrl1 = curve.ctrl1.add(offset);
        Vec3d ctrl2 = curve.ctrl2.add(offset);
        Vec3d p2 = curve.p2.add(offset);

        appendLineQuad(draw, p1, ctrl1, handleColor, width);
        appendLineQuad(draw, p2, ctrl2, handleColor, width);

        appendBlockWireFrame(draw, new VecYPR(p1.add(HANDLE_Y_OFFSET), 45, 0, 0), pointColor, pointSize, 1 / 16f);
        appendBlockWireFrame(draw, new VecYPR(ctrl1.add(HANDLE_Y_OFFSET), 45, 0, 0), controlColor, pointSize, 1 / 16f);
        appendBlockWireFrame(draw, new VecYPR(ctrl2.add(HANDLE_Y_OFFSET), 45, 0, 0), controlColor, pointSize, 1 / 16f);
        appendBlockWireFrame(draw, new VecYPR(p2.add(HANDLE_Y_OFFSET), 45, 0, 0), pointColor, pointSize, 1 / 16f);
    }

    private static void appendRegionBounds(DirectDraw draw, int minX, int minZ, int maxX, int maxZ, boolean detailed) {
        int y0 = 0;
        int y1 = 256;

        Vec3d[] corners = {
                new Vec3d(minX, y0, minZ),
                new Vec3d(maxX, y0, minZ),
                new Vec3d(maxX, y0, maxZ),
                new Vec3d(minX, y0, maxZ),
                new Vec3d(minX, y1, minZ),
                new Vec3d(maxX, y1, minZ),
                new Vec3d(maxX, y1, maxZ),
                new Vec3d(minX, y1, maxZ),
        };

        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7},
        };

        // 外框全量绘制
        for (int[] e : edges) {
            appendLineQuad(draw, corners[e[0]], corners[e[1]], Color.YELLOW, 1 / 2f);
        }

        if (!detailed) return;

        // 网格步长改为 32（512 / 32 = 16，即 16x16 网格）
        final int step = 32;
        double gridWidth = 1 / 16f;

        for (int y : new int[]{y0, y1}) {
            for (int x = minX + step; x < maxX; x += step) {
                appendLineQuad(draw, new Vec3d(x, y, minZ), new Vec3d(x, y, maxZ), Color.YELLOW, gridWidth);
            }
            for (int z = minZ + step; z < maxZ; z += step) {
                appendLineQuad(draw, new Vec3d(minX, y, z), new Vec3d(maxX, y, z), Color.YELLOW, gridWidth);
            }
        }

        for (int x : new int[]{minX, maxX}) {
            for (int z = minZ + step; z < maxZ; z += step) {
                appendLineQuad(draw, new Vec3d(x, y0, z), new Vec3d(x, y1, z), Color.YELLOW, gridWidth);
            }
            for (int y = y0 + step; y < y1; y += step) {
                appendLineQuad(draw, new Vec3d(x, y, minZ), new Vec3d(x, y, maxZ), Color.YELLOW, gridWidth);
            }
        }

        for (int z : new int[]{minZ, maxZ}) {
            for (int x = minX + step; x < maxX; x += step) {
                appendLineQuad(draw, new Vec3d(x, y0, z), new Vec3d(x, y1, z), Color.YELLOW, gridWidth);
            }
            for (int y = y0 + step; y < y1; y += step) {
                appendLineQuad(draw, new Vec3d(minX, y, z), new Vec3d(maxX, y, z), Color.YELLOW, gridWidth);
            }
        }
    }
}