package cam72cam.mod.tile;

import cam72cam.mod.util.TagCompound;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.World;

public class TileEntity extends net.minecraft.tileentity.TileEntity {
    public World world;
    public Vec3i pos;

    public void load(TagCompound data) {
    }

    public void setWorld(World world) {
    }
}
