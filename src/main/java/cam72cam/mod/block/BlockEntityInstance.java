package cam72cam.mod.block;

import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.util.TagCompound;
import cam72cam.mod.world.World;

public abstract class BlockEntityInstance {
    public final BlockEntity.Internal internal;
    public final World world;
    public final Vec3i pos;

    public BlockEntityInstance(BlockEntity.Internal internal) {
        this.internal = internal;
        this.world = internal.world;
        this.pos = internal.pos;
    }

    public static abstract class Tickable extends BlockEntityInstance {
        public Tickable(BlockEntity.Internal internal) {
            super(internal);
        }

        public abstract void update();
    }

    public abstract void load(TagCompound nbt);
    public abstract void save(TagCompound nbt);
    public abstract void writeUpdate(TagCompound nbt);
    public abstract void readUpdate(TagCompound nbt);


    public abstract void onBreak();
    public abstract boolean onClick(Player player, Hand hand, Facing facing, Vec3d hit);
    public abstract ItemStack onPick();
    public abstract void onNeighborChange(Vec3i neighbor);

    public double getHeight() {
        return 1;
    }

    public void markDirty() {
        internal.markDirty();
    }
}
