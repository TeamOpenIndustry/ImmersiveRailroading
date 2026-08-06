package cam72cam.immersiverailroading.util;

import cam72cam.mod.math.Vec3d;

public final class BlockPlaneHeight {

    private BlockPlaneHeight() {}


    /**
     * Calculate the highest Y where plane intersects a unit cube.
     *
     * Cube:
     * min = (0,0,0)
     * max = (1,1,1)
     *
     * plane:
     * point + normal
     *
     * @return highest intersection Y
     */
    public static float calculate(
            Vec3d point,
            Vec3d normal) {


        normal = normal.normalize();


        Vec3d[] corners = new Vec3d[8];

        int i = 0;

        for(int x = 0; x <= 1; x++) {
            for(int y = 0; y <= 1; y++) {
                for(int z = 0; z <= 1; z++) {

                    corners[i++] =
                            new Vec3d(
                                    x,
                                    y,
                                    z
                            );
                }
            }
        }


        double maxY = -Double.MAX_VALUE;


        // check top face first
        for(int x=0;x<=1;x++) {
            for(int z=0;z<=1;z++) {

                Vec3d p =
                        new Vec3d(
                                x,
                                1,
                                z
                        );

                if(onPlane(p, point, normal)) {
                    return 1.0f;
                }
            }
        }


        // intersect every cube edge

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


        for(int[] edge: edges) {

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


            if(da * db <= 0) {

                double t =
                        da / (da - db);


                Vec3d hit =
                        a.add(
                                b.subtract(a)
                                        .scale(t)
                        );


                maxY =
                        Math.max(
                                maxY,
                                hit.y
                        );
            }
        }


        if(maxY == -Double.MAX_VALUE) {
            return 0;
        }


        return (float)maxY;
    }



    private static boolean onPlane(
            Vec3d p,
            Vec3d plane,
            Vec3d normal) {

        return Math.abs(
                distance(
                        p,
                        plane,
                        normal
                )
        ) < 1e-6;
    }



    private static double distance(
            Vec3d p,
            Vec3d plane,
            Vec3d normal) {

        return p.subtract(plane)
                .dotProduct(normal);
    }
}