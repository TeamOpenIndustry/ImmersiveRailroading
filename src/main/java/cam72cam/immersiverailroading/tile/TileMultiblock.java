package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.CraftingMachineMode;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.model.animation.ControlPositionMapper;
import cam72cam.immersiverailroading.model.animation.IAnimatable;
import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.immersiverailroading.multiblock.CustomTransporterMultiblock;
import cam72cam.immersiverailroading.multiblock.Multiblock.MultiblockInstance;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.immersiverailroading.net.MultiblockSelectCraftPacket;
import cam72cam.mod.block.BlockEntityTickable;
import cam72cam.mod.energy.Energy;
import cam72cam.mod.energy.IEnergy;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.fluid.FluidTank;
import cam72cam.mod.fluid.ITank;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.ItemStackHandler;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.BlockInfo;
import org.apache.commons.lang3.tuple.Pair;
import util.Matrix4;

import java.util.HashMap;
import java.util.Map;

public class TileMultiblock extends BlockEntityTickable implements IAnimatable {

    @TagField("replaced")
    private BlockInfo replaced;
    @TagField("offset")
    private Vec3i offset;
    @TagField("rotation")
    private Rotation rotation;
    @TagField("name")
    private String name;
    @TagField("craftMode")
    private CraftingMachineMode craftMode = CraftingMachineMode.STOPPED;
    @TagField("isCustom")
    protected boolean isCustom;
    private long ticks;
    private MultiblockInstance mb;

    //Crafting
    @TagField("craftProgress")
    private int craftProgress = 0;
    @TagField("craftItem")
    private ItemStack craftItem = ItemStack.EMPTY;
    @TagField("defaultContainer")
    private ItemStackHandler container = new ItemStackHandler(0);
    @TagField("energyStorage")
    private Energy energy = new Energy(0, 1000);
    @TagField("tank")
    private FluidTank tank = new FluidTank(null, 0);

    public boolean isLoaded() {
        //TODO FIX ME bad init
        return this.name != null && this.name.length() != 0;
    }

    public void configure(String name, Rotation rot, Vec3i offset, BlockInfo replaced) {
        this.name = name;
        this.rotation = rot;
        this.offset = offset;
        this.replaced = replaced;

        this.isCustom = this.getMultiblock() instanceof CustomTransporterMultiblock.TransporterMbInstance;

        //We want to use one tile to handle all inventory/tank, so here's a complex way of wrapping it...
        if (isCustom && !this.offset.equals(((CustomTransporterMultiblock.TransporterMbInstance)this.getMultiblock()).def.center)) {
            //relative center
            Vec3i center = ((CustomTransporterMultiblock.TransporterMbInstance) this.getMultiblock()).def.center;
            //absolute center
            Vec3i pos = getMultiblock().getOrigin().add(center.rotate(rotation));
            //"center" tile
            TileMultiblock targetProxy = getWorld().getBlockEntity(pos, TileMultiblock.class);
            this.container = targetProxy.container;
            this.tank = targetProxy.tank;
        }
        //Call this to refresh status
        load(null);

        markDirty();
    }

    @Override
    public void load(TagCompound nbt) {
        container.onChanged(slot -> this.markDirty());
        container.setSlotLimit(slot -> getMultiblock().getSlotLimit(offset, slot));
        tank.onChanged(this::markDirty);
        energy.onChanged(this::markDirty);
    }

    @Override
    public void update() {
        this.ticks += 1;

        if (offset != null && getMultiblock() != null) {
            this.getMultiblock().tick(offset);
        } else if (ticks > 20) {
            System.out.println("Error in multiblock, reverting");
            getWorld().breakBlock(getPos());
        }
    }

    @Override
    public IBoundingBox getRenderBoundingBox() {
        return IBoundingBox.INFINITE;
    }

    public Vec3i getOrigin() {
        return getPos().subtract(offset.rotate(rotation));
    }

    public MultiblockInstance getMultiblock() {
        if (this.mb == null && this.isLoaded()) {
            if (MultiblockRegistry.get(name) == null) {
                this.breakBlock();
                return null;
            }
            this.mb = MultiblockRegistry.get(name).instance(getWorld(), getOrigin(), rotation);
        }
        return this.mb;
    }

