package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.library.TrackModelPart;
import cam72cam.mod.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VecYPR extends Vec3d {
    private final float yaw;
    private final float pitch;
    private final float roll;
    private final float length;
    private final List<TrackModelPart> parts;
    private final List<VecYPR> children;

    //Yaw
    public VecYPR(Vec3d orig, float yaw, TrackModelPart... parts) {
        this(orig.x, orig.y, orig.z, yaw, parts);
    }

    public VecYPR(double xIn, double yIn, double zIn, float yaw, TrackModelPart... parts) {
        this(xIn, yIn, zIn, yaw, 0, parts);
    }

    //Pitch
    public VecYPR(Vec3d orig, float yaw, float pitch, TrackModelPart... parts) {
        this(orig.x, orig.y, orig.z, yaw, pitch, -1, parts);
    }

    public VecYPR(double xIn, double yIn, double zIn, float yaw, float pitch, TrackModelPart... parts) {
        this(xIn, yIn, zIn, yaw, pitch, -1, parts);
    }

    //Length
    public VecYPR(VecYPR other, float length, TrackModelPart... parts) {
        this(other, other.yaw, other.pitch, length, parts);
    }

    public VecYPR(Vec3d orig, float yaw, float pitch, float length, TrackModelPart... parts) {
        this(orig.x, orig.y, orig.z, yaw, pitch, length, parts);
    }

    public VecYPR(double xIn, double yIn, double zIn, float yaw, float pitch, float length, TrackModelPart... parts) {
        this(xIn, yIn, zIn, yaw, pitch, 0, length, parts);
    }

    //Roll
    public VecYPR(double xIn, double yIn, double zIn, float yaw, float pitch, float roll, float length, TrackModelPart... parts) {
        super(xIn, yIn, zIn);
        this.yaw = yaw;
        this.parts = Arrays.asList(parts);
        this.pitch = pitch;
        this.roll = roll;
        this.length = length;
        this.children = new ArrayList<>();
    }

    @Override
    public VecYPR add(Vec3d other) {
        return new VecYPR(this.x + other.x, this.y + other.y, this.z + other.z, this.yaw, this.pitch, this.length);
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
}
