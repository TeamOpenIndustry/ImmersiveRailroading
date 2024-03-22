package com.goldenfield192.ire.util.graph;

import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import com.goldenfield192.ire.tiles.TileConnector;

import java.util.*;
import java.util.stream.Collectors;

public class GraphHandler {
    private static HashMap<World,DimGraph> wireMap;

    //初始化
    public static void init(){
        wireMap = new HashMap<>();
        //Though I know I shouldn't do it like this I don't know another way to implement this
        CommonEvents.World.LOAD.subscribe(w ->{
            World world = World.get(w);
            loadWorld(world);
        });
        CommonEvents.World.TICK.subscribe(w -> wireMap.get(World.get(w)).onTick());
    }

    //加载已有世界
    public static void loadWorld(World world){
        if(!wireMap.containsKey(world)){
            HashSet<Vec3i> cbeSet = world.getBlockEntities(TileConnector.class).stream()
                    .map(BlockEntity::getPos).collect(Collectors.toCollection(HashSet::new));
            DimGraph dimGraph = new DimGraph(cbeSet);
            dimGraph.buildExistedSubGraphs(world);
            wireMap.put(world,dimGraph);
        }
    }

    public static DimGraph getDimGraphByWorld(World world){
        if(!wireMap.containsKey(world)){
            loadWorld(world);
        }
        return wireMap.get(world);
    }
}
