package cam72cam.immersiverailroading.multiblock;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.FreightTank;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.MultiblockDefinition;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.ItemStackHandler;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomTransporterMultiblock extends Multiblock {
    /*TODO:
     * use Animatrix to Produce animation(I/O for factory)(NOT NOW)
     * REWRITE CURRENT ANIMATION TO FIT MB(NOT NOW)
     * new readouts(NOT NOW)
     * rewrite manual
     */
    private final MultiblockDefinition def;
    private static final List<Vec3i> fluidOutputPositions;

    static {
        fluidOutputPositions = new ArrayList<>();
        for(int x = -4; x <= 4; x++){//Store the possible relative poses for fluid output in order to avoid more search
            for(int z = -4; z <= 4; z++){
                if(x * x + z * z <= 16){//Radius == 4
                    fluidOutputPositions.add(new Vec3i(x,0,z));
                }
            }
        }
    }

    public CustomTransporterMultiblock(MultiblockDefinition def) {
        super(def.name.toUpperCase(), parseStructure(def));
        this.def = def;
        //The center block should be built first...
        componentPositions.remove(def.center);
        componentPositions.add(0,def.center);
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
        return def.center;
    }

    @Override
    protected MultiblockInstance newInstance(World world, Vec3i origin, Rotation rot) {
        return new TransporterMbInstance(world, origin, rot, this.def);
    }

    public class TransporterMbInstance extends MultiblockInstance {
        public final MultiblockDefinition def;
        public Map<String, FreightTank> stockList;
        private long ticks = 0;

        public TransporterMbInstance(World world, Vec3i origin, Rotation rot, MultiblockDefinition def) {
            super(world, origin, rot);
            this.def = def;
            this.stockList = new HashMap<>();
        }

        @Override
        public boolean onBlockActivated(Player player, Player.Hand hand, Vec3i offset) {
            if (world.isServer) {
                Vec3i pos = getPos(def.center);
                GuiTypes.CUSTOM_MULTIBLOCK_TRANSPORT.open(player, pos);
            }
            return true;
        }

        @Override
        public int getInvSize(Vec3i offset) {
            return this.def.inventoryWidth * this.def.inventoryHeight +
                    this.getTankCapability(offset) == 0 ? 0 : 2;
        }

        @Override
        public int getTankCapability(Vec3i offset) {
            return this.def.tankCapability;
        }

        @Override
        public boolean isRender(Vec3i offset) {
            return offset.equals(def.center);
        }

        @Override
        public void tick(Vec3i offset) {
            this.ticks += 1;

            if(this.getInvSize(def.center) != 0){
                if (def.itemInputPoints.contains(offset)) {
                    //Handle item(thrown) input
                    if (def.allowThrowInput) {
                        Vec3d vec3d = new Vec3d(getPos(offset));
                        List<ItemStack> stacks = world.getDroppedItems(IBoundingBox.from(
                                vec3d.subtract(0.5, 0.5, 0.5), vec3d.add(1.5, 1.5, 1.5)));
                        ItemStackHandler handler = this.getTile(def.center)
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
                if (def.itemOutputPoint.equals(offset)) {
                    if (def.redstoneControlPoint == null)
                        return;
                    if (world.getRedstone(getPos(def.redstoneControlPoint)) != 0 && def.allowThrowItems) {
                        ItemStackHandler handler = getTile(def.center).getContainer();
                        int slotIndex = handler.getSlotCount() - 1;
                        while (handler.get(slotIndex).getCount() == 0) {
                            slotIndex--;
                            if (slotIndex == -1) {
                                return;
                            }
                        }
                        world.dropItem(handler.extract(slotIndex, def.itemOutputRatioBase, false),
                                new Vec3d(getPos(offset)).add(new Vec3d(0.5, 0.5, 0.5)).add(def.throwPosition
                                        .rotateYaw((float) getTile(def.center).getRotation())));
                        if (ticks % 20 <= def.itemOutputRatioMod) {
                            world.dropItem(handler.extract(slotIndex, 1, false),
                                    new Vec3d(getPos(offset)).add(new Vec3d(0.5, 0.5, 0.5)).add(def.throwPosition
                                            .rotateYaw(this.getRotation())));
                        }
                    }
                }
            }
            if(def.fluidOutputPoint != null && def.fluidOutputPoint.equals(offset) && world.isServer && ticks % 10 == 0){
                final Map<String, FreightTank> refreshList = new HashMap<>();
                for (Vec3i position : fluidOutputPositions) {
                    position = position.add(getPos(offset));
                    for(int i = 0; i < 8; i++){
                        //What if we add an event that is fired when the track map is updated...
                        TileRailBase tile = world.getBlockEntity(position.subtract(0,i,0), TileRailBase.class);
                        if(tile != null) {
                            EntityRollingStock tank = tile.getStockOverhead();
                            if(tank instanceof FreightTank){
                                refreshList.put(tank.getDefinition().name() + "_" + tank.getUUID(), (FreightTank) tank);
                            }
                        }
                    }
                }
                stockList = refreshList;
                System.out.println(stockList);
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
            return def.itemOutputPoint.equals(offset);
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
            return false;
        }

        //Helpers
        public float getRotation(){
            return (float) getTile(def.center).getRotation();
        }
    }
}
