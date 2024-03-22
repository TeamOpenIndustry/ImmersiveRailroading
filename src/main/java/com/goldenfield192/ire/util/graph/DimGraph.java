package com.goldenfield192.ire.util.graph;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.mod.energy.Energy;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;
import com.goldenfield192.ire.tiles.TileConnector;
import com.goldenfield192.ire.util.IREEnergy;

import java.util.*;

public class DimGraph {
    public static final SubGraph EMPTY_SUB_GRAPH = new SubGraph();

    private final HashSet<Vec3i> dimNetworkSet;

    private final HashMap<UUID, SubGraph> graphInDim;

    private long ticks = 0;

    public HashMap<UUID, SubGraph> getGraphInDim() {
        return graphInDim;
    }

    public SubGraph getSubGraphByUUID(UUID uuid) {
        return graphInDim.getOrDefault(uuid, DimGraph.EMPTY_SUB_GRAPH);
    }

    public DimGraph(HashSet<Vec3i> dimNetworkSet) {
        this.dimNetworkSet = dimNetworkSet;
        graphInDim = new HashMap<>();
    }

    public void remove(UUID uuid, Vec3i pos) {
        if(graphInDim.get(uuid) != null){
            if (graphInDim.get(uuid).subGraphSet.size() == 1) {
                graphInDim.remove(uuid, graphInDim.get(uuid));
            } else {
                graphInDim.get(uuid).subGraphSet.remove(pos);
            }
        }
    }

    public void merge(UUID base, UUID target, World world) {
        SubGraph t = graphInDim.get(target);
        graphInDim.get(base).subGraphSet.forEach(vec3i -> {
            t.add(vec3i);
            world.getBlockEntity(vec3i, TileConnector.class).setSubGraphID(target);
        });
        graphInDim.remove(base);
    }

    public void split(UUID base, TileConnector tc, World world) {
        SubGraph b = graphInDim.get(base);
        HashSet<TileConnector> iterated = new HashSet<>();
        iterated.add(tc);
        b.subGraphSet.forEach(vec3i -> {
            TileConnector tc1 = world.getBlockEntity(vec3i, TileConnector.class);
            if(tc1 != null && !iterated.contains(tc1)){
                UUID current = UUID.randomUUID();
                this.graphInDim.put(current, new SubGraph());
                dfs(tc1,iterated,world,current);
            }
        });
        graphInDim.remove(base,b);
    }

    private void dfs(TileConnector tc, HashSet<TileConnector> iterated, World world,UUID uuid){
        iterated.add(tc);
        tc.setSubGraphID(uuid);
        this.graphInDim.get(uuid).add(tc.getPos());
        tc.getConnection().keySet().stream().map(v -> world.getBlockEntity(tc.getPos().add(v),TileConnector.class))
                .forEach(tileConnector -> {
                    if(!iterated.contains(tileConnector)){
                        dfs(tileConnector,iterated,world,uuid);
                    }
                });
    }
    
    public void buildExistedSubGraphs(World world) {
        this.dimNetworkSet.forEach(vec3i -> {
            TileConnector tc = world.getBlockEntity(vec3i, TileConnector.class);
            if (tc != null){
                if(!graphInDim.containsKey(tc.getSubGraphID()))
                    addSubGraph(tc.getPos(),tc.getSubGraphID());
                graphInDim.get(tc.getSubGraphID()).add(tc);
            }
        });
    }

    public void addSubGraph(Vec3i pos, UUID uuid) {
        this.graphInDim.put(uuid, new SubGraph(pos));
    }

    public void onTick(){
        ticks++;
        this.graphInDim.values().forEach(subGraph -> subGraph.energy.update(ticks));
    }

    public static class SubGraph {
        public HashSet<Vec3i> subGraphSet;
        public HashSet<EntityRollingStock> stocks;

        @TagField("energy")
        public IREEnergy energy;

        public SubGraph(Vec3i... vec3is) {
            subGraphSet = new HashSet<>();
            energy = new IREEnergy();
            if (vec3is != null) {
                subGraphSet.addAll(Arrays.asList(vec3is));
            }
        }

        public void add(TileConnector tc) {
            this.add(tc.getPos());
        }

        public void add(Vec3i pos) {
            this.subGraphSet.add(pos);
        }
    }
}
