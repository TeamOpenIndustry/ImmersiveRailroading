package cam72cam.immersiverailroading.util;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.cutter.Plane;

import java.util.ArrayList;
import java.util.List;

public final class BlockPlaneHeight {

    private static final double EPS = 1e-8;

    private static final int[][] EDGES = {
            {0,1},
            {0,2},
            {0,4},
            {1,3},
            {1,5},
            {2,3},
            {2,6},
            {4,5},
            {4,6},
            {3,7},
            {5,7},
            {6,7}
    };

    private BlockPlaneHeight() {}

    /**
     * Calculate the center Y of plane intersection with a unit cube.
     *
     * @return center Y of intersection polygon
     */
    public static float calculate(Vec3d point, Vec3d normal) {

        List<Vec3d> intersections = getIntersections(point, normal);

        if (intersections.isEmpty()) {
            return 0;
        }

        double y = 0;

        for (Vec3d v : intersections) {
            y += v.y;
        }

        return (float) (y / intersections.size());
    }


    /**
     * Calculate highest Y of the clipped unit cube.
     *
     * The cube is clipped by the plane and the returned value
     * is the highest vertex height of the remaining volume.
     *
     * @return highest Y, or 1 if the plane does not cut the cube
     */
    public static float getFullHeight(Vec3d point, Vec3d normal) {

        List<Vec3d> vertices = getClippedVertices(point, normal);

        if (vertices.isEmpty()) {
            return 1;
        }

        double maxY = Double.NEGATIVE_INFINITY;

        for (Vec3d v : vertices) {
            maxY = Math.max(maxY, v.y);
        }

        return (float) maxY;
    }


    /**
     * Calculate lowest Y of plane intersection polygon.
     *
     * @return lowest intersection Y, or 0 if no intersection
     */
    public static float getCutPlaneMinHeight(Vec3d point, Vec3d normal) {

        List<Vec3d> intersections = getIntersections(point, normal);

        if (intersections.isEmpty()) {
            return 0;
        }

        double minY = Double.POSITIVE_INFINITY;

        for (Vec3d v : intersections) {
            minY = Math.min(minY, v.y);
        }

        return (float) minY;
    }

    /**
     * Calculate highest Y of plane intersection polygon.
     *
     * @return highest intersection Y, or 1 if no intersection
     */
    public static float getCutPlaneMaxHeight(Vec3d point, Vec3d normal) {

        List<Vec3d> intersections = getIntersections(point, normal);

        if (intersections.isEmpty()) {
            return 1;
        }

        double maxY = Double.NEGATIVE_INFINITY;

        for (Vec3d v : intersections) {
            maxY = Math.max(maxY, v.y);
        }

        return (float) maxY;
    }


    public static Vec3d[] fromPlane(Plane plane) {

        Vec3d normal = plane.normal;

        Vec3d point = normal.scale(
                -plane.d / normal.dotProduct(normal)
        );

        return new Vec3d[]{
                point,
                normal
        };
    }


    /**
     * Get vertices of cube after clipping by plane.
     */
    private static List<Vec3d> getClippedVertices(
            Vec3d point,
            Vec3d normal) {

        normal = normal.normalize();

        Vec3d[] corners = createCorners();

        List<Vec3d> result = new ArrayList<>();

        // Keep original cube vertices inside the half-space
        for (Vec3d corner : corners) {

            if (distance(corner, point, normal) >= -EPS) {
                result.add(corner);
            }
        }


        // Add plane-edge intersections
        for (int[] edge : EDGES) {

            Vec3d a = corners[edge[0]];
            Vec3d b = corners[edge[1]];

            double da = distance(a, point, normal);
            double db = distance(b, point, normal);

            if (da * db < 0) {

                double t = da / (da - db);

                Vec3d hit = a.add(
                        b.subtract(a).scale(t)
                );

                addUnique(result, hit);
            }
        }

        return result;
    }


