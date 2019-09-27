package cam72cam.immersiverailroading.blocks;

import cam72cam.mod.block.*;

import java.util.function.Supplier;

public abstract class BlockRailBase extends BlockTypeTickable {
	BlockRailBase(BlockSettings settings, Supplier<BlockEntityTickable> constructData) {
		super(settings
                .withConnectable(false)
                .withMaterial(Material.METAL)
                .withHardness(1F),
                constructData
        );
	}
}
