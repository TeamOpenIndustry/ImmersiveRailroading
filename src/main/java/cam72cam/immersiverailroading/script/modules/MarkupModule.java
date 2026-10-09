package cam72cam.immersiverailroading.script.modules;

import cam72cam.immersiverailroading.script.LuaFunction;
import cam72cam.immersiverailroading.script.LuaModule;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.resource.Identifier;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MarkupModule implements LuaModule {
    public MarkupModule() {}

    @LuaFunction(module = "Markup")
    private LuaValue read(LuaValue fileIdentifier) {
        Identifier location = new Identifier(fileIdentifier.tojstring());
        try {
            DataBlock block = DataBlock.load(location);
            return convertDataBlock(block);
        } catch (IOException e) {
            return new LuaError("Given file is not recognised as json or caml").getMessageObject();
        }
    }

    private static LuaValue convertDataBlock(DataBlock block) {
        LuaTable table = new LuaTable();

        LuaTable primitives = new LuaTable();
        for (Map.Entry<String, DataBlock.Value> entry : block.getValueMap().entrySet()) {
            String key = entry.getKey();
            DataBlock.Value value = entry.getValue();

            primitives.set(key, convertValue(value));

            table.set(key, convertValue(value));
        }

        LuaTable primitiveSets = new LuaTable();
        for (Map.Entry<String, List<DataBlock.Value>> entry : block.getValuesMap().entrySet()) {
            String key = entry.getKey();
            LuaTable listTable = new LuaTable();
            List<DataBlock.Value> values = entry.getValue();
            for (int i = 0; i < values.size(); i++) {
                listTable.set(i + 1, convertValue(values.get(i)));
            }

            primitiveSets.set(key, listTable);
            table.set(key, listTable);
        }

        LuaTable blocks = new LuaTable();
        for (Map.Entry<String, DataBlock> entry : block.getBlockMap().entrySet()) {
            table.set(entry.getKey(), convertDataBlock(entry.getValue()));
            blocks.set(entry.getKey(), convertDataBlock(entry.getValue()));
        }

        LuaTable blockSets = new LuaTable();
        for (Map.Entry<String, List<DataBlock>> entry : block.getBlocksMap().entrySet()) {
            String key = entry.getKey();
            LuaTable listTable = new LuaTable();
            List<DataBlock> dataBlocks = entry.getValue();
            for (int i = 0; i < dataBlocks.size(); i++) {
                listTable.set(i + 1, convertDataBlock(dataBlocks.get(i)));
            }
            table.set(key, listTable);
            blockSets.set(key, listTable);
        }

        return table;
    }

    private static LuaValue convertValue(DataBlock.Value value) {
        if (value == null) return LuaValue.NIL;

        Boolean boolVal = value.asBoolean();
        if (boolVal != null) {
            return LuaValue.valueOf(boolVal);
        }

        Integer intVal = value.asInteger();
        if (intVal != null) {
            return LuaValue.valueOf(intVal);
        }

        Double doubleVal = value.asDouble();
        if (doubleVal != null) {
            return LuaValue.valueOf(doubleVal);
        }

        String strVal = value.asString();
        if (strVal != null) {
            return LuaValue.valueOf(strVal);
        }

        return LuaValue.NIL;
    }
}
