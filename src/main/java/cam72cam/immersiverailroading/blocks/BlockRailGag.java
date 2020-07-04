package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.tile.TileRailGag;
import cam72cam.mod.block.BlockEntity;

public class BlockRailGag extends BlockRailBase {
	public BlockRailGag() {
		super("block_rail_gag");
	}

	@Override
	public BlockEntity constructBlockEntity() {
		return new TileRailGag();
	}
}