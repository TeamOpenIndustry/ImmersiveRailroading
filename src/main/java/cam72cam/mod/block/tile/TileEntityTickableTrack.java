package cam72cam.mod.block.tile;

import cam72cam.Mod;
import cam72cam.mod.block.BlockEntityTickableTrack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import trackapi.lib.ITrack;

public class TileEntityTickableTrack extends TileEntityTickable implements ITrack {

    public TileEntityTickableTrack() {
        super();
    }

    public TileEntityTickableTrack(Identifier id) {
        super(id);
    }

    @Override
    public double getTrackGauge() {
        return instance() instanceof BlockEntityTickableTrack ? ((BlockEntityTickableTrack)instance()).getTrackGauge() : 0;
    }

    @Override
    public net.minecraft.util.math.Vec3d getNextPosition(net.minecraft.util.math.Vec3d pos, net.minecraft.util.math.Vec3d mot) {
        return instance() instanceof BlockEntityTickableTrack ? ((BlockEntityTickableTrack)instance()).getNextPosition(new Vec3d(pos), new Vec3d(mot)).internal : pos;
    }

    @Override
    public Identifier getName() {
        return new Identifier(Mod.MODID, "tile_track");
    }
}
