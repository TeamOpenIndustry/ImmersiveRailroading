package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.inventory.FilteredStackHandler;
import cam72cam.immersiverailroading.inventory.SlotFilter;
import cam72cam.immersiverailroading.library.CraftingMachineMode;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.multiblock.CustomTransporterMultiblock;
import cam72cam.immersiverailroading.multiblock.Multiblock.MultiblockInstance;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.immersiverailroading.net.MultiblockSelectCraftPacket;
import cam72cam.mod.block.BlockEntityTickable;
import cam72cam.mod.energy.Energy;
import cam72cam.mod.energy.IEnergy;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
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

public class TileMultiblock extends BlockEntityTickable {

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
    @TagField("container")
    private ItemStackHandler container = new ItemStackHandler(0);
    @TagField("energyStorage")
    private Energy energy = new Energy(0, 1000);
    @TagField("bucketContainer")
    private FilteredStackHandler bucketContainer = new FilteredStackHandler(2);
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
            this.bucketContainer = targetProxy.bucketContainer;
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
        bucketContainer.filter.put(0, SlotFilter.FLUID_CONTAINER);
        bucketContainer.filter.put(1, SlotFilter.FLUID_CONTAINER);
        tank.onChanged(this::markDirty);
        energy.onChanged(this::markDirty);
    }

    @Override
    public void update() {
        this.ticks += 1;

        this.isCustom = this.getMultiblock() instanceof CustomTransporterMultiblock.TransporterMbInstance;

        if (tank.getCapacity() != 0) {
            ItemStack input = bucketContainer.get(0);

            final ItemStack[] inputCopy = {input.copy()};
            inputCopy[0].setCount(1);
            ITank inputTank = ITank.getTank(inputCopy[0], (ItemStack stack) -> inputCopy[0] = stack);

            if (input.getCount() > 0) {

                for (Boolean doFill : new Boolean[]{false, true}) {
                    boolean success;
                    if (doFill) {
                        success = tank.drain(inputTank, tank.getCapacity(), true) > 0;
                    } else {
                        success = tank.fill(inputTank, tank.getCapacity(), true) > 0;
                    }

                    if (success) {
                        ItemStack out = inputCopy[0].copy();
                        if (this.bucketContainer.insert(1, out, true).getCount() == 0) {
                            if (doFill) {
                                tank.drain(inputTank, tank.getCapacity(), false);
                            } else {
                                tank.fill(inputTank, tank.getCapacity(), false);
                            }
                                // Decrease input
                            bucketContainer.extract(0, 1, false);

                                // Increase output
                                this.bucketContainer.insert(1, out, false);
                                break;
                        }
                    }
                }

            }
        }

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

    public ItemStackHandler getBucketContainer() {
        return this.bucketContainer;
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
            this.bucketContainer = targetProxy.bucketContainer;
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
            this.bucketContainer = targetProxy.bucketContainer;
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
}
