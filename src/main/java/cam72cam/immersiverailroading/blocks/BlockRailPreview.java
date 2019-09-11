package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.mod.block.BlockSettings;
import cam72cam.mod.block.BlockTypeTickable;
import cam72cam.mod.block.Material;

public class BlockRailPreview extends BlockTypeTickable {
	public BlockRailPreview() {
		super(new BlockSettings(ImmersiveRailroading.MODID, "block_rail_preview")
				.withMaterial(Material.WOOL)
				.withHardness(0.2F)
				.withExplosionResistance(2000)
				.withConnectable(false)
		, TileRailPreview::new);
	}
}
