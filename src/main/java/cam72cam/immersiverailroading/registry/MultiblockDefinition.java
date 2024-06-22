package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.library.MultiblockTypes;
import cam72cam.immersiverailroading.model.MultiblockModel;
import cam72cam.immersiverailroading.render.multiblock.CustomMultiblockRender;
import cam72cam.immersiverailroading.util.CAML;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static cam72cam.immersiverailroading.library.MultiblockTypes.*;

public class MultiblockDefinition {
    public final String mbID;
    public final String name;
    public final MultiblockTypes type;

    public final int length;
    public final int height;
    public final int width;
    public final HashMap<Vec3i, String> structure;
    public final Vec3i center;
    public final MultiblockModel model;//-x is left, +y is front, origin in blender is origin in game

    public final List<Vec3i> itemInputPoints;
    public final List<Vec3i> fluidInputPoints;
    public final boolean allowThrowInput;
    //For TRANSPORTERS
    public final int inventoryHeight;
    public final int inventoryWidth;
    public final int tankCapability;
    //For CRAFTER
    //Unstable, DON'T USE!
    public final List<Vec3i> energyInputPoints;
    public final int powerMaximumValue;
    public final DataBlock gui;

    public final Vec3i outputPoint;
    public final int outputRatioBase;
    public final int outputRatioMod;
    public final boolean allowThrowOutput;
    public final Vec3d throwOutputOffset;
    public final boolean useRedstoneControl;
    public final Vec3i redstoneControlPoint;

    MultiblockDefinition(String multiblockID, DataBlock object) throws Exception {
        this.mbID = multiblockID;
        this.name = object.getValue("name").asString().toUpperCase();
        this.type = MultiblockTypes.valueOf(object.getValue("type").asString());

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
        this.center = parseString(object.getValue("center").asString());
        if(!structure.containsKey(center)){
            throw new IllegalArgumentException("You must include the center block in the structure!");
        }
        this.model = new MultiblockModel(object.getValue("model").asIdentifier(), 0);
        CustomMultiblockRender.addDef(this);

        DataBlock input = object.getBlock("input");
        this.itemInputPoints = new LinkedList<>();
        List<DataBlock.Value> items = input.getValues("item_input_point");
        if (items != null) {
            items.stream().map(DataBlock.Value::asString).map(MultiblockDefinition::parseString).forEach(itemInputPoints::add);
        }
        this.fluidInputPoints = new LinkedList<>();
        List<DataBlock.Value> fluids = input.getValues("fluid_input_point");
        if (fluids != null) {
            fluids.stream().map(DataBlock.Value::asString).map(MultiblockDefinition::parseString).forEach(fluidInputPoints::add);
        }
        this.energyInputPoints = new LinkedList<>();
        List<DataBlock.Value> energy = input.getValues("energy_input_point");
        if (energy != null) {
            energy.stream().map(DataBlock.Value::asString).map(MultiblockDefinition::parseString).forEach(energyInputPoints::add);
        }
        this.allowThrowInput = input.getValue("allow_throw").asBoolean();
        if(this.type == TRANSPORTER){
            this.inventoryHeight = input.getValue("inventory_height").asInteger();
            this.inventoryWidth = input.getValue("inventory_width").asInteger();
            this.tankCapability = input.getValue("tank_capability_mb").asInteger();
            this.powerMaximumValue = 0;
        }else if(this.type == CRAFTER){
            this.powerMaximumValue = input.getValue("power_limit_rf").asInteger();
            this.inventoryHeight = 0;
            this.inventoryWidth = 0;
            this.tankCapability = 0;
        }else{//DETECTOR
            this.inventoryHeight = 0;
            this.inventoryWidth = 0;
            this.tankCapability = 0;
            this.powerMaximumValue = 0;
        }

        DataBlock output = object.getBlock("output");
        this.outputPoint = parseString(output.getValue("item_output_point").asString());
        this.outputRatioBase = output.getValue("output_ratio_items_per_sec").asInteger() / 20;
        this.outputRatioMod = output.getValue("output_ratio_items_per_sec").asInteger() % 20;
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
            this.redstoneControlPoint = null;
        }

        DataBlock properties = object.getBlock("properties");
        if(this.type == CRAFTER){
            this.gui = CAML.parse(properties.getValue("gui").asIdentifier().getResourceStream());
        }else{
            this.gui = null;
        }
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
