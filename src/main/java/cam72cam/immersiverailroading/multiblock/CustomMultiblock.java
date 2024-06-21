package cam72cam.immersiverailroading.multiblock;

import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.MultiblockDefinition;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.ItemStackHandler;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

import java.util.List;

public class CustomMultiblock extends Multiblock {
    /*TODO:
     * use Animatrix to Produce animation(I/O for factory)(NOT NOW)
     * REWRITE CURRENT ANIMATION TO FIT MB(NOT NOW)
     * new readouts(NOT NOW)
     * rewrite manual
     */
    private final MultiblockDefinition def;

    public CustomMultiblock(MultiblockDefinition def) {
        super(def.name.toUpperCase(), parseStructure(def));
        this.def = def;
    }

    private static FuzzyProvider[][][] parseStructure(MultiblockDefinition def) {
        FuzzyProvider[][][] provider = new FuzzyProvider[def.length][def.height][def.width];
        for (Vec3i entry : def.structure.keySet()) {
            provider[entry.z][entry.y][entry.x] = parseFuzzy(def.structure.get(entry));
        }
        return provider;
    }

    private static FuzzyProvider parseFuzzy(String def) {
        if(Fuzzy.get(def).isEmpty()){
            switch (def){
                case "light_engineering_block":
                    return L_ENG();
                case "heavy_engineering_block":
                    return H_ENG();
                case "steel":
                default:
                    return STEEL();
            }
        }
        return ()->Fuzzy.get(def);
    }

    @Override
    public Vec3i placementPos() {
        return Vec3i.ZERO;
    }

    @Override
    protected MultiblockInstance newInstance(World world, Vec3i origin, Rotation rot) {
        return new CustomMultiblockInstance(world, origin, rot, this.def);
    }

    public class CustomMultiblockInstance extends MultiblockInstance {
        public final MultiblockDefinition def;
        private long ticks = 0;

        public CustomMultiblockInstance(World world, Vec3i origin, Rotation rot, MultiblockDefinition def) {
            super(world, origin, rot);
            this.def = def;
        }

        @Override
        public boolean onBlockActivated(Player player, Player.Hand hand, Vec3i offset) {
            if (world.isServer) {
                Vec3i pos = getPos(Vec3i.ZERO);
                GuiTypes.CUSTOM_MULTIBLOCK_TRANS.open(player, pos);
            }
            return true;
        }

        @Override
        public int getInvSize(Vec3i offset) {
            return this.def.inventoryWidth * this.def.inventoryHeight;
        }

        @Override
        public int getTankCapability(Vec3i offset) {
            return this.def.tankCapability;
        }

        @Override
        public boolean isRender(Vec3i offset) {
            return offset.equals(Vec3i.ZERO);
        }

        @Override
        public void tick(Vec3i offset) {
            this.ticks += 1;
            if(this.getInvSize(Vec3i.ZERO) != 0){
                if (def.itemInputPoints.contains(offset)) {
                    //Handle item(thrown) input
                    if (def.allowThrowInput) {
                        Vec3d vec3d = new Vec3d(getPos(offset));
                        List<ItemStack> stacks = world.getDroppedItems(IBoundingBox.from(
                                vec3d.subtract(0.5, 0.5, 0.5), vec3d.add(1.5, 1.5, 1.5)));
                        ItemStackHandler handler = this.getTile(Vec3i.ZERO)//TODO perhaps we should use the "center" property?
                                .getContainer();
                        if (!stacks.isEmpty()) {
                            for (int fromSlot = 0; fromSlot < stacks.size(); fromSlot++) {
                                ItemStack stack = stacks.get(fromSlot);
                                int origCount = stack.getCount();

                                if (stack.isEmpty()) {
                                    continue;
                                }

                                for (int toSlot = 0; toSlot < handler.getSlotCount(); toSlot++) {
                                    stack.setCount(handler.insert(toSlot, stack, false).getCount());
                                    if (stack.isEmpty()) {
                                        break;
                                    }
                                }

                                if (origCount != stack.getCount()) {
                                    stacks.set(fromSlot, stack);
                                }
                            }
                        }
                    }
                }
                //Handle item output
                if (def.outputPoint.equals(offset)) {
                    if (def.redstoneControlPoint == null)
                        return;
                    if (world.getRedstone(getPos(def.redstoneControlPoint)) != 0 && def.allowThrowOutput) {
                        ItemStackHandler handler = getTile(Vec3i.ZERO).getContainer();
                        int slotIndex = handler.getSlotCount() - 1;
                        while (handler.get(slotIndex).getCount() == 0) {
                            slotIndex--;
                            if (slotIndex == -1) {
                                return;
                            }
                        }
                        world.dropItem(handler.extract(slotIndex, def.outputRatioBase, false),
                                new Vec3d(getPos(offset)).add(new Vec3d(0.5, 0.5, 0.5)).add(def.throwOutputOffset
                                        .rotateYaw((float) getTile(Vec3i.ZERO).getRotation())));
                        if (ticks % 20 <= def.outputRatioMod) {
                            world.dropItem(handler.extract(slotIndex, 1, false),
                                    new Vec3d(getPos(offset)).add(new Vec3d(0.5, 0.5, 0.5)).add(def.throwOutputOffset
                                            .rotateYaw(this.getRotation())));
                        }
                    }
                }
            }
        }

        @Override
        public boolean canInsertItem(Vec3i offset, int slot, ItemStack stack) {
            return def.itemInputPoints.stream().anyMatch(offset::equals);
        }

        @Override
        public boolean canReceiveFluid(Vec3i offset) {
            return def.fluidInputPoints.stream().anyMatch(offset::equals);
        }

        @Override
        public boolean isItemOutputSlot(Vec3i offset, int slot) {
            return def.outputPoint.equals(offset);
        }

        @Override
        public boolean isFluidOutputSlot(Vec3i offset) {
            return false;
        }

        @Override
        public int getSlotLimit(Vec3i offset, int slot) {
            return 64;
        }

        @Override
        public boolean canRecievePower(Vec3i offset) {
            return def.energyInputPoints.stream().anyMatch(offset::equals);
        }

        //Helpers
        public float getRotation(){
            return (float) getTile(Vec3i.ZERO).getRotation();
        }
    }
}
