package cam72cam.mod.block;

import cam72cam.mod.entity.Player;
import cam72cam.mod.world.World;
import cam72cam.mod.math.Vec3i;

public interface IBreakCancelable {
    boolean tryBreak(World world, Vec3i pos, Player player);
}
