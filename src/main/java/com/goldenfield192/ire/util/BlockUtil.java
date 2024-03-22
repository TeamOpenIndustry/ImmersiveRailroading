package com.goldenfield192.ire.util;

import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import com.goldenfield192.ire.tiles.TileConnector;

import java.util.HashSet;

import static com.goldenfield192.ire.util.MathUtil.*;
import static java.lang.Math.*;

public class BlockUtil {
    public static void vecToBlocks(TileConnector from, TileConnector to, TileRail block){
        World world = from.getWorld();
        Vec3i startPos = toVec3i(toVec3d(from.getPos()).add(from.inBlockOffset.rotateYaw(from.getRotation())));
        Vec3i endPos = toVec3i(toVec3d(to.getPos()).add(to.inBlockOffset.rotateYaw(to.getRotation())));
        HashSet<Vec3i> iterated = new HashSet<>();
        System.out.println(startPos+" "+endPos);

        int maxY = max(startPos.y,endPos.y);
        double radian = Math.toRadians(vecToDegreeXZ(toVec3d(endPos.subtract(startPos))));
        System.out.println(radian);
        double limit = vecToLength(startPos.subtract(endPos));
        for(float iteration = 0;iteration < limit;iteration+=0.1){
            Vec3i vec3i = new Vec3i(Math.floor(iteration * sin(radian)),0,Math.floor(iteration * cos(radian)));
            if(iterated.add(vec3i)) {
                for(int j = 0; j<=8;j++){
                    Vec3i storage = startPos.add(vec3i).subtract(0, j, 0);
                    ModCore.info(storage.toString());
                    if(world.getBlockEntity(storage, TileRail.class) != null) {
                        IRETileRail ireTileRail = new IRETileRail(world.getBlockEntity(storage, TileRail.class));
                        world.setBlockEntity(storage, ireTileRail);
                        ModCore.info("Succeed %s",j);
                    }
                }
            }
        }
    }
}
