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

import java.util.*;

import static cam72cam.immersiverailroading.render.multiblock.CustomMultiblockRender.*;

public class CustomTransporterMultiblock extends Multiblock {
    /*TODO:
     * use Animatrix to Produce animation(I/O for factory)(NOT NOW)
     * REWRITE CURRENT ANIMATION TO FIT MB(NOT NOW)
     * new readouts(NOT NOW)
     * rewrite manual
     */
    private final MultiblockDefinition def;
    private static final List<Vec3i> fluidOutputPositions;
    public static final HashMap<Vec3i, MultiblockPackage> packages;

    static {
        fluidOutputPositions = new ArrayList<>();
        packages = new HashMap<>();
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

    public CustomTransporterMultiblock(MultiblockDefinition def) {
        super(def.name.toUpperCase(), parseStructure(def));
        this.def = def;
        //The center block should be built first...
        componentPositions.remove(def.center);
        componentPositions.add(0, def.center);
    }

    private static FuzzyProvider[][][] parseStructure(MultiblockDefinition def) {
        FuzzyProvider[][][] provider = new FuzzyProvider[def.length][def.height][def.width];
        for (Vec3i entry : def.structure.keySet()) {
            provider[entry.z][entry.y][entry.x] = parseFuzzy(def.structure.get(entry));
        }
        return provider;
    }

    private static FuzzyProvider parseFuzzy(String def) {
        if (Fuzzy.get(def).isEmpty()) {
            switch (def) {
                case "light_engineering_block":
                    return L_ENG();
                case "heavy_engineering_block":
                    return H_ENG();
                case "steel":
                default:
                    return STEEL();
            }
        }
        return () -> Fuzzy.get(def);
    }

    @Override
    public Vec3i placementPos() {
        return def.center;
    }

    @Override
    protected MultiblockInstance newInstance(World world, Vec3i origin, Rotation rot) {
        packages.put(origin.add(def.center.rotate(rot)), new MultiblockPackage(this.def));
        return new TransporterMbInstance(world, origin, rot, this.def);
    }

    public class TransporterMbInstance extends MultiblockInstance {
        public final MultiblockDefinition def;
        private long ticks = 0;
//        public HashMap<String, FreightTank> stockMap;

        public TransporterMbInstance(World world, Vec3i origin, Rotation rot, MultiblockDefinition def) {
            super(world, origin, rot);
            this.def = def;
            packages.get(getPos(def.center)).refreshSelf(this);
        }

        @Override
        public boolean onBlockActivated(Player player, Player.Hand hand, Vec3i offset) {
            if (world.isServer) {
                Vec3i pos = getPos(def.center);
                GuiTypes.CUSTOM_TRANSPORT_MB_GUI.open(player, pos);
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
            return offset.equals(def.center);
        }

        @Override
        public void tick(Vec3i offset) {
            this.ticks += 1;

            if (this.getInvSize(def.center) != 0) {
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
                if (def.center.equals(offset)) {
                    if (def.redstoneControlPoint == null) {
                        if (def.useRedstoneControl)
                            return;

                    }
                    if (def.redstoneControlPoint == null || world.getRedstone(getPos(def.redstoneControlPoint)) != 0 && def.allowThrowItems) {
                        ItemStackHandler handler = getTile(def.center).getContainer();
                        int slotIndex = handler.getSlotCount() - 1;
                        while (handler.get(slotIndex).getCount() == 0) {
                            slotIndex--;
                            if (slotIndex == -1) {
                                return;
                            }
                        }
                        Vec3d vec3d = new Vec3d(def.itemOutputPoint.internal());
                        switch ((int) (this.getRotation() + 90)) {
                            case 180:
                                vec3d = vec3d.add(-1, 0, -1);
                                break;
                            case 90:
                                vec3d = vec3d.add(east);
                                break;
                            case -90:
                                vec3d = vec3d.add(west);
                        }
                        world.dropItem(handler.extract(slotIndex, def.itemOutputRatioBase, false),
                                vec3d.rotateYaw(this.getRotation() + 90).add(origin),
                                def.initialVelocity.rotateYaw(this.getRotation() + 90).scale(0.05));
                        if (ticks % 20 <= 0) {
                            world.dropItem(handler.extract(slotIndex, 1, false),
                                    vec3d.rotateYaw(this.getRotation() + 90).add(origin),
                                    def.initialVelocity.rotateYaw(this.getRotation() + 90).scale(0.05));
                        }
                    }
                }
            }

            //Handle fluid output
            if (def.fluidOutputPoint != null && def.center.equals(offset) && world.isServer && ticks % 10 == 0) {
                packages.get(getPos(def.center)).refreshSelf(this);
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
            return def.itemOutputPoint.equals(new Vec3d(offset));
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
        public float getRotation() {
            return (float) getTile(def.center).getRotation();
        }
    }

    public static class MultiblockPackage {
        public HashMap<String, FreightTank> stockMap = new HashMap<>();
        public HashMap<String, String> guiMap = new HashMap<>();
        public List<Vec3i> trackList = new LinkedList<>();
        public FreightTank target;
        public int status = 0;//0 is N/A, 1 is true, 2 is false
        public boolean autoFill = false;

        public MultiblockPackage(MultiblockDefinition def) {
            if(def.autoFillTanks.equals("true")){
                status = 1;
                autoFill = true;
            }else if(def.autoFillTanks.equals("false")){
                status = 2;
            }
        }

        public void setTarget(String name) {
            this.target = stockMap.getOrDefault(name, null);
        }

        public boolean setAutoFill(boolean drain) {
            if(status == 0) {
                this.autoFill = drain;
                return true;
            }
            return false;
        }

        public void refreshSelf(TransporterMbInstance instance) {
            final HashMap<String, FreightTank> refreshStockMap = new HashMap<>();
            final HashMap<String, String> refreshGuiMap = new HashMap<>();
            final List<Vec3i> refreshTrackList = new LinkedList<>();
            final Vec3i absPos = instance.getPos(instance.def.fluidOutputPoint);
            final Rotation rot1 = instance.rot == Rotation.COUNTERCLOCKWISE_90
                    ? Rotation.CLOCKWISE_90
                    : instance.rot == Rotation.CLOCKWISE_90
                    ? Rotation.COUNTERCLOCKWISE_90
                    : instance.rot;//Rotate it back

            //What if we add an event that is fired when the track map is updated...
            for (Vec3i position : fluidOutputPositions) {
                position = position.add(absPos);
                TileRailBase tile = instance.world.getBlockEntity(position, TileRailBase.class);
                if (tile != null) {
                    refreshTrackList.add(position.subtract(instance.getPos(Vec3i.ZERO)).rotate(rot1));
                    EntityRollingStock tank = tile.getStockOverhead();
                    if (tank instanceof FreightTank && ((FreightTank) tank).getCurrentSpeed().metric() <= 1) {
                        refreshStockMap.put(tank.getDefinition().name() + "_" + tank.getUUID().toString().substring(0, 6),
                                (FreightTank) tank);
                        refreshGuiMap.put(tank.getDefinition().name() + "_" + tank.getUUID().toString().substring(0, 6),
                                tank.getDefinition().name() + "_" + tank.getUUID().toString().substring(0, 6));
                    }
                }
            }

            this.stockMap = refreshStockMap;
            this.guiMap = refreshGuiMap;
            this.trackList = refreshTrackList;

            if (instance.def.autoFillTanks == null) {//Controlled by gui
                if (autoFill) {
                    this.stockMap.values().forEach(stock ->
                            instance.getTile(instance.def.center).getFluidContainer().fill(stock.theTank, 300, false));
                } else if (this.target != null) {
                    instance.getTile(instance.def.center).getFluidContainer().fill(this.target.theTank, 300, false);
                }
            } else {//Controlled by json
                if (instance.def.autoFillTanks.equals("true")) {
                    this.stockMap.values().forEach(stock ->
                            instance.getTile(instance.def.center).getFluidContainer().fill(stock.theTank, 300, false));
                } else if (instance.def.autoFillTanks.equals("false")) {
                    instance.getTile(instance.def.center).getFluidContainer().fill(this.target.theTank, 300, false);
                }
            }
            if (!this.stockMap.containsValue(this.target)) {
                this.target = null;
            }
        }
    }
}
