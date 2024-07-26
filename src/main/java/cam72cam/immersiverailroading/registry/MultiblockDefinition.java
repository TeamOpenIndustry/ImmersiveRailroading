package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.library.AnimationMode;
import cam72cam.immersiverailroading.library.MultiblockTypes;
import cam72cam.immersiverailroading.model.MultiblockModel;
import cam72cam.immersiverailroading.model.animation.Animatrix;
import cam72cam.immersiverailroading.model.animation.MultiblockAnimation;
import cam72cam.immersiverailroading.render.multiblock.CustomMultiblockRender;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;

import java.io.IOException;
import java.util.*;

public class MultiblockDefinition {
    public final String mbID;
    public final String name;
    public final MultiblockTypes type;

    public int height;
    public int width;
    public int length;
    public final HashMap<Vec3i, String> structure = new HashMap<>();
    public final Vec3i center;
    public final MultiblockModel model;//-x is left, +y is front, origin in blender is origin in game

    //items
    public final List<Vec3i> itemInputPoints = new LinkedList<>();
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

    public final MultiblockAnimationDefinition itemAnimation;
    public float transportPercent = 0;
    public int transportAmount = 0;
    public float itemAnimFrameCount = 0;

    //fluids
    public final List<Vec3i> fluidHandlePoints = new LinkedList<>();
    public final float interactRadius;
    public final int tankCapability;
    public final boolean isFluidToStocks;//true is from pipe to stock, false is the opposite
    public final String autoInteractWithStocks;
//    public final List<Vec3i> energyInputPoints;
//    public final int powerMaximumValue;
//    public final DataBlock gui;

    //Animations
    public final Map<String, MultiblockAnimation> animations = new HashMap<>();

    MultiblockDefinition(String multiblockID, DataBlock object) throws Exception {
        //Standards
        this.mbID = multiblockID;
        this.name = object.getValue("name").asString().toUpperCase();
        this.type = MultiblockTypes.valueOf(object.getValue("type").asString());

        //Structure
        this.length = object.getValue("length").asInteger();
        this.height = object.getValue("height").asInteger();
        this.width = object.getValue("width").asInteger();
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
        if (item != null) {
            List<DataBlock.Value> itemInputPoint = item.getValues("item_input_point");
            if (itemInputPoint != null) {
                itemInputPoint.stream().map(DataBlock.Value::asString).map(MultiblockDefinition::toVec3i).forEach(itemInputPoints::add);
            }
            this.allowThrowInput = item.getValue("allow_throw_input").asBoolean();
            this.inventoryHeight = item.getValue("inventory_height").asInteger();
            this.inventoryWidth = item.getValue("inventory_width").asInteger();
            if(item.getBlock("animation") != null){
                this.itemOutputRatioBase = 0;
                this.itemOutputRatioMod = 0;

                this.itemAnimation = new MultiblockAnimationDefinition(item.getBlock("animation"), "items");
                this.animations.put("items", new MultiblockAnimation(itemAnimation));

                int transportFrame = item.getBlock("animation").getValue("transport_frame").asInteger();
                this.transportAmount = item.getBlock("animation").getValue("transport_amount_per_loop").asInteger();

                try{
                    this.itemAnimFrameCount = new Animatrix(this.itemAnimation.animatrix.getResourceStream(), 1).frameCount();
                    this.transportPercent = (float) transportFrame / itemAnimFrameCount;
                    if (transportPercent < 0 || transportPercent > 1) {
                        throw new IllegalArgumentException(String.format("Invalid transport frame: must in range [0, %f]", itemAnimFrameCount));
                    }
                }catch (IOException e){
                    this.itemAnimFrameCount = 0;
                    ModCore.error(e.toString());
                }
            }else{
                itemAnimation = null;
                this.itemOutputRatioBase = item.getValue("items_output_per_sec").asInteger() / 20;
                this.itemOutputRatioMod = item.getValue("items_output_per_sec").asInteger() % 20;
            }

            if (model.itemOutputPoint != null) {
                this.itemOutputPoint = new Vec3d(-model.itemOutputPoint.center.x, model.itemOutputPoint.center.y, -model.itemOutputPoint.center.z);

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
                this.itemOutputPoint = Vec3d.ZERO;
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
            this.itemAnimation = null;
        }

        DataBlock fluid = object.getBlock("fluid");
        if (fluid != null) {
            List<DataBlock.Value> fluids = fluid.getValues("fluid_handle_points");
            if (fluids != null) {
                fluids.stream().map(DataBlock.Value::asString).map(MultiblockDefinition::toVec3i).forEach(fluidHandlePoints::add);
            }

            this.tankCapability = fluid.getValue("tank_capability_mb").asInteger();
            float radius = fluid.getValue("stock_interact_radius_m").asFloat();
            this.interactRadius = radius * (radius <= 0 ? -1 : 1);

            this.isFluidToStocks = fluid.getValue("pipes_to_stocks").asBoolean();
            if(fluid.getValue("auto_interact").asBoolean() != null) {
                this.autoInteractWithStocks = fluid.getValue("auto_interact").asBoolean().toString();
            }else{
                this.autoInteractWithStocks = null;
            }
        } else {
            this.tankCapability = 0;
            this.interactRadius = 0;
            this.isFluidToStocks = false;
            this.autoInteractWithStocks = null;
        }


        List<DataBlock> animationBlocks = object.getBlocks("animations");
        if (animationBlocks != null) {
            animationBlocks.stream()
                    .map(block -> new MultiblockAnimationDefinition(block, null))
                    .forEach(def -> {
                        try {
                            animations.put(def.control_group, new MultiblockAnimation(def));
                        } catch (IOException e) {
                            throw new RuntimeException("Invalid identifier of multiblock anim file");
                        }
                    });
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

    public static class MultiblockAnimationDefinition extends AnimationDefinition{
        public final String control_group;
        public final AnimationMode mode;
        public final boolean toggle;

        public MultiblockAnimationDefinition(DataBlock block, String cg) {
            super(block);
            control_group = cg == null ? block.getValue("control_group").asString() : cg;
            if (control_group == null) {
                throw new IllegalArgumentException(String.format("You must specify a control group for animation: %s", animatrix.toString()));
            }
            mode = AnimationMode.valueOf(block.getValue("mode").asString("LOOP").toUpperCase(Locale.ROOT));
            toggle = block.getValue("toggle").asBoolean(true);
        }
    }
}
