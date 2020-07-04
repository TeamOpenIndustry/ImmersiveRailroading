package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.block.*;

import java.util.function.Supplier;

public abstract class BlockRailBase extends BlockTypeEntity {
	public BlockRailBase(String name) {
		super(ImmersiveRailroading.MODID, name);
	}

	@Override
	public Material getMaterial() {
		return Material.METAL;
	}

	@Override
	public float getHardness() {
		return 1;
	}

	@Override
	public boolean isConnectable() {
		return false;
	}
}
