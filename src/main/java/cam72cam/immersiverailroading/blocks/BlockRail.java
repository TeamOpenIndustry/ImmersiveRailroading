package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.Rail;
import cam72cam.mod.block.BlockSettings;

public class BlockRail extends BlockRailBase {
	public BlockRail() {
		super(new BlockSettings(ImmersiveRailroading.MODID, "block_rail"), Rail::new);
	}
}
