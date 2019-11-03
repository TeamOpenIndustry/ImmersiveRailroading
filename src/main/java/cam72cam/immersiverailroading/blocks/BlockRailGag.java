package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileRailGag;
import cam72cam.mod.block.BlockSettings;

public class BlockRailGag extends BlockRailBase {
	public BlockRailGag() {
		super(new BlockSettings(ImmersiveRailroading.MODID, "block_rail_gag"), TileRailGag::new);
	}
}