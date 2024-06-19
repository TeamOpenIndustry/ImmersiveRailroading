package cam72cam.immersiverailroading.multiblock;

import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.MultiblockDefinition;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.ItemStackHandler;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

import java.util.List;

public class CustomMultiblock extends Multiblock {
    /* Define multiblock through json Done
     *TODO:
     * use Animatrix to Produce animation(I/O for factory)
     * new readouts
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
        def = def.toUpperCase();
        if (def.equals("L_ENG"))
            return L_ENG();
        else if (def.equals("H_ENG"))
            return H_ENG();
        return STEEL();
    }

    @Override
    public Vec3i placementPos() {
        return Vec3i.ZERO;
    }

    @Override
    protected MultiblockInstance newInstance(World world, Vec3i origin, Rotation rot) {
        return new CustomMultiblockInstance(world, origin, rot, this.def);
    }

    public class CustomMultiblockInstance extends MultiblockInstance {//TODO sync the inventories
        public final MultiblockDefinition def;
        private boolean allowOutput = false;

        public CustomMultiblockInstance(World world, Vec3i origin, Rotation rot, MultiblockDefinition def) {
            super(world, origin, rot);
            this.def = def;
        }

        @Override
        public boolean onBlockActivated(Player player, Player.Hand hand, Vec3i offset) {
            if (world.isServer) {
                Vec3i pos = getPos(offset);
                GuiTypes.CUSTOM_MULTIBLOCK_TRANS.open(player, pos);
            }
            return true;
        }

        @Override
        public int getInvSize(Vec3i offset) {
            return this.def.inventoryWidth * this.def.inventoryHeight;
        }

        @Override
        public boolean isRender(Vec3i offset) {
            return offset.x == 0 && offset.y == 0 && offset.z == 0;
        }

        @Override
        public void tick(Vec3i offset) {
            if (def.itemInputPoints.contains(offset)) {
                if (def.allowThrowInput) {
                    Vec3d vec3d = new Vec3d(getPos(offset));
                    List<ItemStack> stacks = world.getDroppedItems(IBoundingBox.from(
                            vec3d.subtract(0.5, 0.5, 0.5), vec3d.add(1.5, 1.5, 1.5)));
                    ItemStackHandler handler = this.getTile(new Vec3i(0, 0, 0))//TODO perhaps we should open the "center" property?
                            .getContainer();
                    if (!stacks.isEmpty()) {
                        //transfer to this.cargoItems
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

            if (def.useRedstoneControl && def.redstoneControlPoint.equals(offset)) {
                this.allowOutput = world.getRedstone(getPos(offset)) >= 1;
            }

            if (allowOutput && def.outputPoint.equals(offset) && def.allowThrowOutput) {
                int slotIndex = 0;
                int count = getTile(Vec3i.ZERO).getContainer().getSlotCount();
                while (getTile(Vec3i.ZERO).getContainer().get(slotIndex).getCount() == 0) {
                    slotIndex++;
                    if (slotIndex >= count) {
                        return;
                    }
                }
                world.dropItem(getTile(Vec3i.ZERO).getContainer().extract(slotIndex, 1, false),
                        new Vec3d(getPos(offset)).add(def.throwOutputOffset));
            }
        }

        @Override
        public boolean canInsertItem(Vec3i offset, int slot, ItemStack stack) {
            return def.itemInputPoints.stream().anyMatch(offset::equals);
        }

        @Override
        public boolean isOutputSlot(Vec3i offset, int slot) {
            return def.outputPoint.equals(offset);
        }

        @Override
        public int getSlotLimit(Vec3i offset, int slot) {
            return 64;
        }

        @Override
        public boolean canRecievePower(Vec3i offset) {
            return def.energyInputPoints.stream().anyMatch(offset::equals);
        }
    }
}
