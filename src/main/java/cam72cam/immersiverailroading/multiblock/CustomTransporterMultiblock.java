package cam72cam.immersiverailroading.multiblock;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.FreightTank;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.MultiblockDefinition;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.fluid.ITank;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.ItemStackHandler;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;
import util.Matrix4;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Added a way of customizing your own multiblocks
 */
public class CustomTransporterMultiblock extends Multiblock {
    /*
     * TODO
     * use Animatrix to Produce animation(I/O)(DONE)
     * REWRITE CURRENT ANIMATION TO FIT MB(DONE)
     * new readouts(DONE)
     * rewrite animation panel
     */
    private final MultiblockDefinition def;
    public static final HashMap<Vec3i, MultiblockDataSaver> storages = new HashMap<>();
    private final List<Vec3i> fluidOutputPositions;//All relative possible positions for searching

    public CustomTransporterMultiblock(MultiblockDefinition def) {
        super(def.name, parseStructure(def));
        this.def = def;
        //The center block should be built first...
        componentPositions.remove(def.center);
        componentPositions.add(0, def.center);

        fluidOutputPositions = new ArrayList<>();
        int bound = (int) Math.ceil(def.interactRadius);
        float distance = def.interactRadius * def.interactRadius;
        //Store the possible relative poses for fluid output in order to avoid more calculation
        for (int x = -bound; x <= bound; x++) {
            for (int y = 0; y > -8; y--) {
                for (int z = -bound; z <= bound; z++) {
                    if (x * x + z * z <= distance) {
                        fluidOutputPositions.add(new Vec3i(x, y, z));
                    }
                }
            }
        }
    }

