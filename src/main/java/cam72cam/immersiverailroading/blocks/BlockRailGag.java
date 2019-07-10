package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.RailGagInstance;
import cam72cam.mod.block.BlockSettings;

public class BlockRailGag extends BlockRailBase<RailGagInstance> {
	public BlockRailGag() {
		super(new BlockSettings(ImmersiveRailroading.MODID, "block_rail_gag"), RailGagInstance::new);
	}
}