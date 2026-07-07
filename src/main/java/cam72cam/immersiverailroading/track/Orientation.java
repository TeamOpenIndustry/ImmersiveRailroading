package cam72cam.immersiverailroading.track;

import cam72cam.mod.math.Vec3d;
import util.Matrix4;

/**
 * Represents an orthonormal local coordinate system.
 *
 * The orientation is stored directly as three perpendicular basis vectors:
 *
 *     forward - local forward direction
 *     right   - local right direction
 *     up      - local up direction
 *
 * Unlike Euler angles, this representation avoids gimbal lock during most
 * geometric operations and is better suited for track generation where local
 * axes are frequently reconstructed from geometry.
 *
 * Yaw/Pitch/Roll are only used when converting to or from rendering data.
 */
public class Orientation {

    /** Local forward direction (track direction). */
    public final Vec3d forward;

    /** Local right direction. */
    public final Vec3d right;

    /** Local up direction (track normal). */
    public final Vec3d up;


    private Orientation(Vec3d forward, Vec3d right, Vec3d up) {
        this.forward = forward.normalize();
        this.right = right.normalize();
        this.up = up.normalize();
    }

    /**
     * Constructs an orientation from forward and right vectors.
     *
     * The supplied vectors do not need to be perfectly orthogonal.
     * The right vector is projected onto the plane perpendicular to forward,
     * then the up vector is reconstructed using a cross product.
     *
     * This guarantees an orthonormal basis.
     */
    public Orientation(Vec3d forward, Vec3d right) {
        Vec3d f = forward.normalize();

        Vec3d r = right.subtract(
                f.scale(right.dotProduct(f))
        ).normalize();

        Vec3d u = f.crossProduct(r).normalize();

        this.forward = f;
        this.right = r;
        this.up = u;
    }


    public static Orientation fromYPR(VecYPR cur) {
        return fromYPR(cur.getYaw(), cur.getPitch(), cur.getRoll());
    }

    /**
     * Constructs an orientation from Euler angles.
     *
     * IMPORTANT:
     * The rotation order must exactly match the render pipeline:
     *
     *     Yaw   (around local/world Y)
     *     Pitch (around local X)
     *     Roll  (around local Z)
     *
     * This order is intentionally kept identical to renderPiece() so that
     * converting between Orientation and VecYPR is lossless under normal
     * operating ranges.
     */
    public static Orientation fromYPR(float yaw, float pitch, float roll) {

        Matrix4 mat = new Matrix4();

        mat.rotate(Math.toRadians(yaw), 0, 1, 0);
        mat.rotate(Math.toRadians(pitch), 1, 0, 0);
        mat.rotate(Math.toRadians(roll), 0, 0, 1);

        Vec3d right = new Vec3d(
                mat.m00,
                mat.m10,
                mat.m20
        );

        Vec3d up = new Vec3d(
                mat.m01,
                mat.m11,
                mat.m21
        );

        Vec3d forward = new Vec3d(
                mat.m02,
                mat.m12,
                mat.m22
        );

        return new Orientation(forward, right, up);
    }

    /*
     * This method is intended for serialization and rendering only.
     * For geometric editing, prefer operating directly on Orientation
     * instead of repeatedly converting through Euler angles.
     */
    /**
     * Extracts Euler angles from the current orientation.
     *
     * The extracted angles assume the same Yaw-Pitch-Roll order used by
     * fromYPR().
     *
     * Since Euler angles are not unique, multiple angle combinations may
     * represent the same orientation. This method returns one valid solution.
     *
     * Near pitch = +/-90 degrees the representation becomes singular
     * (gimbal lock), in which case roll is set to zero.
     */
    public VecYPR toYPR() {

        Matrix4 m = toMatrix();

        double pitch = -Math.asin(m.m12);
        double cp = Math.cos(pitch);

        double yaw;
        double roll;

        if (Math.abs(cp) > 1E-6) {

            yaw = Math.atan2(
                    m.m02,
                    m.m22
            );

            roll = Math.atan2(
                    m.m10,
                    m.m11
            );

        } else {

            roll = 0;

            yaw = Math.atan2(
                    -m.m20,
                    m.m00
            );
        }

        return new VecYPR(
                0,
                0,
                0,
                (float) Math.toDegrees(yaw),
                (float) Math.toDegrees(pitch),
                (float) Math.toDegrees(roll),
                1
        );
    }

