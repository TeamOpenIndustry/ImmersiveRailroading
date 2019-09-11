package cam72cam.mod.block;

import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.block.tile.TileEntityTickable;

import java.util.function.Supplier;

public class BlockTypeTickable extends BlockTypeEntity {
    public BlockTypeTickable(BlockSettings settings, Supplier<BlockEntityTickable> constructData) {
        super(settings, constructData::get);
    }

    @Override
    protected TileEntity getTile() {
        return new TileEntityTickable(id);
    }
}
