package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.RailBase;
import cam72cam.mod.block.*;
import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.block.tile.TileEntityTickable;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import trackapi.lib.ITrack;

import java.util.function.Function;
import java.util.function.Supplier;

public abstract class BlockRailBase extends BlockTypeTickable {
	BlockRailBase(BlockSettings settings, Supplier<BlockEntityTickable> constructData) {
		super(settings
                .withConnectable(false)
                .withMaterial(Material.METAL)
                .withHardness(1F),
                constructData
        );
	}

    /*

    Custom Tile for ITrack

     */

    public static class TileEntityRailBlock extends TileEntityTickable implements ITrack {

        public TileEntityRailBlock() {
            super();
        }

        public TileEntityRailBlock(Identifier id) {
            super(id);
        }

        @Override
        public double getTrackGauge() {
            return instance() instanceof RailBase ? ((RailBase)instance()).getTrackGauge() : 0;
        }

        @Override
        public net.minecraft.util.math.Vec3d getNextPosition(net.minecraft.util.math.Vec3d pos, net.minecraft.util.math.Vec3d mot) {
            return instance() instanceof RailBase ? ((RailBase)instance()).getNextPosition(new Vec3d(pos), new Vec3d(mot)).internal : pos;
        }

        @Override
        public Identifier getName() {
            return new Identifier(ImmersiveRailroading.MODID, "tile_track");
        }
    }

    @Override
    public TileEntity getTile() {
        return new TileEntityRailBlock(id);
    }

}