    /**
     * Converts to a rotation-only Matrix4.
     */
    public Matrix4 toMatrix() {

        Matrix4 mat = new Matrix4();

        mat.m00 = right.x;
        mat.m10 = right.y;
        mat.m20 = right.z;

        mat.m01 = up.x;
        mat.m11 = up.y;
        mat.m21 = up.z;

        mat.m02 = forward.x;
        mat.m12 = forward.y;
        mat.m22 = forward.z;

        return mat;
    }

    /**
     * For orthonormal matrices, the inverse is the transpose.
     */
    public Matrix4 toInverseMatrix() {
        Matrix4 mat = toMatrix();
        mat.transpose();
        return mat;
    }

    /**
     * Rotates the entire local basis around a world-space axis.
     *
     * The supplied axis is interpreted in global coordinates.
     * Every basis vector is rotated by the same amount, preserving
     * orthogonality.
     */
    public Orientation rotateWorld(Vec3d axis, double angle) {

        axis = axis.normalize();

        return new Orientation(
                rotateAroundAxis(forward, axis, angle),
                rotateAroundAxis(right, axis, angle),
                rotateAroundAxis(up, axis, angle)
        ).normalize();
    }

    /**
     * Rotates around one of the local axes.
     *
     * The local axis is expressed in orientation space:
     *
     * (1,0,0) -> local right
     * (0,1,0) -> local up
     * (0,0,1) -> local forward
     *
     * The corresponding world-space axis is first reconstructed, then
     * rotateWorld() is used internally.
     */
    public Orientation rotateLocal(Vec3d localAxis, double angle) {

        Vec3d axis =
                right.scale(localAxis.x)
                        .add(up.scale(localAxis.y))
                        .add(forward.scale(localAxis.z))
                        .normalize();

        return rotateWorld(axis, angle);
    }

    /**
     * Re-orthogonalizes the basis to prevent accumulated floating-point errors.
     */
    public Orientation normalize() {

        Vec3d f = forward.normalize();

        Vec3d r = right.subtract(
                f.scale(right.dotProduct(f))
        ).normalize();

        Vec3d u = f.crossProduct(r).normalize();

        return new Orientation(f, r, u);
    }

    /**
     * Rotates a vector around an arbitrary axis using Rodrigues'
     * rotation formula.
     *
     * The axis does not need to be normalized.
     */
    private static Vec3d rotateAroundAxis(Vec3d vec, Vec3d axis, double angle) {

        axis = axis.normalize();

        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        return vec.scale(cos)
                .add(axis.crossProduct(vec).scale(sin))
                .add(axis.scale(axis.dotProduct(vec) * (1.0 - cos)));
    }

    @Override
    public String toString() {
        return "Orientation{" +
                "forward=" + forward +
                ", right=" + right +
                ", up=" + up +
                '}';
    }

    /**
     * Rotates around the current local right axis.
     */
    public Orientation rotatePitch(double degree) {
        return rotateLocal(new Vec3d(1, 0, 0), Math.toRadians(degree));
    }

    /**
     * Rotates around the current local up axis.
     */
    public Orientation rotateYaw(double degree) {
        return rotateLocal(new Vec3d(0, 1, 0), Math.toRadians(degree));
    }

    /**
     * Rotates around the current local forward axis.
     */
    public Orientation rotateRoll(double degree) {
        return rotateLocal(new Vec3d(0, 0, 1), Math.toRadians(degree));
    }
}