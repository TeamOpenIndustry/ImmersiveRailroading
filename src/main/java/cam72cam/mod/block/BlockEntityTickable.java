package cam72cam.mod.block;

import cam72cam.mod.block.tile.TileEntity;

public abstract class BlockEntityTickable extends BlockEntity {
    public BlockEntityTickable(TileEntity internal) {
        super(internal);
    }

    public abstract void update();
}
