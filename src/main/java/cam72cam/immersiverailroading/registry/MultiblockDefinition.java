package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.model.MultiblockModel;
import cam72cam.immersiverailroading.render.multiblock.CustomRender;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class MultiblockDefinition {
    public final String mbID;
    public final String name;
    public final String type;

    public final int length;
    public final int height;
    public final int width;
    public final HashMap<Vec3i, String> structure;
    public final MultiblockModel model;//-x is left, +y is front, origin in blender is origin in game

    public final List<Vec3i> itemInputPoints;
    public final List<Vec3i> energyInputPoints;
    public final boolean allowThrowInput;
    public final int inventoryHeight;
    public final int inventoryWidth;

    public final Vec3i outputPoint;
    public final int outputRatio;
    public final boolean allowThrowOutput;
    public final Vec3d throwOutputOffset;
    public final boolean useRedstoneControl;
    public final Vec3i redstoneControlPoint;

    MultiblockDefinition(String multiblockID, DataBlock object) throws Exception {
        this.mbID = multiblockID;
        this.name = object.getValue("name").asString().toUpperCase();
        this.type = object.getValue("type").asString();

        this.length = object.getValue("length").asInteger();
        this.height = object.getValue("height").asInteger();
        this.width = object.getValue("width").asInteger();
        this.structure = new HashMap<>();
        DataBlock blocks = object.getBlock("structure");
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    String vec = String.format("%s,%s,%s", x, y, z);
                    if (blocks.getValue(vec).asString() != null) {
                        structure.put(new Vec3i(x, y, z), blocks.getValue(vec).asString());
                    }
                }
            }
        }

        this.model = new MultiblockModel(object.getValue("model").asIdentifier(), 0);
        CustomRender.addDef(this);

        DataBlock input = object.getBlock("input");
        this.itemInputPoints = new LinkedList<>();
        List<DataBlock.Value> items = input.getValues("item_input_point");
        if (items != null) {
            items.stream().map(DataBlock.Value::asString).map(MultiblockDefinition::parseString).forEach(itemInputPoints::add);
        }
        this.energyInputPoints = new LinkedList<>();
        List<DataBlock.Value> energy = input.getValues("energy_input_point");
        if (energy != null) {
            energy.stream().map(DataBlock.Value::asString).map(MultiblockDefinition::parseString).forEach(energyInputPoints::add);
        }
        this.allowThrowInput = input.getValue("allow_throw").asBoolean();
        if(this.type.equals("TRANSPORTER")){
            this.inventoryHeight = input.getValue("height").asInteger();
            this.inventoryWidth = input.getValue("width").asInteger();
        }else{
            this.inventoryHeight = 0;
            this.inventoryWidth = 0;
        }

        DataBlock output = object.getBlock("output");
        this.outputPoint = parseString(output.getValue("item_output_point").asString());
        this.outputRatio = output.getValue("output_ratio_items_per_sec").asInteger();
        if(outputPoint != null){
            this.allowThrowOutput = output.getValue("should_throw").asBoolean();
            String str = output.getValue("offset").asString();
            switch (str){
                case "+X":
                    this.throwOutputOffset = new Vec3d(0,0,0.75);
                    break;
                case "+Y":
                    this.throwOutputOffset = new Vec3d(0.75,0,0);
                    break;
                case "+Z":
                    this.throwOutputOffset = new Vec3d(0,0.75,0);
                    break;
                case "-X":
                    this.throwOutputOffset = new Vec3d(0,0,-0.75);
                    break;
                case "-Y":
                    this.throwOutputOffset = new Vec3d(-0.75,0,0);
                    break;
                case "-Z":
                    this.throwOutputOffset = new Vec3d(0,-0.75,0);
                    break;
                default:
                    this.throwOutputOffset = Vec3d.ZERO;
            }
        } else {
            this.allowThrowOutput = false;
            this.throwOutputOffset = Vec3d.ZERO;
        }
        this.useRedstoneControl = output.getValue("redstone_control").asBoolean();
        if(this.useRedstoneControl){
            this.redstoneControlPoint = parseString(output.getValue("redstone_control_point").asString());
        }else{
            this.redstoneControlPoint  = Vec3i.ZERO;
        }

//        DataBlock properties = object.getBlock("properties");
    }

    private static Vec3i parseString(String origin){
        if(origin == null)
            return null;
        String[] split = origin.split(",");
        if(split.length != 3){
            throw new IllegalArgumentException("Contains invalid vector: %s");
        }
        return new Vec3i(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
    }
}
