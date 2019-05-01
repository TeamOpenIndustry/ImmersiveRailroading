package cam72cam.mod.tile;

import cam72cam.mod.util.TagCompound;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public class TileEntity extends net.minecraft.tileentity.TileEntity {
    public World world;
    public Vec3i pos;

    @Override
    public final void readFromNBT(NBTTagCompound compound) {
        load(new TagCompound(compound));
    }
    @Override
    public final NBTTagCompound writeToNBT(NBTTagCompound compound) {
        save(new TagCompound(compound));
        return compound;
    }

    public void load(TagCompound data) {
        super.readFromNBT(data.internal);
        pos = new Vec3i(super.pos);
    }
    public void save(TagCompound data) {
        super.writeToNBT(data.internal);
    }

    @Override
    public void setWorld(net.minecraft.world.World world) {
        super.setWorld(world);
        this.world = World.get(world);
    }
    public void setWorld(World world) {
        setWorld(world.internal);
    }
    @Override
    public void setPos(BlockPos pos) {
        this.pos = new Vec3i(pos);
        super.setPos(pos);
    }
    public void setPos(Vec3i pos) {
        super.setPos(pos.internal);
    }
}
