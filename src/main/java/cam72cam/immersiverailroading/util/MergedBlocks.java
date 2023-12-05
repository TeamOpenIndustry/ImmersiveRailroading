package cam72cam.immersiverailroading.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MergedBlocks implements DataBlock {
    Map<String, DataBlock.Value> primitives;
    Map<String, List<DataBlock.Value>> primitiveSets;
    Map<String, DataBlock> blocks;
    Map<String, List<DataBlock>> blockSets;

    public MergedBlocks(DataBlock base, DataBlock override) {
        this.primitives = new LinkedHashMap<>(base.getValueMap());
        this.primitiveSets = new LinkedHashMap<>(base.getValuesMap());
        this.blocks = new LinkedHashMap<>(base.getBlockMap());
        this.blockSets = new LinkedHashMap<>(base.getBlocksMap());

        primitives.putAll(override.getValueMap());
        override.getValuesMap().forEach((key, values) -> {
            if (primitiveSets.containsKey(key)) {
                // Merge into new list
                List<Value> tmp = new ArrayList<>(primitiveSets.get(key));
                tmp.addAll(values);
                values = tmp;
            }
            primitiveSets.put(key, values);
        });
        override.getBlockMap().forEach((key, block) -> {
            if (blocks.containsKey(key)) {
                block = new MergedBlocks(blocks.get(key), block);
            }
            blocks.put(key, block);
        });
        override.getBlocksMap().forEach((key, blocks) -> {
            if (blockSets.containsKey(key)) {
                List<DataBlock> tmp = new ArrayList<>(blockSets.get(key));
                tmp.addAll(blocks);
                blocks = tmp;
            }
            blockSets.put(key, blocks);
        });
    }

    @Override
    public Map<String, Value> getValueMap() {
        return primitives;
    }

    @Override
    public Map<String, List<Value>> getValuesMap() {
        return primitiveSets;
    }

    @Override
    public Map<String, DataBlock> getBlockMap() {
        return blocks;
    }

    @Override
    public Map<String, List<DataBlock>> getBlocksMap() {
        return blockSets;
    }
}
