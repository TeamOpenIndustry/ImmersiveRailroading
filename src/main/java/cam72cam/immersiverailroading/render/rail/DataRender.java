package cam72cam.immersiverailroading.render.rail;

import cam72cam.immersiverailroading.data.Region;
import cam72cam.immersiverailroading.data.TrackInfo;
import cam72cam.immersiverailroading.data.WorldData;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.model.TrackModel;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.VBO;
import cam72cam.mod.world.World;
import org.lwjgl.opengl.GL11;
import util.Matrix4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataRender {
    // TODO per-world
    // TODO Expire
    private static final Map<Region, RegionRender> regions = new HashMap<>();
    public static class RegionRender {
        boolean dirty = true;
        private final Map<String, VBO> cache = new HashMap<>();
        private final Region region;

        public RegionRender(Region r) {
            this.region = r;
        }

        public void render(RenderState state) {
            if (dirty) {
                // TODO threaded loading...
                // TODO this map could be cached
                Map<String, List<RailInfo>> byModel = new HashMap<>();
                for (TrackInfo info : region.getTrackInfos()) {
                    RailInfo rail = (info.settings.type == TrackItems.SWITCH ? info.withSettings(b -> b.type = TrackItems.STRAIGHT) : info);
                    byModel.computeIfAbsent(info.settings.track, k -> new ArrayList<>()).add(rail);
                }

                for (Map.Entry<String, List<RailInfo>> entry : byModel.entrySet()) {

                    // TODO Gauge
                    TrackModel model = DefinitionManager.getTrack(entry.getKey(), Gauge.STANDARD);
                    if (model == null) {
                        return;
                    }

                    OBJRender.Builder renderBuilder = model.binder().builder();
                    for (RailInfo info : entry.getValue()) {
                        BuilderBase builder = info.getBuilder(MinecraftClient.getPlayer().getWorld());
                        List<BuilderBase.VecYawPitch> renderData = builder.getRenderData();
                        for (BuilderBase.VecYawPitch piece : renderData) {
                            Matrix4 m = new Matrix4();
                            m.translate(info.placementInfo.placementPosition.x, info.placementInfo.placementPosition.y, info.placementInfo.placementPosition.z);
                            //m.rotate(Math.toRadians(info.placementInfo.yaw), 0, 1, 0);
                            m.translate(piece.x, piece.y, piece.z);
                            m.rotate(Math.toRadians(piece.getYaw()), 0, 1, 0);
                            m.rotate(Math.toRadians(piece.getPitch()), 1, 0, 0);
                            m.rotate(Math.toRadians(-90), 0, 1, 0);

                            if (piece.getLength() != -1) {
                                m.scale(piece.getLength() / info.settings.gauge.scale(), 1, 1);
                            }
                            double scale = info.settings.gauge.scale();
                            m.scale(scale, scale, scale);

                            if (piece.getGroups().size() != 0) {
                                List<String> groups = model.groups().stream()
                                        .filter(group -> piece.getGroups().stream().anyMatch(group::contains))
                                        .collect(Collectors.toList());
                                renderBuilder.draw(groups, m);
                            } else {
                                renderBuilder.draw(m);
                            }
                        }
                    }
                    cache.put(entry.getKey(), renderBuilder.build());
                }
                dirty = false;
            }

            for (Map.Entry<String, VBO> entry : cache.entrySet()) {
                try(VBO.Binding restore = entry.getValue().bind(state)) {
                    restore.draw();
                }
            }
        }
    }

    private static final Map<Region, RegionData> regionData = new HashMap<>();

    public static class RegionData {
        Map<String, List<TrackRenderData>> typeToData = new HashMap<>();
        Map<String, Integer> displayLists = new HashMap<>();

        public RegionData(Region region) {
            World world = MinecraftClient.getPlayer().getWorld();
            Map<String, List<RailInfo>> byModel = new HashMap<>();
            for (TrackInfo info : region.getTrackInfos()) {
                RailInfo rail = (info.settings.type == TrackItems.SWITCH ? info.withSettings(b -> b.type = TrackItems.STRAIGHT) : info);
                byModel.computeIfAbsent(info.settings.track, k -> new ArrayList<>()).add(rail);
            }

            for (Map.Entry<String, List<RailInfo>> entry : byModel.entrySet()) {
                List<TrackRenderData> renderBuilder = new ArrayList<>();

                TrackModel model = DefinitionManager.getTrack(entry.getKey(), Gauge.STANDARD);
                if (model == null) {
                    continue;
                }

                for (RailInfo info : entry.getValue()) {
                    BuilderBase builder = info.getBuilder(world);
                    List<BuilderBase.VecYawPitch> renderData = builder.getRenderData();
                    for (BuilderBase.VecYawPitch piece : renderData) {
                        Matrix4 m = new Matrix4();
                        Vec3d off = piece.add(info.placementInfo.placementPosition);
                        //m.rotate(Math.toRadians(info.placementInfo.yaw), 0, 1, 0);
                        m.translate(off.x, off.y, off.z);
                        //m.translate(piece.x, piece.y, piece.z);
                        m.rotate(Math.toRadians(piece.getYaw()), 0, 1, 0);
                        m.rotate(Math.toRadians(piece.getPitch()), 1, 0, 0);
                        m.rotate(Math.toRadians(-90), 0, 1, 0);

                        if (piece.getLength() != -1) {
                            m.scale(piece.getLength() / info.settings.gauge.scale(), 1, 1);
                        }
                        double scale = info.settings.gauge.scale();
                        m.scale(scale, scale, scale);

                        if (piece.getGroups().size() != 0) {
                            List<String> groups = model.groups().stream()
                                    .filter(group -> piece.getGroups().stream().anyMatch(group::contains))
                                    .collect(Collectors.toList());
                            renderBuilder.add(new TrackRenderData(m, groups, world.getBlockLightLevel(new Vec3i(off)), world.getSkyLightLevel(new Vec3i(off))));
                        } else {
                            renderBuilder.add(new TrackRenderData(m, null, world.getBlockLightLevel(new Vec3i(off)), world.getSkyLightLevel(new Vec3i(off))));
                        }
                    }
                }
                typeToData.put(entry.getKey(), renderBuilder);
            }
        }

        public void render(RenderState state) {
            for (Map.Entry<String, List<TrackRenderData>> entry : typeToData.entrySet()) {

                TrackModel model = DefinitionManager.getTrack(entry.getKey(), Gauge.STANDARD);
                if (model == null) {
                    continue;
                }
                try (OBJRender.Binding bound = model.binder().bind(state)) {
                    if (displayLists.containsKey(entry.getKey())) {
                        GL11.glCallList(displayLists.get(entry.getKey()));
                        continue;
                    }

                    int dlid = GL11.glGenLists(1);
                    GL11.glNewList(dlid, GL11.GL_COMPILE);

                    for (TrackRenderData data : entry.getValue()) {
                        if (data.groups == null) {
                            bound.draw(s -> {
                                s.model_view().multiply(data.m);
                                s.lightmap(data.bll, data.sll);
                            });
                        } else {
                            bound.draw(data.groups, s -> {
                                s.model_view().multiply(data.m);
                                s.lightmap(data.bll, data.sll);
                            });
                        }
                    }

                    GL11.glEndList();
                    displayLists.put(entry.getKey(), dlid);
                }
            }
        }
    }
    public static class TrackRenderData {
        Matrix4 m;
        float sll;
        float bll;
        List<String> groups;

        public TrackRenderData(Matrix4 m, List<String> groups, float blockLightLevel, float skyLightLevel) {
            this.m = m;
            this.groups = groups;
            this.bll = blockLightLevel;
            this.sll = skyLightLevel;
        }
    }

    public static void render(RenderState state, float partialTicks) {
        if (!MinecraftClient.isReady()) {
            return;
        }
        World world = MinecraftClient.getPlayer().getWorld();
        WorldData data = WorldData.get(world);
        if (data == null ) {
            return;
        }

        if (1 == 0) {
            for (Region region : data.getRegions()) {
                RegionRender renderer = regions.computeIfAbsent(region, RegionRender::new);
                renderer.render(state);
            }
        } else if (1 == 0){
            for (Region region : data.getRegions()) {
                for (TrackInfo info : region.getTrackInfos()) {
                    // TODO filter...
                    // TODO lighting...
                    RenderState infoState = state.clone().translate(info.placementInfo.placementPosition);
                    RailInfo rail = (info.settings.type == TrackItems.SWITCH ? info.withSettings(b -> b.type = TrackItems.STRAIGHT) : info);
                    RailRender.render(rail, world, Vec3i.ZERO, false, infoState);
                }
            }
        } else if (1 == 1) {
            Map<String, RailInfo> infos = new HashMap<>();
            Map<String, List<Vec3d>> posMap = new HashMap<>();


            for (Region region : data.getRegions()) {
                for (TrackInfo info : region.getTrackInfos()) {
                    RailInfo rail = (info.settings.type == TrackItems.SWITCH ? info.withSettings(b -> b.type = TrackItems.STRAIGHT) : info);

                    if (!infos.containsKey(rail.uniqueID)) {
                        infos.put(rail.uniqueID, rail);
                        posMap.put(rail.uniqueID, new ArrayList<>());
                    }
                    posMap.get(rail.uniqueID).add(rail.placementInfo.placementPosition);
                }
            }
            for (RailInfo info : infos.values()) {
                RailRender renderer = RailRender.get(info);
                // TODO filter...
                // TODO lighting...
                renderer.renderRailModelMulti(state, posMap.get(info.uniqueID));

            }
        } else {
            for (Region region : data.getRegions()) {
                RegionData renderer = regionData.computeIfAbsent(region, RegionData::new);
                renderer.render(state);
            }
        }
    }
}
