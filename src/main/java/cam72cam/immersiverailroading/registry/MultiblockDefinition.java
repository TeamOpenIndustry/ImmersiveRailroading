package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.library.MultiblockTypes;
import cam72cam.immersiverailroading.model.MultiblockModel;
import cam72cam.immersiverailroading.render.multiblock.CustomMultiblockRender;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;

import java.util.*;

public class MultiblockDefinition {
    public final String mbID;
    public final String name;
    public final MultiblockTypes type;

    public int height;
    public int width;
    public int length;
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
        this.name = object.getValue("name").asString().toUpperCase();
        this.type = MultiblockTypes.valueOf(object.getValue("type").asString());

        //Structure
        this.length = object.getValue("length").asInteger();
        this.height = object.getValue("height").asInteger();
        this.width = object.getValue("width").asInteger();
        this.structure = new HashMap<>();
        DataBlock struct = object.getBlock("structure");
        struct.getValueMap().forEach((s, value) -> {
            Vec3i vec3i = toVec3i(s);
            if(vec3i.x >= 0 && vec3i.x < width
                    && vec3i.y >= 0 && vec3i.y < height
                    && vec3i.z >= 0 && vec3i.z < length){
                structure.put(vec3i, value.asString());
            }
        });
        this.center = toVec3i(object.getValue("center").asString());
        if (!structure.containsKey(center)) {
            throw new IllegalArgumentException("You must include the center block in the structure!");
        }
        this.model = new MultiblockModel(object.getValue("model").asIdentifier(), 0, this);

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
            this.itemOutputPoint = model.itemOutputPoint.center.scale(-1);
            this.itemOutputRatioBase = item.getValue("items_output_per_loop").asInteger() / 20;
            this.itemOutputRatioMod = item.getValue("items_output_per_loop").asInteger() % 20;

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
            this.inventoryHeight = 0;
            this.inventoryWidth = 0;
            this.itemOutputPoint = null;
            this.itemOutputRatioBase = 0;
            this.itemOutputRatioMod = 0;
            this.allowThrowInput = false;
            this.allowThrowOutput = false;
            this.initialVelocity = Vec3d.ZERO;
            this.useRedstoneControl = false;
            this.redstoneControlPoint = null;
        }

        this.fluidHandlePoints = new LinkedList<>();
        this.possibleTrackPositions = new HashSet<>();
        DataBlock fluid = object.getBlock("fluid");
        if (fluid != null) {
            List<DataBlock.Value> fluids = fluid.getValues("fluid_handle_points");
            if (fluids != null) {
                fluids.stream().map(DataBlock.Value::asString).map(MultiblockDefinition::toVec3i).forEach(fluidHandlePoints::add);
            }

            model.fluidHandlerPoints.stream().map(component -> component.center.scale(-1)).forEach(vec3d ->
                            fluidOutputPositions.stream()
                                    .map(vec3i -> vec3i.add(new Vec3i(vec3d.x, vec3d.y, vec3d.z)))
                                    .forEach(possibleTrackPositions::add));

            this.tankCapability = fluid.getValue("tank_capability_mb").asInteger();

            this.isFluidToStocks = fluid.getValue("pipes_to_stocks").asBoolean();
            if(fluid.getValue("auto_interact").asBoolean() != null) {
                this.autoInteractWithStocks = fluid.getValue("auto_interact").asBoolean().toString();
            }else{
                this.autoInteractWithStocks = null;
            }
        } else {
            this.tankCapability = 0;
            this.isFluidToStocks = false;
            this.autoInteractWithStocks = null;
        }

        model.generateStaticModels();
        CustomMultiblockRender.addDef(this);
    }

    private static Vec3i toVec3i(String origin) {
        if (origin == null)
            return null;
        String[] split = origin.split(",");
        if (split.length != 3) {
            throw new IllegalArgumentException("Invalid vector: %s");
        }
        return new Vec3i(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
    }

    private static Vec3d toVec3d(String origin) {
        if (origin == null)
            return null;
        String[] split = origin.split(",");
        if (split.length != 3) {
            throw new IllegalArgumentException("Invalid vector: %s");
        }
        return new Vec3d(Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]));
    }

    public static class MultiblockAnimation{
        public final Identifier animatrix;
        public final String animationMode;
        public final float fps;
        public final int transportTime;
        public final int transportAmount;

        public MultiblockAnimation(Identifier animatrix, String animationMode, int fps, int transportTime, int transportAmount) {
            this.animatrix = animatrix;
            this.animationMode = animationMode;
            this.fps = fps;
            this.transportTime = transportTime;
            this.transportAmount = transportAmount;
        }
    }
}
