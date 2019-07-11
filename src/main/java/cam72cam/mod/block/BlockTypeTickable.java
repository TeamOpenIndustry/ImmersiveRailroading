package cam72cam.mod.block;

import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.block.tile.TileEntityTickable;

import java.util.function.Function;

public class BlockTypeTickable extends BlockTypeEntity {
    public BlockTypeTickable(BlockSettings settings, Function<TileEntity, BlockEntity> constructData) {
        super(settings, constructData);
    }

    @Override
    protected TileEntity getTile() {
        return new TileEntityTickable(id);
    }
}
