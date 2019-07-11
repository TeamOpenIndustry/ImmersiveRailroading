package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.block.BlockTypeEntity;
import cam72cam.mod.block.BlockSettings;
import cam72cam.mod.block.Material;

public class BlockMultiblock extends BlockTypeEntity {

	public BlockMultiblock() {
		super(new BlockSettings(ImmersiveRailroading.MODID, "multiblock")
				.withMaterial(Material.METAL)
				.withHardness(0.2F)
				, TileMultiblock::new
		);
	}

}
