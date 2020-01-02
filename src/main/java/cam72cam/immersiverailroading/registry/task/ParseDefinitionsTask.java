package cam72cam.immersiverailroading.registry.task;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.resource.Identifier;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.Tuple;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class ParseDefinitionsTask implements Callable<Object> {

    private final List<Tuple<String, String>> definitionTuples;
    private final Map<String, DefinitionManager.JsonLoader> jsonLoaders;

    public ParseDefinitionsTask(List<Tuple<String, String>> definitionTuples, Map<String, DefinitionManager.JsonLoader> jsonLoaders) {
        this.definitionTuples = definitionTuples;
        this.jsonLoaders = jsonLoaders;
    }

    @Override
    public Object call() {
        ArrayList<EntityRollingStockDefinition> stockDefinitions = new ArrayList<>(this.definitionTuples.size());

        for (Tuple<String, String> tuple : this.definitionTuples) {
            String defID = tuple.getFirst();
            String defType = tuple.getSecond();

            try {
                ImmersiveRailroading.debug("Parsing model %s", defID);
                Identifier resource = new Identifier(ImmersiveRailroading.MODID, defID);
                InputStream input = resource.getResourceStream();
                JsonParser parser = new JsonParser();
                JsonObject data = parser.parse(new InputStreamReader(input)).getAsJsonObject();
                input.close();

                stockDefinitions.add(jsonLoaders.get(defType).apply(defID, data));
            } catch (Exception e) {
                ImmersiveRailroading.catching(e);

                // Important so that progress bar steps correctly.
                stockDefinitions.add(null);
            }
        }

        return stockDefinitions;
    }

}
