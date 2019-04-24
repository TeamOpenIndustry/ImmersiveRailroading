package cam72cam.mod.tile;

import net.minecraft.util.math.Vec3d;

public interface ITrack extends trackapi.lib.ITrack {
    @Override
    default Vec3d getNextPosition(Vec3d currentPosition, Vec3d motion) {
        return getNextPosition(new cam72cam.mod.math.Vec3d(currentPosition), new cam72cam.mod.math.Vec3d(motion)).internal;
    }

    cam72cam.mod.math.Vec3d getNextPosition(cam72cam.mod.math.Vec3d currentPosition, cam72cam.mod.math.Vec3d motion);

}
