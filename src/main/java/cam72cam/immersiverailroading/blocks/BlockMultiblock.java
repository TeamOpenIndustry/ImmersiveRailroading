package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockSettings;
import cam72cam.mod.block.Material;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;

public class BlockMultiblock extends BlockEntity<TileMultiblock> {

	public BlockMultiblock() {
		super(new BlockSettings(ImmersiveRailroading.MODID, "multiblock")
				.withMaterial(Material.METAL)
				.withHardness(0.2F)
				, TileMultiblock::new
		);
	}

}
