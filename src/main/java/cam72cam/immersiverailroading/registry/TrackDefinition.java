package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.TrackComponent;
import cam72cam.immersiverailroading.model.TrackModel;
import cam72cam.immersiverailroading.util.OreHelper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.*;

public class TrackDefinition {
    public final String trackID;
    public final String name;
    public final List<TrackModel> models;
    public final Map<TrackComponent, List<TrackMaterial>> materials = new HashMap<>();

    public static class TrackMaterial {
        public final String item;
        public final float cost;
        public final int meta;

        public TrackMaterial(String item, float cost) {
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
                //TODO make IR ore dicts actual ore dicts
                if (oreName.equals("irRail")) {
                    examples.addAll(OreHelper.IR_RAIL.getOres());
                }
                if (oreName.equals("irTie")) {
                    examples.addAll(OreHelper.IR_TIE.getOres());
                }
                examples.addAll(OreDictionary.getOres(oreName));
            } else {
                examples.add(new ItemStack(Item.getByNameOrId(this.item), 1, meta));
            }
            return examples;
        }

        public boolean matches(ItemStack stack) {
            if (item.startsWith("ore:")) {
                String oreName = item.replace("ore:", "");
                //TODO make IR ore dicts actual ore dicts
                if (oreName.equals("irRail")) {
                    return OreHelper.IR_RAIL.matches(stack, false);
                }
                if (oreName.equals("irTie")) {
                    return OreHelper.IR_TIE.matches(stack, false);
                }
                return OreHelper.oreDictionaryContainsMatch(false, OreDictionary.getOres(oreName), stack);
            }
            return stack.getItem() == Item.getByNameOrId(item) && stack.getMetadata() == meta;
        }
    }

    public TrackDefinition(String trackID, JsonObject object) throws Exception {
        this.trackID = trackID;
        this.name = object.get("name").getAsString();
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
}
