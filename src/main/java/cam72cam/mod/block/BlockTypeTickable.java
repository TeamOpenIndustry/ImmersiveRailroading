package cam72cam.mod.block;

import java.util.function.Supplier;

public class BlockTypeTickable extends BlockTypeEntity {
    public BlockTypeTickable(BlockSettings settings, Supplier<BlockEntityTickable> constructData) {
        super(settings, constructData::get);
    }
}
