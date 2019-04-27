package cam72cam.mod.tile;

import cam72cam.mod.util.TagCompound;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.World;
import net.minecraft.nbt.NBTTagCompound;

public class TileEntity extends net.minecraft.tileentity.TileEntity {
    public World world;
    public Vec3i pos;


    public final void readFromNBT(NBTTagCompound compound) {
        load(new TagCompound(compound));
    }
    public final NBTTagCompound writeToNBT(NBTTagCompound compound) {
        save(new TagCompound(compound));
        return compound;
    }

    public void load(TagCompound data) {
        super.readFromNBT(data.internal);
    }
    public void save(TagCompound data) {
        super.writeToNBT(data.internal);
    }

    public void setWorld(World world) {
        super.world = world.internal;
        this.world = world;
    }
    public void setPos(Vec3i pos) {
        super.setPos(pos.internal);
    }
}