    //(-x, z, y)
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
        //We have to use this to save additional data here...
        storages.put(origin.add(def.center.rotate(rot)), new MultiblockDataSaver(this.def));
        return new TransporterMbInstance(world, origin, rot, this.def);
    }

    public class TransporterMbInstance extends MultiblockInstance {
        public final MultiblockDefinition def;
        private long ticks = 0;

        private int transportCooldown;
        private boolean shouldInitialize = true;

        public TransporterMbInstance(World world, Vec3i origin, Rotation facing, MultiblockDefinition def) {
            super(world, origin, facing);
            this.def = def;
            storages.get(getPos(def.center)).onTick(this, false);
        }

        @Override
        public boolean onBlockActivated(Player player, Player.Hand hand, Vec3i offset) {
            if (world.isClient) {
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
            //Only render the center block
            return offset.equals(def.center);
        }

        @Override
        public void tick(Vec3i offset) {
            this.ticks += 1;

            //As the tile isn't created in the constructor we have to init here
            if (shouldInitialize && def.center.equals(offset)) {
                def.animations.keySet().stream()
                        .filter(s -> !getTile(offset).getControlPositions().containsKey(s))
                        .forEach(s -> getTile(offset).setControlPosition(s, 0));
                shouldInitialize = false;
            }

            //If it has inventory then attempt to handle items
            if (def.inventoryHeight * def.inventoryWidth != 0) {
                if (def.itemInputPoints.contains(offset) && def.allowThrowInput) {
                    receiveItems(offset);
                }

                if (def.center.equals(offset)) {
                    extractItems(offset);
                }
            }

            //If it has tank then attempt to handle fluid
            if (def.tankCapability != 0) {
                //Handle fluid interaction with stock
                if (!def.fluidHandlePoints.isEmpty() && def.center.equals(offset) && ticks % 10 == 0) {
                    storages.get(getPos(def.center)).onTick(this, true);
                }

                //Handle fluid interaction with pipe
                if (!def.isFluidToStocks && def.fluidHandlePoints.contains(offset)) {
                    Vec3i pos = getPos(offset);
                    List<ITank> tanks = new LinkedList<>();
                    for (Facing facing : Facing.values()) {
                        if (!world.hasBlockEntity(pos.offset(facing), TileMultiblock.class) && world.getTank(pos.offset(facing)) != null) {
                            tanks.addAll(world.getTank(pos.offset(facing)));
                        }
                    }

                    tanks.forEach(t -> t.drain(getTile(def.center).getFluidContainer(), 50, false));
                }
            }
        }

        private void receiveItems(Vec3i offset) {
            Vec3d vec3d = new Vec3d(getPos(offset));
            List<ItemStack> stacks = world.getDroppedItems(IBoundingBox.from(
                    vec3d.subtract(0.5, 0.5, 0.5), vec3d.add(1.5, 1.5, 1.5)));
            ItemStackHandler handler = this.getTile(def.center)
                    .getContainer();
            if (!stacks.isEmpty()) {
                //Basic merge function
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

        private void extractItems(Vec3i offset) {
            transportCooldown--;

            if ((def.redstoneControlPoint == null || world.getRedstone(getPos(def.redstoneControlPoint)) != 0) //Have valid RS input
                    && def.allowThrowOutput) {
                ItemStackHandler handler = getTile(def.center).getContainer();
                int slotIndex = handler.getSlotCount() - 1;
                while (handler.get(slotIndex).getCount() == 0) {
                    slotIndex--;
                    if (slotIndex == -1) {
                        //Empty inventory, stop transport
                        getTile(offset).setControlPosition("items", 0);
                        return;
                    }
                }
                Vec3d vec3d = new Vec3d(def.itemOutputPoint.x, def.itemOutputPoint.y, def.itemOutputPoint.z);
                switch ((int) (this.getRotation() + 90)) {
                    case 180:
                        vec3d = vec3d.add(-1, 0, -1);
                        break;
                    case 90:
                        vec3d = vec3d.add(-1, 0, 0);
                        break;
                    case -90:
                    case 270:
                        vec3d = vec3d.add(0, 0, -1);
                }

                getTile(offset).setControlPosition("items", 1);

                if (def.itemAnimation != null) {
                    if (world.isServer) {
                        if (Math.abs(def.animations.get("items").getPercent(getTile(offset), 0) - def.transportPercent) <= 0.05
                                && transportCooldown <= 0) {
                            world.dropItem(handler.extract(slotIndex, def.transportAmount, false),
                                    vec3d.rotateYaw(this.getRotation() + 90).add(origin),
                                    def.initialVelocity.rotateYaw(this.getRotation() + 90).scale(0.05));
                            transportCooldown = (int) (def.itemAnimFrameCount / def.itemAnimation.frames_per_tick) - 4;
                        }
                    }
                } else {
                    if (def.itemOutputRatioBase != 0) {
                        world.dropItem(handler.extract(slotIndex, def.itemOutputRatioBase, false),
                                vec3d.rotateYaw(this.getRotation() + 90).add(origin),
                                def.initialVelocity.rotateYaw(this.getRotation() + 90).scale(0.05));
                    }
                    if (ticks % 20 <= def.itemOutputRatioMod) {
                        world.dropItem(handler.extract(slotIndex, 1, false),
                                vec3d.rotateYaw(this.getRotation() + 90).add(origin),
                                def.initialVelocity.rotateYaw(this.getRotation() + 90).scale(0.05));
                    }
                }
            } else {
                //Invalid RS input/don't allow output, stop transport
                getTile(offset).setControlPosition("items", 0);
            }
        }

        @Override
        public boolean canInsertItem(Vec3i offset, int slot, ItemStack stack) {
            return def.itemInputPoints.stream().anyMatch(offset::equals);
        }

        @Override
        public boolean canReceiveFluid(Vec3i offset) {
            if (def.isFluidToStocks) {
                return def.fluidHandlePoints.stream().anyMatch(offset::equals);
            }
            return false;
        }

        @Override
        public boolean isItemOutputSlot(Vec3i offset, int slot) {
            return def.itemOutputPoint.equals(new Vec3d(offset));
        }

        @Override
        public boolean isFluidOutputSlot(Vec3i offset) {
            if (def.isFluidToStocks) {
                return false;
            }
            return def.fluidHandlePoints.stream().anyMatch(offset::equals);
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

    //A hack to storage some extra information with the instance
    public class MultiblockDataSaver {
        private final MultiblockDefinition def;
        private final Set<Vec3i> possibleTrackPositions;
        private Set<Vec3i> stockFluidHandlerPoints;

        public HashMap<String, FreightTank> stockMap = new HashMap<>();
        public HashMap<String, String> guiMap = new HashMap<>();
        public List<Vec3i> trackList = new LinkedList<>();
        public FreightTank targetStock;
        public int fluidStatus = 0;//Autofill tanks wrapper: 0 is N/A(Manual mode), 1 is true, 2 is false
        public boolean autoInteract = false;

        public MultiblockDataSaver(MultiblockDefinition def) {
            this.def = def;
            this.stockFluidHandlerPoints = new HashSet<>();
            this.possibleTrackPositions = new HashSet<>();

            if (def.autoInteractWithStocks == null) {
                fluidStatus = 0;
            } else if (def.autoInteractWithStocks.equals("true")) {
                fluidStatus = 1;
                autoInteract = true;
            } else if (def.autoInteractWithStocks.equals("false")) {
                fluidStatus = 2;
            }
        }

        public void setTargetStock(String name) {
            this.targetStock = stockMap.getOrDefault(name, null);
        }

        public boolean shouldAutoInteract(boolean autoFill) {
            if (fluidStatus == 0) {
                this.autoInteract = autoFill;
                return true;
            }
            return false;
        }

        public void onTick(TransporterMbInstance instance, boolean handle) {
            final HashMap<String, FreightTank> refreshedStockMap = new HashMap<>();
            final HashMap<String, String> refreshedGuiStringMap = new HashMap<>();
            final List<Vec3i> refreshedTrackList = new LinkedList<>();
            final Rotation invertedRot = instance.rot == Rotation.COUNTERCLOCKWISE_90
                    ? Rotation.CLOCKWISE_90
                    : instance.rot == Rotation.CLOCKWISE_90
                    ? Rotation.COUNTERCLOCKWISE_90
                    : instance.rot;//Rotate back

            //What if we add an event that is fired when the track map is updated...
            for (Vec3i position : possibleTrackPositions) {
                position = position.rotate(instance.rot).add(instance.origin);
                TileRailBase tile = instance.world.getBlockEntity(position, TileRailBase.class);
                if (tile != null) {
                    refreshedTrackList.add(position.subtract(instance.getPos(Vec3i.ZERO)).rotate(invertedRot));
                    EntityRollingStock tank = tile.getStockOverhead();
                    if (tank instanceof FreightTank && ((FreightTank) tank).getCurrentSpeed().metric() <= 1) {
                        refreshedStockMap.put(tank.getDefinition().name() + "_" + tank.getUUID().toString().substring(0, 6),
                                (FreightTank) tank);
                        refreshedGuiStringMap.put(tank.getDefinition().name() + "_" + tank.getUUID().toString().substring(0, 6),
                                tank.getDefinition().name() + "_" + tank.getUUID().toString().substring(0, 6));
                    }
                }
            }

            this.stockMap = refreshedStockMap;
            this.guiMap = refreshedGuiStringMap;
            this.trackList = refreshedTrackList;

            if (!this.stockMap.containsValue(this.targetStock)) {
                this.targetStock = null;
            }

            if (handle) {//Avoid NullPointerException caused by initialization
                if (instance.world.isServer) {
                    refreshTrackPositions(instance.getTile(instance.def.center));
                }
                if (instance.def.isFluidToStocks) {//Output to stock
                    if (fluidStatus == 0) {//Output controlled by gui
                        if (autoInteract) {//Output to all available tanks
                            this.stockMap.values().forEach(stock ->
                                    instance.getTile(instance.def.center).getFluidContainer().fill(stock.theTank, 300, false));
                        } else if (this.targetStock != null) {
                            instance.getTile(instance.def.center).getFluidContainer().fill(this.targetStock.theTank, 300, false);
                        }
                    } else {//Output controlled by json
                        if (fluidStatus == 1) {//Should automatically fill the stocks
                            this.stockMap.values().forEach(stock ->
                                    instance.getTile(instance.def.center).getFluidContainer().fill(stock.theTank, 300, false));
                        } else {
                            instance.getTile(instance.def.center).getFluidContainer().fill(this.targetStock.theTank, 300, false);
                        }
                    }
                } else {//Output to pipe(input from stocks)
                    if (fluidStatus == 0) {//Input controlled by gui
                        if (autoInteract) {
                            this.stockMap.values().forEach(stock ->
                                    instance.getTile(instance.def.center).getFluidContainer().drain(stock.theTank, 300, false));
                        } else if (this.targetStock != null) {
                            instance.getTile(instance.def.center).getFluidContainer().drain(this.targetStock.theTank, 300, false);
                        }
                    } else {//Input controlled by json
                        if (fluidStatus == 1) {//Should automatically fill from stocks
                            this.stockMap.values().forEach(stock ->
                                    instance.getTile(instance.def.center).getFluidContainer().drain(stock.theTank, 300, false));
                        } else {
                            instance.getTile(instance.def.center).getFluidContainer().drain(this.targetStock.theTank, 300, false);
                        }
                    }
                }
            }
        }


        //When we placed/broke some track we need to refresh their poses
        public void refreshTrackPositions(TileMultiblock tile) {
            Set<Vec3i> vecs = def.model.fluidHandlerPoints.stream()
                    .map(component -> {
                        Matrix4 matrix = def.model.state.getGroupMatrix(tile, component.key, 0);
                        return matrix != null ? matrix.apply(component.center) : new Matrix4().setIdentity().apply(component.center);
                    })
                    .map(vec3d -> new Vec3i(-vec3d.x, vec3d.y, -vec3d.z))
                    .collect(Collectors.toSet());
            if (vecs.stream().anyMatch(vec3i -> !stockFluidHandlerPoints.contains(vec3i))) {
                this.stockFluidHandlerPoints = vecs;
                possibleTrackPositions.clear();
                this.stockFluidHandlerPoints.forEach(vec -> fluidOutputPositions.stream()
                        .map(vec3i -> vec3i.add(new Vec3i(vec.x, vec.y, vec.z)))
                        .forEach(possibleTrackPositions::add));
            }
        }
    }
}