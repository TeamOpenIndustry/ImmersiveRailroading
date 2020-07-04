package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockTypeEntity;
import cam72cam.mod.block.Material;

public class BlockRailPreview extends BlockTypeEntity {
	public BlockRailPreview() {
		super(ImmersiveRailroading.MODID, "block_rail_preview");
	}

	@Override
	public Material getMaterial() {
		return Material.WOOL;
	}

	@Override
	public float getHardness() {
		return 0.2f;
	}

	@Override
	public float getExplosionResistance() {
		return 2000;
	}

	@Override
	public boolean isConnectable() {
		return super.isConnectable();
	}

	@Override
	public BlockEntity constructBlockEntity() {
		return new TileRailPreview();
	}
}
