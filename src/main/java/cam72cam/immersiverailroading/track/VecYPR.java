package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.library.TrackModelPart;
import cam72cam.mod.math.Matrix3;
import cam72cam.mod.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VecYPR extends Vec3d {
    private final float yaw;
    private final float pitch;
    private final float roll;
    private final Matrix3 matrixCache;
    private final float length;
    private final List<TrackModelPart> parts;
    private final List<VecYPR> children;

    public VecYPR(double xIn, double yIn, double zIn, float yaw, float pitch, float roll, float length, TrackModelPart... parts) {//all
        super(xIn, yIn, zIn);
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
        this.matrixCache = Matrix3.fromEuler(yaw, pitch, roll);

        this.parts = Arrays.asList(parts);
        this.length = length;
        this.children = new ArrayList<>();
    }

    //Yaw
    public VecYPR(Vec3d orig, float yaw, TrackModelPart... parts) {//pitch roll length => default
        this(orig.x, orig.y, orig.z, yaw, 0, 0, -1, parts);
    }

    public VecYPR(double xIn, double yIn, double zIn, float yaw, TrackModelPart... parts) {//pitch roll length => default
        this(xIn, yIn, zIn, yaw, 0, 0, -1, parts);
    }

    //Pitch & Roll
    public VecYPR(Vec3d orig, float yaw, float pitch, float roll, TrackModelPart... parts) {//length => default
        this(orig.x, orig.y, orig.z, yaw, pitch, roll, -1, parts);
    }

    public VecYPR(double xIn, double yIn, double zIn, float yaw, float pitch, float roll, TrackModelPart... parts) {//length => default
        this(xIn, yIn, zIn, yaw, pitch, roll, -1, parts);
    }

    //Length
    public VecYPR(VecYPR other, float length, TrackModelPart... parts) {// all
        this(other, other.yaw, other.pitch, other.roll, length, parts);
    }

    public VecYPR(Vec3d orig, float yaw, float pitch, float roll, float length, TrackModelPart... parts) {//all
        this(orig.x, orig.y, orig.z, yaw, pitch, roll, length, parts);
    }

    public VecYPR withMatrix3(Matrix3 matrix3) {
        Vec3d ypr = matrix3.toEuler();
        return copyWith(this.x, this.y, this.z, (float) ypr.x, (float) ypr.y, (float) ypr.z);
    }

    @Override
    public VecYPR add(Vec3d other) {
        return copyWith(this.x + other.x, this.y + other.y, this.z + other.z, this.yaw, this.pitch, this.roll);
    }

    public void addChild(VecYPR another) {
        this.children.add(another);
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public float getRoll() {
        return roll;
    }

    public float getLength() {
        return this.length;
    }

    public List<TrackModelPart> getParts() {
        return this.parts;
    }

    public List<VecYPR> getChildren() {
        return children;
    }

    public Matrix3 toMatrix3() {
        return matrixCache.copy();
    }

    private VecYPR copyWith(double x, double y, double z, float yaw, float pitch, float roll) {
        //Preserve other data and override position/orientation
        VecYPR result = new VecYPR(x, y, z, yaw, pitch, roll, this.length, this.parts.toArray(new TrackModelPart[0]));
        result.children.addAll(this.children);
        return result;
    }

    @Override
    public String toString() {
        return String.format("Pos:(%s, %s, %s), YPR:(%s, %s, %s)", this.x, this.y, this.z, this.yaw, this.pitch, this.roll);
    }
}