    public String getName() {
        return name;
    }

    public long getRenderTicks() {
        return this.ticks;
    }

    public ItemStackHandler getContainer() {
        if (container.getSlotCount() != getMultiblock().getInvSize(offset)) {
            container.setSize(getMultiblock().getInvSize(offset));
        }

        return this.container;
    }

    public FluidTank getFluidContainer() {
        if (tank.getCapacity() != getMultiblock().getTankCapability(offset)) {
            tank.setCapacity(getMultiblock().getTankCapability(offset));
        }

        //We want to use one tile to handle all inventory/tank, so here's a complex way of wrapping it...
        if (isCustom && !this.offset.equals(((CustomTransporterMultiblock.TransporterMbInstance)this.getMultiblock()).def.center)) {
            //relative center
            Vec3i center = ((CustomTransporterMultiblock.TransporterMbInstance) this.getMultiblock()).def.center;
            //absolute center
            Vec3i pos = getMultiblock().getOrigin().add(center.rotate(rotation));
            TileMultiblock targetProxy = getWorld().getBlockEntity(pos, TileMultiblock.class);
            this.tank = targetProxy.tank;
        }

        return this.tank;
    }

    /*
     * BlockType Functions to pass on to the multiblock
     */

    public void breakBlock() {
        if (getMultiblock() != null) {
            getMultiblock().onBreak();
        }
    }

    public boolean onBlockActivated(Player player, Player.Hand hand) {
        return getMultiblock().onBlockActivated(player, hand, offset);
    }

    /*
     * Event Handlers
     */

    public void onBreakEvent() {
        if(this.isCustom && !this.offset.equals(((CustomTransporterMultiblock.TransporterMbInstance)this.getMultiblock()).def.center)){
            if (replaced != null) {
                getWorld().setBlock(getPos(), replaced);
            }

            return;
        }

        for (int slot = 0; slot < container.getSlotCount(); slot++) {
            ItemStack item = container.get(slot);
            if (!item.isEmpty()) {
                getWorld().dropItem(item, getPos());
            }
        }

        if (replaced != null) {
            getWorld().setBlock(getPos(), replaced);
        }
    }

    public boolean isRender() {
        return getMultiblock().isRender(offset);
    }

    public double getRotation() {
        return 180 - Facing.EAST.rotate(rotation).getAngle();
    }

    public Rotation geFacing() {
        return rotation;
    }

    /*
     * Crafting
     */
    public int getCraftProgress() {
        return craftProgress;
    }

    public void setCraftProgress(int progress) {
        if (craftProgress != progress) {
            craftProgress = progress;
            this.markDirty();
        }
    }

    public CraftingMachineMode getCraftMode() {
        return craftMode;
    }

    public void setCraftMode(CraftingMachineMode mode) {
        if (getWorld().isServer) {
            if (craftMode != mode) {
                craftMode = mode;
                this.markDirty();
            }
        } else {
            new MultiblockSelectCraftPacket(getPos(), craftItem, mode).sendToServer();
        }
    }

    public ItemStack getCraftItem() {
        return craftItem;
    }

    public void setCraftItem(ItemStack selected) {
        if (getWorld().isServer) {
            if (selected == null || !selected.equals(craftItem)) {
                this.craftItem = selected == null ? null : selected.copy();
                this.craftProgress = 0;
                this.markDirty();
            }
        } else {
            new MultiblockSelectCraftPacket(getPos(), selected, craftMode).sendToServer();
        }
    }

    /*
     * Capabilities
     */

