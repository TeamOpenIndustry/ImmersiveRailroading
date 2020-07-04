package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockTypeEntity;
import cam72cam.mod.block.Material;

public class BlockMultiblock extends BlockTypeEntity {
	@Override
	public String getModID() {
		return ImmersiveRailroading.MODID;
	}

	@Override
	public String getName() {
		return "multiblock";
	}

	@Override
	public Material getMaterial() {
		return Material.METAL;
	}

	@Override
	public float getHardness() {
		return 0.2f;
	}

	@Override
	public BlockEntity constructBlockEntity() {
		return new TileMultiblock();
	}
}
