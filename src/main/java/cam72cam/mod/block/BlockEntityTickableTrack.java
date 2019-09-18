package cam72cam.mod.block;


import cam72cam.mod.math.Vec3d;

public abstract class BlockEntityTickableTrack extends BlockEntityTickable {
    public abstract double getTrackGauge();

    public abstract Vec3d getNextPosition(Vec3d vec3d, Vec3d vec3d1);
}