    @Override
    public IInventory getInventory(Facing facing) {
        if (this.getMultiblock() == null || this.getMultiblock().getInvSize(offset) == 0) {
            return null;
        }

        //We want to use one tile to handle all inventory/tank, so here's a complex way of wrapping it...
        if (isCustom && !this.offset.equals(((CustomTransporterMultiblock.TransporterMbInstance)this.getMultiblock()).def.center)) {
            //relative center
            Vec3i center = ((CustomTransporterMultiblock.TransporterMbInstance) this.getMultiblock()).def.center;
            //absolute center
            Vec3i pos = getMultiblock().getOrigin().add(center.rotate(rotation));
            TileMultiblock targetProxy = getWorld().getBlockEntity(pos, TileMultiblock.class);
            this.container = targetProxy.container;
        }

        if (container.getSlotCount() != getMultiblock().getInvSize(offset)) {
            container.setSize(getMultiblock().getInvSize(offset));
        }

        return getMultiblock().canInsertItem(offset, 0, null) ? this.container : null;
    }

    @Override
    public ITank getTank(Facing side) {
        if (this.getMultiblock() == null || this.getMultiblock().getTankCapability(offset) == 0) {
            return null;
        }

        //We want to use one tile to handle all inventory/tank, so here's a complex way of wrapping it...
        if (isCustom && !this.offset.equals(((CustomTransporterMultiblock.TransporterMbInstance)this.getMultiblock()).def.center)) {
            //relative center
            Vec3i center = ((CustomTransporterMultiblock.TransporterMbInstance) this.getMultiblock()).def.center;
            //absolute center
            Vec3i pos = getMultiblock().getOrigin().add(center.rotate(rotation));
            TileMultiblock targetProxy = getWorld().getBlockEntity(pos, TileMultiblock.class);
            this.tank = targetProxy.tank;
        }

        if (tank.getCapacity() != getMultiblock().getTankCapability(offset)) {
            tank.setCapacity(getMultiblock().getTankCapability(offset));
        }

        return getMultiblock().canReceiveFluid(offset) ? this.tank : null;
    }

    @Override
    public IEnergy getEnergy(Facing facing) {
        return this.isLoaded() && this.getMultiblock().canRecievePower(offset) ? energy : null;
    }

    @Override
    public void onBreak() {
        try {
            // Multiblock break
            this.breakBlock();
        } catch (Exception ex) {
            ImmersiveRailroading.catching(ex);
            // Something broke
            // TODO figure out why
            getWorld().setToAir(getPos());
        }
    }

    @Override
    public boolean onClick(Player player, Player.Hand hand, Facing facing, Vec3d hit) {
        if (!player.hasPermission(Permissions.MACHINIST)) {
            return false;
        }
        return onBlockActivated(player, hand);
    }

    @Override
    public ItemStack onPick() {
        return ItemStack.EMPTY;
    }

    /*
     * Animations
     */

    @TagSync
    @TagField(value="controlPositions", mapper = ControlPositionMapper.class)
    protected Map<String, Pair<Boolean, Float>> controlPositions = new HashMap<>();

    @Override
    public Matrix4 getModelMatrix() {
        return null;
    }

    @Override
    public float defaultControlPosition(Control<?> control) {
        return 0;
    }

    @Override
    public Pair<Boolean, Float> getControlData(String control) {
        return controlPositions.getOrDefault(control, Pair.of(false, 0f));
    }

    @Override
    public Pair<Boolean, Float> getControlData(Control<?> control) {
        return controlPositions.getOrDefault(control.controlGroup, Pair.of(false, defaultControlPosition(control)));
    }

    @Override
    public float getControlPosition(Control<?> control) {
        return getControlData(control).getRight();
    }

    @Override
    public float getControlPosition(String control) {
        return getControlData(control).getRight();
    }

    @Override
    public void setControlPosition(Control<?> control, float val) {
//        val = Math.min(1, Math.max(0, val));
//        controlPositions.put(control.controlGroup, Pair.of(getControlPressed(control), val));
    }

    @Override
    public void setControlPosition(String control, float val) {
        val = Math.min(1, Math.max(0, val));
        controlPositions.put(control, Pair.of(false, val));
    }

    @Override
    public void setControlPositions(ModelComponentType type, float val) {
//        getDefinition().getModel().getControls().stream().filter(x -> x.part.type == type).forEach(c -> setControlPosition(c, val));
    }

    public Map<String, Pair<Boolean, Float>> getControlPositions() {
        return controlPositions;
    }
}
