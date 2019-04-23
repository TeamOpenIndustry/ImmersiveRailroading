package cam72cam.mod.math;

import cam72cam.mod.util.Facing;
import net.minecraft.util.math.BlockPos;

public class Vec3i {
    public final BlockPos internal;
    public final int x;
    public final int y;
    public final int z;

    public Vec3i(BlockPos pos) {
        this.internal = pos;

        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
    }

    public Vec3i offset(Facing facing, int offset) {
        return new Vec3i(internal.offset(facing.internal, offset));
    }

    public Vec3i offset(Facing facing) {
        return offset(facing, 1);
    }

    public Vec3i up() {
        return offset(Facing.UP);
    }
    public Vec3i down() {
        return offset(Facing.DOWN);
    }
    public Vec3i north() {
        return offset(Facing.NORTH);
    }
    public Vec3i east() {
        return offset(Facing.EAST);
    }
    public Vec3i south() {
        return offset(Facing.SOUTH);
    }
    public Vec3i west() {
        return offset(Facing.WEST);
    }
    public Vec3i up(int offset) {
        return offset(Facing.UP, offset);
    }
    public Vec3i down(int offset) {
        return offset(Facing.DOWN, offset);
    }
    public Vec3i north(int offset) {
        return offset(Facing.NORTH, offset);
    }
    public Vec3i east(int offset) {
        return offset(Facing.EAST, offset);
    }
    public Vec3i south(int offset) {
        return offset(Facing.SOUTH, offset);
    }
    public Vec3i west(int offset) {
        return offset(Facing.WEST, offset);
    }

    public Vec3i add(Vec3i other) {
        return new Vec3i(internal.add(other.internal));
    }
    public Vec3i subtract(Vec3i other) {
        return new Vec3i(internal.subtract(other.internal));
    }
}
