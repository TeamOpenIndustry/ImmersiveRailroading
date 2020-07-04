package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.mod.block.BlockEntity;

public class BlockRail extends BlockRailBase {
	public BlockRail() {
		super("block_rail");
	}

	@Override
	public BlockEntity constructBlockEntity() {
		return new TileRail();
	}
}
