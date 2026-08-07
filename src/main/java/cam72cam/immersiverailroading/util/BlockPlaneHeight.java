package cam72cam.immersiverailroading.util;

import cam72cam.mod.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class BlockPlaneHeight {

    private static final double EPS = 1e-8;

    private BlockPlaneHeight() {}

    /**
     * Calculate the center Y of plane intersection with a unit cube.
     *
     * Cube:
     * min = (0,0,0)
     * max = (1,1,1)
     *
     * @return center Y of intersection polygon
     */
    public static float calculate(Vec3d point, Vec3d normal) {

        normal = normal.normalize();

        Vec3d[] corners = new Vec3d[8];

        int i = 0;

        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    corners[i++] = new Vec3d(x, y, z);
                }
            }
        }


        int[][] edges = {
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


        List<Vec3d> intersections =
                new ArrayList<>();


        for (int[] edge : edges) {

            Vec3d a = corners[edge[0]];
            Vec3d b = corners[edge[1]];


            double da =
                    distance(
                            a,
                            point,
                            normal
                    );

            double db =
                    distance(
                            b,
                            point,
                            normal
                    );


            if (Math.abs(da - db) < EPS) {
                continue;
            }


            if (da * db <= 0) {

                double t =
                        da / (da - db);


                Vec3d hit =
                        a.add(
                                b.subtract(a)
                                        .scale(t)
                        );


                addUnique(
                        intersections,
                        hit
                );
            }
        }


        if (intersections.isEmpty()) {
            return 0;
        }


        double y = 0;

        for (Vec3d v : intersections) {
            y += v.y;
        }


        return (float)
                (y / intersections.size());
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