package cam72cam.immersiverailroading.util;

/** The inclination of the bogie chord, in IR's front-positive passenger convention. */
final class SpawnOrientation {
    private SpawnOrientation() {}

    static float pitch(double frontX, double frontY, double frontZ,
                       double rearX, double rearY, double rearZ, float fallback) {
        double dx = frontX - rearX;
        double dy = frontY - rearY;
        double dz = frontZ - rearZ;
        double horizontal = Math.hypot(dx, dz);
        // An invalid/degenerate chord cannot establish a new orientation.
        if (!Double.isFinite(horizontal) || !Double.isFinite(dy) || horizontal < 1e-6) {
            return fallback;
        }
        return (float) Math.toDegrees(Math.atan2(dy, horizontal));
    }
}
