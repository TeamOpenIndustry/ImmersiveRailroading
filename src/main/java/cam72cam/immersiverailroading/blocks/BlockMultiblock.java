package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.umc.api.block.BlockEntity;
import cam72cam.umc.api.block.BlockTypeEntity;
import cam72cam.umc.api.block.Material;

public class BlockMultiblock extends BlockTypeEntity {
	public BlockMultiblock() {
		super(ImmersiveRailroading.MODID, "multiblock");
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