    public static Plane createBottomSidePlane(Plane plane) {
        Vec3d[] raw = fromPlane(plane);
        Vec3d point = raw[0];
        Vec3d normal = raw[1];
        List<Vec3d> bottom = getBottomIntersections(point, normal);

        if (bottom.size() != 2) {
            return null;
        }

        Vec3d p0 = bottom.get(0);
        Vec3d p1 = bottom.get(1);

        Vec3d edge = p1.subtract(p0).normalize();

        // vertical plane normal
        Vec3d candidateNormal =
                edge.crossProduct(new Vec3d(0, 1, 0))
                        .normalize();

        Vec3d center = p0.add(p1).scale(0.5);

        /*
         * 判断哪个方向是未切区域
         *
         * 原切割面:
         * distance > 0 的一侧认为是保留区域
         */
        Vec3d testOffset =
                candidateNormal.scale(0.01);

        double positive =
                distance(
                        center.add(testOffset),
                        point,
                        normal
                );

        double negative =
                distance(
                        center.subtract(testOffset),
                        point,
                        normal
                );


        // candidateNormal 指向被切掉区域，翻转
        if (positive < negative) {
            candidateNormal = candidateNormal.scale(-1);
        }


        /*
         * Plane:
         * n dot x + d = 0
         */
        double d =
                -candidateNormal.dotProduct(center);


        return new Plane(candidateNormal, d);
    }


    private static List<Vec3d> getBottomIntersections(
            Vec3d point,
            Vec3d normal
    ) {

        normal = normal.normalize();

        Vec3d[] corners = {
                new Vec3d(0,0,0),
                new Vec3d(1,0,0),
                new Vec3d(0,0,1),
                new Vec3d(1,0,1)
        };

        int[][] edges = {
                {0,1},
                {0,2},
                {1,3},
                {2,3}
        };


        List<Vec3d> result = new ArrayList<>();


        for (int[] edge : edges) {

            Vec3d a = corners[edge[0]];
            Vec3d b = corners[edge[1]];

            double da = distance(a, point, normal);
            double db = distance(b, point, normal);


            if (Math.abs(da-db) < EPS) {
                continue;
            }


            if (da * db <= 0) {

                double t = da / (da-db);

                Vec3d hit =
                        a.add(
                                b.subtract(a)
                                        .scale(t)
                        );

                addUnique(result, hit);
            }
        }

        return result;
    }


    /**
     * Get intersection points between plane and cube edges.
     */
    private static List<Vec3d> getIntersections(
            Vec3d point,
            Vec3d normal) {

        normal = normal.normalize();

        Vec3d[] corners = createCorners();

        List<Vec3d> intersections = new ArrayList<>();

        for (int[] edge : EDGES) {

            Vec3d a = corners[edge[0]];
            Vec3d b = corners[edge[1]];

            double da = distance(a, point, normal);
            double db = distance(b, point, normal);


            if (Math.abs(da - db) < EPS) {
                continue;
            }


            if (da * db <= 0) {

                double t = da / (da - db);

                Vec3d hit = a.add(
                        b.subtract(a).scale(t)
                );

                addUnique(intersections, hit);
            }
        }

        return intersections;
    }


    private static Vec3d[] createCorners() {

        Vec3d[] corners = new Vec3d[8];

        int i = 0;

        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    corners[i++] = new Vec3d(x, y, z);
                }
            }
        }

        return corners;
    }


    private static void addUnique(
            List<Vec3d> list,
            Vec3d value) {

        for (Vec3d v : list) {

            if (Math.abs(v.x - value.x) < EPS
                    && Math.abs(v.y - value.y) < EPS
                    && Math.abs(v.z - value.z) < EPS) {

                return;
            }
        }

        list.add(value);
    }


    private static double distance(
            Vec3d p,
            Vec3d plane,
            Vec3d normal) {

        return p.subtract(plane)
                .dotProduct(normal);
    }
}