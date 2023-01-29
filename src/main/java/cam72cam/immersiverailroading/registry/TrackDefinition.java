package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.immersiverailroading.library.TrackComponent;
import cam72cam.immersiverailroading.model.TrackModel;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackDefinition {
    public final String trackID;
    public final String name;
    public final String modelerName;
    public final String packName;
    public final List<TrackModel> models;
    public final boolean clack;
    public final Map<TrackComponent, List<TrackMaterial>> materials = new HashMap<>();
    public final float bumpiness;
    public final boolean cog;

    TrackDefinition(String trackID, DataBlock object) throws Exception {
        this.trackID = trackID;
        this.name = object.getValue("name").getString();
        this.modelerName = object.getValue("modeler").getString();
        this.packName = object.getValue("pack").getString();

        this.clack = object.getValue("clack").getBoolean(true);
        this.bumpiness = object.getValue("bumpiness").getFloat(clack ? 1f : 0f);
        this.cog = object.getValue("cog").getBoolean(false);
        this.models = new ArrayList<>();
        DataBlock models = object.getBlock("models");
        for (Map.Entry<String, DataBlock.Value> entry : models.getValueMap().entrySet()) {
            this.models.add(new TrackModel(entry.getKey(), entry.getValue().getIdentifier()));
        }

        DataBlock mats = object.getBlock("materials");
        for (TrackComponent comp : TrackComponent.values()) {
            List<DataBlock> blocks = mats.getBlocks(comp.name());
            if (blocks != null) {
                List<TrackMaterial> parts = new ArrayList<>();
                for (DataBlock part : blocks) {
                    parts.add(new TrackMaterial(
                            part.getValue("item").getString(),
                            part.getValue("cost").getFloat()
                    ));
                }
                if (parts.size() > 0) {
                    materials.put(comp, parts);
                }
            }
        }
    }

    public TrackModel getTrackForGauge(double gauge) {
        for (TrackModel model : models) {
            if (model.canRender(gauge)) {
                return model;
            }
        }
        ImmersiveRailroading.warn("Bad track gauge def for %s - %s", trackID, gauge);
        return models.get(0);
    }

    public static class TrackMaterial {
        public final String item;
        public final float cost;
        public final int meta;

        TrackMaterial(String item, float cost) {
            if (item.contains("|")) {
                this.item = item.split("\\|")[0];
                this.meta = Integer.parseInt(item.split("\\|")[1]);
            } else {
                this.item = item;
                this.meta = 0;
            }
            this.cost = cost;
        }

        public List<ItemStack> examples() {
            List<ItemStack> examples = new ArrayList<>();

            if (item.startsWith("ore:")) {
                String oreName = item.replace("ore:", "");
                examples.addAll(Fuzzy.get(oreName).enumerate());
            } else {
                examples.add(new ItemStack(this.item, 1, meta));
            }
            return examples;
        }

        public boolean matches(ItemStack stack) {
            if (item.startsWith("ore:")) {
                String oreName = item.replace("ore:", "");
                return Fuzzy.get(oreName).matches(stack);
            }
            return stack.is(new ItemStack(item, 1, meta));
        }
    }
}
