package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.RailBaseInstance;
import cam72cam.immersiverailroading.tile.RailGagInstance;
import cam72cam.immersiverailroading.tile.RailInstance;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockSettings;
import cam72cam.mod.block.IBreakCancelable;
import cam72cam.mod.block.Material;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.TagCompound;
import cam72cam.mod.world.World;
import trackapi.lib.ITrack;

import java.util.function.Function;

public abstract class BlockRailBase<T extends RailBaseInstance> extends BlockEntity<T> {
	public BlockRailBase(BlockSettings settings, Function<Internal, T> constructData) {
		super(settings
                .withConnectable(false)
                .withMaterial(Material.METAL)
                .withHardness(1F),
                constructData
        );
	}

	/*

	Custom Block for IBreakCancellable

	 */

    protected class RailBlockInternal extends BlockEntityInternal implements IBreakCancelable {
        @Override
        public boolean tryBreak(World world, Vec3i pos, Player player) {
            return BlockRailBase.this.tryBreak(world, pos, player);
        }
    }

    @Override
    protected BlockInternal getBlock() {
        return new RailBlockInternal();
    }

    /*

    Custom Tile for ITrack

     */

    protected class RailBlockEntityInternal extends Internal implements ITrack {

        @Override
        public double getTrackGauge() {
            return instance() instanceof RailBaseInstance ? ((RailBaseInstance)instance()).getTrackGauge() : 0;
        }

        @Override
        public net.minecraft.util.math.Vec3d getNextPosition(net.minecraft.util.math.Vec3d pos, net.minecraft.util.math.Vec3d mot) {
            return instance() instanceof RailBaseInstance ? ((RailBaseInstance)instance()).getNextPosition(new Vec3d(pos), new Vec3d(mot)).internal : pos;
        }
    }

    @Override
    protected Internal getTile() {
        return new RailBlockEntityInternal();
    }

    /*

    Helpers

     */


	public static void breakParentIfExists(RailBaseInstance te) {
		RailInstance parent = te.getParentTile();
		if (parent != null && !te.getWillBeReplaced()) {
            parent.spawnDrops();
            //if (tryBreak(te.getWorld(), te.getPos())) {
            te.world.setToAir(parent.pos);
            //}
		}
	}

    public boolean tryBreak(World world, Vec3i pos, Player player) {
        try {
            RailBaseInstance rail = world.getBlockEntity(pos, RailBaseInstance.class);
            if (rail != null) {
                if (rail.getReplaced() != null) {
                    // new object here is important
                    RailGagInstance newGag = new RailGagInstance();
                    newGag.load(rail.getReplaced());
                    while(true) {
                        if (newGag.getParent() != null && world.hasBlockEntity(newGag.getParent(), RailInstance.class)) {
                            rail.world.setTileEntity(pos, newGag);
                            newGag.markDirty();
                            breakParentIfExists(rail);
                            return false;
                        }
                        // Only do replacement if parent still exists

                        TagCompound data = newGag.getReplaced();
                        if (data == null) {
                            break;
                        }

                        newGag = new RailGagInstance();
                        newGag.load(data);
                    }
                }
            }
        } catch (StackOverflowError ex) {
            ImmersiveRailroading.error("Invalid recursive rail block at %s", pos);
            ImmersiveRailroading.catching(ex);
            world.setToAir(pos);
        }
        return true;
    }
}
