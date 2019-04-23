package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.mod.block.BlockSettings;

public class BlockRail extends BlockRailBase {
	public BlockRail() {
		super(new BlockSettings(ImmersiveRailroading.MODID, "block_rail").withBlockEntity(TileRail::new));
	}

	@Override
	public void onBreak(TileRailBase te) {
		((TileRail) te).spawnDrops();
		super.onBreak(te);
	}
}
