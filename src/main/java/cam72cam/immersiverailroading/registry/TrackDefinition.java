package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemRail;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.TrackComponent;
import cam72cam.immersiverailroading.model.TrackModel;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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

    TrackDefinition(String trackID, JsonObject object) throws Exception {
        this.trackID = trackID;
        this.name = object.get("name").getAsString();
        this.modelerName = object.has("modeler") ? object.get("modeler").getAsString() : null;
        this.packName = object.has("pack") ? object.get("pack").getAsString() : null;

        this.clack = !object.has("clack") || object.get("clack").getAsBoolean();
        this.bumpiness = object.has("bumpiness") ? object.get("bumpiness").getAsFloat() : clack ? 1f : 0f;
        this.cog = object.has("cog") && object.get("cog").getAsBoolean();
        this.models = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("models").entrySet()) {
            models.add(new TrackModel(entry.getKey(), entry.getValue().getAsString()));
        }

        JsonObject mats = object.getAsJsonObject("materials");
        for (TrackComponent comp : TrackComponent.values()) {
            if (mats.has(comp.name())) {
                List<TrackMaterial> parts = new ArrayList<>();
                for (JsonElement part : mats.get(comp.name()).getAsJsonArray()) {
                    parts.add(new TrackMaterial(
                            part.getAsJsonObject().get("item").getAsString(),
                            part.getAsJsonObject().get("cost").getAsFloat()
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

        public List<ItemStack> examples(Gauge gauge) {
            List<ItemStack> examples = new ArrayList<>();

            if (item.startsWith("ore:")) {
                String oreName = item.replace("ore:", "");
                examples.addAll(Fuzzy.get(oreName).enumerate());
                for (ItemStack example : examples) {
                    if (example.is(IRItems.ITEM_RAIL)) {
                        ItemRail.Data data = new ItemRail.Data(example);
                        data.gauge = gauge;
                        data.write();
                    }
                }
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
