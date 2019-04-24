package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.block.BlockEntityBase;
import cam72cam.mod.block.BlockSettings;
import cam72cam.mod.block.Material;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;

public class BlockMultiblock extends BlockEntityBase<TileMultiblock> {

	public BlockMultiblock() {
		super(new BlockSettings(ImmersiveRailroading.MODID, "multiblock")
				.withMaterial(Material.METAL)
				.withHardness(0.2F)
				.withBlockEntity(TileMultiblock::new)
		);
	}

	@Override
	public void onBreak(TileMultiblock te) {
        try {
            // Multiblock break
            te.breakBlock();
        } catch (Exception ex) {
            ImmersiveRailroading.catching(ex);
            // Something broke
            // TODO figure out why
            te.world.setToAir(te.pos);
        }
	}

	@Override
	public boolean onClick(TileMultiblock te, Player player, Hand hand, Facing facing, Vec3d hit) {
        return te.onBlockActivated(player, hand);
	}

	@Override
	public ItemStack onPick(TileMultiblock te) {
		return ItemStack.EMPTY;
	}

	@Override
	public void onNeighborChange(TileMultiblock entity, Vec3i neighbor) {
	}
}
