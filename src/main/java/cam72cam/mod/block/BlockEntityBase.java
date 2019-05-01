package cam72cam.mod.block;

import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.tile.TileEntity;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.world.World;

public abstract class BlockEntityBase<T extends TileEntity> extends BlockBase {
    private final Class<? extends TileEntity> cls;

    public BlockEntityBase(BlockSettings settings) {
        super(settings);
        cls = settings.entity.get().getClass();
    }

    @Override
    public final void onBreak(World world, Vec3i pos) {
        T te = (T) world.getTileEntity(pos, cls);
        if (te != null) {
            onBreak(te);
        }
    }
    public abstract void onBreak(T entity);

    @Override
    public final boolean onClick(World world, Vec3i pos, Player player, Hand hand, Facing facing, Vec3d hit) {
        T te = (T) world.getTileEntity(pos, cls);
        if (te != null) {
            return onClick(te, player, hand, facing, hit);
        }
        return false;
    }
    public abstract boolean onClick(T entity, Player player, Hand hand, Facing facing, Vec3d hit);

    @Override
    public final ItemStack onPick(World world, Vec3i pos) {
        T te = (T) world.getTileEntity(pos, cls);
        if (te != null) {
            return onPick(te);
        }
        return ItemStack.EMPTY;
    }
    public abstract ItemStack onPick(T entity);

    @Override
    public final void onNeighborChange(World world, Vec3i pos, Vec3i neighbor) {
        T te = (T) world.getTileEntity(pos, cls, false);
        if (te != null) {
            onNeighborChange(te, neighbor);
        }
    }
    public abstract void onNeighborChange(T entity, Vec3i neighbor);

    @Override
    public final double getHeight(World world, Vec3i pos) {
        T te = (T) world.getTileEntity(pos, cls, false);
        if (te != null) {
            getHeight(te);
        }
        return 1;
    }

    public double getHeight(T entity) {
        return 1;
    }

}
