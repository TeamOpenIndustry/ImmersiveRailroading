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
    public final List<Vec3i> energyInputPoints;
    public final int powerMaximumValue;
    public final DataBlock gui;

    public final Vec3d itemOutputPoint;
    public final int itemOutputRatioBase;
    public final int itemOutputRatioMod;
    public final Vec3i fluidOutputPoint;
    public final boolean allowThrowItems;
    public final String autoFillTanks;
    public final Vec3d initialVelocity;
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
        this.center = toVec3i(object.getValue("center").asString());
        if (!structure.containsKey(center)) {
            throw new IllegalArgumentException("You must include the center block in the structure!");
        }

        DataBlock input = object.getBlock("input");
        this.itemInputPoints = new LinkedList<>();
        List<DataBlock.Value> items = input.getValues("item_input_point");
        if (items != null) {
            items.stream().map(DataBlock.Value::asString).map(MultiblockDefinition::toVec3i).forEach(itemInputPoints::add);
        }
        this.fluidInputPoints = new LinkedList<>();
        List<DataBlock.Value> fluids = input.getValues("fluid_input_point");
        if (fluids != null) {
            fluids.stream().map(DataBlock.Value::asString).map(MultiblockDefinition::toVec3i).forEach(fluidInputPoints::add);
        }
        this.energyInputPoints = new LinkedList<>();
        List<DataBlock.Value> energy = input.getValues("energy_input_point");
        if (energy != null) {
            energy.stream().map(DataBlock.Value::asString).map(MultiblockDefinition::toVec3i).forEach(energyInputPoints::add);
        }
        this.allowThrowInput = input.getValue("allow_throw").asBoolean();
        if (this.type == TRANSPORTER) {
            this.inventoryHeight = input.getValue("inventory_height").asInteger();
            this.inventoryWidth = input.getValue("inventory_width").asInteger();
            this.tankCapability = input.getValue("tank_capability_mb").asInteger();
            this.powerMaximumValue = 0;
        } else if (this.type == CRAFTER) {
            this.inventoryHeight = 0;
            this.inventoryWidth = 0;
            this.tankCapability = 0;
            this.powerMaximumValue = input.getValue("power_limit_rf").asInteger();
        } else {//DETECTOR
            this.inventoryHeight = 0;
            this.inventoryWidth = 0;
            this.tankCapability = 0;
            this.powerMaximumValue = 0;
        }

        DataBlock output = object.getBlock("output");
        this.itemOutputPoint = toVec3d(output.getValue("item_output_point").asString());
        this.itemOutputRatioBase = output.getValue("output_ratio_items_per_sec").asInteger() / 20;
        this.itemOutputRatioMod = output.getValue("output_ratio_items_per_sec").asInteger() % 20;
        this.fluidOutputPoint = toVec3i(output.getValue("fluid_output_point").asString());
        if (itemOutputPoint != null) {
            this.allowThrowItems = output.getValue("should_throw").asBoolean();
            this.initialVelocity = toVec3d(output.getValue("initial_velocity").asString());

            this.useRedstoneControl = output.getValue("redstone_control").asBoolean();
            if (this.useRedstoneControl) {
                this.redstoneControlPoint = toVec3i(output.getValue("redstone_control_point").asString());
            } else {
                this.redstoneControlPoint = null;
            }
        } else {
            this.allowThrowItems = false;
            this.initialVelocity = Vec3d.ZERO;
            this.useRedstoneControl = false;
            this.redstoneControlPoint = null;
        }

        if (this.fluidOutputPoint != null && output.getValue("auto_fill") != null) {
            this.autoFillTanks = output.getValue("auto_fill").asBoolean().toString();
        } else {
            this.autoFillTanks = null;
        }

        DataBlock properties = object.getBlock("properties");
        if (this.type == CRAFTER) {
            this.gui = CAML.parse(properties.getValue("gui").asIdentifier().getResourceStream());
        } else {
            this.gui = null;
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
