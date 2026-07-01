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

    /**
     * If use this, YPR will be overridden !
     * */
    private final Orientation orientation;

    //Yaw
    public VecYPR(Vec3d orig, float yaw, TrackModelPart... parts) {//pitch roll length => default
        this(orig.x, orig.y, orig.z, yaw, parts);
    }

    public VecYPR(double xIn, double yIn, double zIn, float yaw, TrackModelPart... parts) {//pitch roll length => default
        this(xIn, yIn, zIn, yaw, 0, 0, null, -1, parts);
    }

    //Pitch
    public VecYPR(Vec3d orig, float yaw, float pitch, float roll, TrackModelPart... parts) {//length => default
        this(orig.x, orig.y, orig.z, yaw, pitch, roll, null, -1, parts);
    }

    //Length
    public VecYPR(VecYPR other, float length, TrackModelPart... parts) {
        this(other, other.yaw, other.pitch, other.roll, other.orientation, length, parts);
    }

    public VecYPR(Vec3d orig, float yaw, float pitch, float roll, Orientation orientation, float length, TrackModelPart... parts) {
        this(orig.x, orig.y, orig.z, yaw, pitch, roll, orientation, length, parts);
    }

    public VecYPR(double xIn, double yIn, double zIn, float yaw, float pitch, float length, TrackModelPart... parts) {//roll => default
        this(xIn, yIn, zIn, yaw, pitch, 0, null, length, parts);
    }

    //all
    /**
     * If use orientation, YPR will be overridden !
     * */
    public VecYPR(double xIn, double yIn, double zIn, float yaw, float pitch, float roll, Orientation orientation, float length, TrackModelPart... parts) {
        super(xIn, yIn, zIn);
        this.yaw = yaw;
        this.parts = Arrays.asList(parts);
        this.pitch = pitch;
        this.roll = roll;
        this.orientation = orientation;
        this.length = length;
        this.children = new ArrayList<>();
    }

    //roll
    public VecYPR(double xIn, double yIn, double zIn, float yaw, float pitch, float roll, float length, TrackModelPart... parts) {
        super(xIn, yIn, zIn);
        this.yaw = yaw;
        this.parts = Arrays.asList(parts);
        this.pitch = pitch;
        this.roll = roll;
        this.orientation = null;
        this.length = length;
        this.children = new ArrayList<>();
    }

    public VecYPR withYaw(float yaw) {//parts?
        return new VecYPR(this.x, this.y, this.z, yaw, this.pitch, this.roll, this.orientation, this.length);
    }

    public VecYPR withPitch(float pitch) {//parts?
        return new VecYPR(this.x, this.y, this.z, this.yaw, pitch, this.roll, this.orientation, this.length);
    }

    public VecYPR withRoll(float roll) {//parts?
        return new VecYPR(this.x, this.y, this.z, this.yaw, this.pitch, roll, this.orientation, this.length);
    }

    public VecYPR withOrientation(Orientation orientation) {
        return new VecYPR(this.x, this.y, this.z, this.yaw, this.pitch, this.roll, orientation, this.length);
    }

    @Override
    public VecYPR add(Vec3d other) {//roll => default
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

    public Orientation getOrientation() {
        return orientation;
    }
}
