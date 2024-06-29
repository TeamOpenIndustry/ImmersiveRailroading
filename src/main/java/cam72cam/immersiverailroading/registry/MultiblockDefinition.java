package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.library.MultiblockTypes;
import cam72cam.immersiverailroading.model.MultiblockModel;
import cam72cam.immersiverailroading.render.multiblock.CustomMultiblockRender;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;

import java.util.*;

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

    //items
    public final List<Vec3i> itemInputPoints;
    public final Vec3d itemOutputPoint;
    public final int inventoryHeight;
    public final int inventoryWidth;
    public final int itemOutputRatioBase;
    public final int itemOutputRatioMod;
    public final boolean allowThrowInput;
    public final boolean allowThrowOutput;
    public final Vec3d initialVelocity;
    public final boolean useRedstoneControl;
    public final Vec3i redstoneControlPoint;

    //fluids
    public final List<Vec3i> fluidHandlePoints;
    public final Set<Vec3i> possibleTrackPositions;
    public final int tankCapability;
    public final boolean isFluidToStocks;//true is from pipe to stock, false is the opposite
    public final String autoInteractWithStocks;
//    public final List<Vec3i> energyInputPoints;
//    public final int powerMaximumValue;
//    public final DataBlock gui;


    private static final List<Vec3i> fluidOutputPositions;//All relative possible positions for searching

    static {
        fluidOutputPositions = new ArrayList<>();
        for (int x = -6; x <= 6; x++) {//Store the possible relative poses for fluid output in order to avoid more calculation
            for (int y = 0; y > -8; y--) {
                for (int z = -6; z <= 6; z++) {
                    if (x * x + z * z <= 36) {//Radius == 6
                        fluidOutputPositions.add(new Vec3i(x, y, z));
                    }
                }
            }
        }
    }

    MultiblockDefinition(String multiblockID, DataBlock object) throws Exception {
        //Standards
        this.mbID = multiblockID;
        this.name = object.getValue("name").asString();
        this.type = MultiblockTypes.valueOf(object.getValue("type").asString());

        //Structure
        this.length = object.getValue("length").asInteger();
        this.height = object.getValue("height").asInteger();
        this.width = object.getValue("width").asInteger();
        this.structure = new HashMap<>();
        DataBlock struct = object.getBlock("structure");
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    String vec = String.format("%s,%s,%s", x, y, z);
                    if (struct.getValue(vec).asString() != null) {
                        structure.put(new Vec3i(x, y, z), struct.getValue(vec).asString());
                    }
                }
            }
        }
        this.center = toVec3i(object.getValue("center").asString());
        if (!structure.containsKey(center)) {
            throw new IllegalArgumentException("You must include the center block in the structure!");
        }

        DataBlock item = object.getBlock("item");
        this.itemInputPoints = new LinkedList<>();
        if (item != null) {
            List<DataBlock.Value> itemInputPoint = item.getValues("item_input_point");
            if (itemInputPoint != null) {
                itemInputPoint.stream().map(DataBlock.Value::asString).map(MultiblockDefinition::toVec3i).forEach(itemInputPoints::add);
            }
            this.allowThrowInput = item.getValue("allow_throw_input").asBoolean();
            this.inventoryHeight = item.getValue("inventory_height").asInteger();
            this.inventoryWidth = item.getValue("inventory_width").asInteger();
            this.itemOutputPoint = toVec3d(item.getValue("item_output_point").asString());
            this.itemOutputRatioBase = item.getValue("output_ratio_items_per_sec").asInteger() / 20;
            this.itemOutputRatioMod = item.getValue("output_ratio_items_per_sec").asInteger() % 20;

            if (itemOutputPoint != null) {
                this.allowThrowOutput = item.getValue("should_throw_output").asBoolean();
                this.initialVelocity = toVec3d(item.getValue("initial_velocity").asString());

                this.useRedstoneControl = item.getValue("redstone_control").asBoolean();
                if (this.useRedstoneControl) {
                    this.redstoneControlPoint = toVec3i(item.getValue("redstone_control_point").asString());
                } else {
                    this.redstoneControlPoint = null;
                }
            } else {
                this.allowThrowOutput = false;
                this.initialVelocity = Vec3d.ZERO;
                this.useRedstoneControl = false;
                this.redstoneControlPoint = null;
            }
        } else {
            this.itemOutputPoint = null;
            this.inventoryHeight = 0;
            this.inventoryWidth = 0;
            this.itemOutputRatioBase = 0;
            this.itemOutputRatioMod = 0;
            this.allowThrowInput = false;
            this.allowThrowOutput = false;
            this.initialVelocity = Vec3d.ZERO;
            this.useRedstoneControl = false;
            this.redstoneControlPoint = null;
        }

        DataBlock fluid = object.getBlock("fluid");
        this.fluidHandlePoints = new LinkedList<>();
        this.possibleTrackPositions = new HashSet<>();
        if (fluid != null) {
            List<DataBlock.Value> fluids = fluid.getValues("fluid_handle_points");
            if (fluids != null) {
                fluids.stream().map(DataBlock.Value::asString).map(MultiblockDefinition::toVec3i).forEach(fluidHandlePoints::add);
            }

            List<DataBlock.Value> trackHandlePoints = fluid.getValues("track_handle_points");
            if(trackHandlePoints != null){
                trackHandlePoints.stream()
                        .map(DataBlock.Value::asString)
                        .map(MultiblockDefinition::toVec3i)
                        .forEach(vec3i -> fluidOutputPositions.stream().map(vec3i::add).forEach(possibleTrackPositions::add));
            }
            this.tankCapability = fluid.getValue("tank_capability_mb").asInteger();

            this.isFluidToStocks = fluid.getValue("pipes_to_stocks").asBoolean();
            if(fluid.getValue("auto_interact") != null) {
                this.autoInteractWithStocks = fluid.getValue("auto_interact").asBoolean().toString();
            }else{
                this.autoInteractWithStocks = null;
            }
        } else {
            this.tankCapability = 0;
            this.isFluidToStocks = false;//true is from pipe to stock, false is the opposite
            this.autoInteractWithStocks = null;
        }

        //Put these at the bottom as them use the properties above
        this.model = new MultiblockModel(object.getValue("model").asIdentifier(), 0, this);
        CustomMultiblockRender.addDef(this);
    }

    private static Vec3i toVec3i(String origin) {
        if (origin == null)
            return null;
        String[] split = origin.split(",");
        if (split.length != 3) {
            throw new IllegalArgumentException("Contains invalid vector: %s");
        }
        return new Vec3i(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
    }

    private static Vec3d toVec3d(String origin) {
        if (origin == null)
            return null;
        String[] split = origin.split(",");
        if (split.length != 3) {
            throw new IllegalArgumentException("Contains invalid vector: %s");
        }
        return new Vec3d(Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]));
    }
}
