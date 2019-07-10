package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.RailInstance;
import cam72cam.mod.block.BlockSettings;

public class BlockRail extends BlockRailBase<RailInstance> {
	public BlockRail() {
		super(new BlockSettings(ImmersiveRailroading.MODID, "block_rail"), RailInstance::new);
	}

	@Override
	public Internal getTile() {
		return new RailBlockEntityInternal() {

		};
	}
}
