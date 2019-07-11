package cam72cam.mod.block.tile;

import cam72cam.mod.block.BlockEntityTickable;
import cam72cam.mod.util.Identifier;
import net.minecraft.util.ITickable;

public class TileEntityTickable extends TileEntity implements ITickable {
    @Override
    public void update() {
        BlockEntityTickable tickable = (BlockEntityTickable) instance();
        tickable.update();
    }

    @Override
    public Identifier getName() {
        return new Identifier("notreallyamodthismightbreak", "hack_tickable");
    }
}
