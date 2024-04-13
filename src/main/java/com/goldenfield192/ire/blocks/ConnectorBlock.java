package com.goldenfield192.ire.blocks;

import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockTypeEntity;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;
import com.goldenfield192.ire.tiles.TileConnector;

public class ConnectorBlock extends BlockTypeEntity {
    private Facing facing = Facing.WEST;

    public void setFacing(Facing facing) {
        this.facing = facing;
    }

    public ConnectorBlock(String modID, String name) {
        super(modID, name);
    }

    @Override
    protected BlockEntity constructBlockEntity() {
        return new TileConnector((this.facing == null ? Facing.SOUTH : this.facing).toString());
    }

    @Override
    public IBoundingBox getBoundingBox(World world, Vec3i pos) {
        return IBoundingBox.from(new Vec3d(0.25,0,0.25),new Vec3d(0.75,0.75,0.75));
    }

    @Override
    public boolean isConnectable() { //栅栏和玻璃板
        return false;
    }
}
