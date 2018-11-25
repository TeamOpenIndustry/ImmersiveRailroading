package cam72cam.immersiverailroading.tile;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.Config.ConfigBalance;
import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.Freight;
import cam72cam.immersiverailroading.entity.FreightTank;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.entity.Tender;
import cam72cam.immersiverailroading.library.Augment;
import cam72cam.immersiverailroading.library.CouplerAugmentMode;
import cam72cam.immersiverailroading.library.LocoControlMode;
import cam72cam.immersiverailroading.library.StockDetectorMode;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.physics.MovementTrack;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.ParticleUtil;
import cam72cam.immersiverailroading.util.RedstoneUtil;
import cam72cam.immersiverailroading.util.SwitchUtil;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk.EnumCreateEntityType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import trackapi.lib.ITrack;

public class TileRailBase extends SyncdTileEntity implements ITrack, ITickable {
	public static TileRailBase get(IBlockAccess world, BlockPos pos) {
		SyncdTileEntity te = SyncdTileEntity.get(world, pos, EnumCreateEntityType.IMMEDIATE);
		return te instanceof TileRailBase ? (TileRailBase)te : null;
	}
	
	private BlockPos parent;
	private float bedHeight = 0;
	private float railHeight = 0;
	private Augment augment; 
	private String augmentFilterID;
	private int snowLayers = 0;
	protected boolean flexible = false;
	private boolean willBeReplaced = false; 
	private NBTTagCompound replaced;
	private boolean skipNextRefresh = false;
	public ItemStack railBedCache = null;
	private FluidTank augmentTank = null;
	private int redstoneLevel = 0;
	private StockDetectorMode redstoneMode = StockDetectorMode.SIMPLE;
	private LocoControlMode controlMode = LocoControlMode.THROTTLE_FORWARD;
	private CouplerAugmentMode couplerMode = CouplerAugmentMode.ENGAGED;
	private int clientLastTankAmount = 0;
	private long clientSoundTimeout = 0;
	private int ticksExisted;
	public boolean blockUpdate;
	
	@Override
	public boolean isLoaded() {
		return !world.isRemote || hasTileData;
	}

	public void setBedHeight(float height) {
		this.bedHeight = height;
	}
	public float getBedHeight() {
		return this.bedHeight;
	}
	public void setRailHeight(float height) {
		this.railHeight = height;
	}
	public float getRailHeight() {
		return this.railHeight;
	}
	
	public void setAugment(Augment augment) {
		this.augment = augment;
		setAugmentFilter(null);
		this.markDirty();
	}
	public boolean setAugmentFilter(String definitionID) {
		if (definitionID != augmentFilterID) {
			this.augmentFilterID = definitionID;
		} else {
			this.augmentFilterID = null;
		}
		this.markDirty();
		return this.augmentFilterID != null;
	}
	public String nextAugmentRedstoneMode() {
		if (this.augment == null) {
			return null;
		}
		switch(this.augment) {
		case DETECTOR:
			redstoneMode = StockDetectorMode.values()[((redstoneMode.ordinal() + 1) % (StockDetectorMode.values().length))];
			return redstoneMode.toString();
		case LOCO_CONTROL:
			controlMode = LocoControlMode.values()[((controlMode.ordinal() + 1) % (LocoControlMode.values().length))];
			return controlMode.toString();
		case COUPLER:
			couplerMode = CouplerAugmentMode.values()[((couplerMode.ordinal() + 1) % (CouplerAugmentMode.values().length))];
			return couplerMode.toString();
		default:
			return null;
		}
	}
	public Augment getAugment() {
		return this.augment;
	}
	public int getSnowLayers() {
		return this.snowLayers;
	}
	public void setSnowLayers(int snowLayers) {
		this.snowLayers = snowLayers;
		this.markDirty();
	}
	public float getFullHeight() {
		return this.bedHeight + this.snowLayers / 8.0f;
	}
	
	public boolean handleSnowTick() {
		if (this.snowLayers < (ConfigDebug.deepSnow ? 8 : 1)) {
			this.snowLayers += 1;
			this.markDirty();
			return true;
		}
		return !ConfigDebug.deepSnow;
	}

	public BlockPos getParent() {
		if (parent == null) {
			if (ticksExisted > 1 && !world.isRemote) {
				ImmersiveRailroading.warn("Invalid block without parent");
				// Might be null during init
				world.setBlockToAir(pos);
			}
			return null;
		}
		return parent.add(pos);
	}
	public void setParent(BlockPos pos) {
		this.parent = pos.subtract(this.pos);
	}
	
	public boolean isFlexible() {
		return this.flexible;
	}
	
	public ItemStack getRenderRailBed() {
		if (railBedCache == null) {
			TileRail pt = this.getParentTile();
			if (pt != null) {
				railBedCache = pt.info.settings.railBed;
			}
		}
		return railBedCache;
	}
	
	@Override
	public void writeUpdateNBT(NBTTagCompound nbt) {
		if (this.getRenderRailBed() != null) {
			nbt.setTag("renderBed", this.getRenderRailBed().serializeNBT());
		}
	}
	@Override
	public void readUpdateNBT(NBTTagCompound nbt) {
		if (nbt.hasKey("renderBed")) {
			this.railBedCache = new ItemStack(nbt.getCompoundTag("renderBed"));
		}
		if (this.augmentTank != null && this.augment == Augment.WATER_TROUGH) {
			int delta = clientLastTankAmount - this.augmentTank.getFluidAmount();
			if (delta > 0) {
				// We lost water, do spray
				// TODO, this fires during rebalance which is not correct
				for (int i = 0; i < delta/10; i ++) {
					for (EnumFacing facing : EnumFacing.HORIZONTALS) {
						ParticleUtil.spawnParticle(world, EnumParticleTypes.WATER_SPLASH, new Vec3d(pos.offset(facing)).addVector(0.5, 0.5, 0.5));
					}
				}
				if (clientSoundTimeout < world.getWorldTime()) {
					world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1, 1, false);
					clientSoundTimeout = world.getWorldTime() + 10;
				}
			}
			clientLastTankAmount = this.augmentTank.getFluidAmount();
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		
		int version = 0;
		if (nbt.hasKey("version")) {
			version = nbt.getInteger("version");
		}
		
		
		railHeight = bedHeight = nbt.getFloat("height");
		snowLayers = nbt.getInteger("snowLayers");
		flexible = nbt.getBoolean("flexible");
		if (nbt.hasKey("replaced")) {
			replaced = nbt.getCompoundTag("replaced");
		}
		
		if (nbt.hasKey("augment")) {
			augment = Augment.values()[nbt.getInteger("augment")];
		}

		parent = BlockPos.fromLong(nbt.getLong("parent"));

		switch(version) {
		case 0:
			//NOP
		case 1:
			parent = parent.subtract(pos);
		case 2:
			// Nothing in base
		case 3:
			// Nothing yet ...
		}

		if (nbt.hasKey("augmentTank")) {
			createAugmentTank();
			augmentTank.readFromNBT(nbt.getCompoundTag("augmentTank"));			
		}
		if (nbt.hasKey("augmentFilterID")) {
			augmentFilterID = nbt.getString("augmentFilterID");
		}
		if (nbt.hasKey("redstoneMode")) {
			redstoneMode = StockDetectorMode.values()[nbt.getInteger("redstoneMode")];
		}
		if (nbt.hasKey("controlMode")) {
			controlMode = LocoControlMode.values()[nbt.getInteger("controlMode")];
		}
		if (nbt.hasKey("couplerMode")) {
			couplerMode = CouplerAugmentMode.values()[nbt.getInteger("couplerMode")];
		}
		if (nbt.hasKey("railHeight")) {
			railHeight = nbt.getFloat("railHeight");
		}
	}
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setLong("parent", parent.toLong());
		nbt.setFloat("height", bedHeight);
		nbt.setFloat("railHeight", railHeight);
		nbt.setInteger("snowLayers", snowLayers);
		nbt.setBoolean("flexible", flexible);
		if (replaced != null) {
			nbt.setTag("replaced", replaced);
		}
		
		if (augment != null) {
			nbt.setInteger("augment", this.augment.ordinal());
			if (augmentTank != null) {
				nbt.setTag("augmentTank", augmentTank.writeToNBT(new NBTTagCompound()));
			}
			if (augmentFilterID != null) {
				nbt.setString("augmentFilterID", augmentFilterID);
			}
		}
		nbt.setInteger("redstoneMode", redstoneMode.ordinal());
		nbt.setInteger("controlMode", controlMode.ordinal());
		nbt.setInteger("couplerMode", couplerMode.ordinal());
		
		nbt.setInteger("version", 3);
		
		
		return super.writeToNBT(nbt);
	}
	
	public TileRail getParentTile() {
		if (this.getParent() == null) {
			return null;
		}
		TileRail te = TileRail.get(world, this.getParent());
		if (te == null || !te.isLoaded()) {
			return null;
		}
		return te;
	}
	public void setReplaced(NBTTagCompound replaced) {
		this.replaced = replaced;
	}
	public NBTTagCompound getReplaced() {
		return replaced;
	}
	
	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		// This works around a hack where Chunk does a removeTileEntity directly after calling breakBlock
		// We have already removed the original TE and are replacing it with one which goes with a new block 
		if (this.skipNextRefresh ) {
			return false;
		}
		return super.shouldRefresh(world, pos, oldState, newState);
	}
	
	// Called before flex track replacement
	public void setWillBeReplaced(boolean value) {
		this.willBeReplaced = value;
	}
	// Called duing flex track replacement
	public boolean getWillBeReplaced() {
		return this.willBeReplaced;
	}

	public void cleanSnow() {
		int snow = this.getSnowLayers();
		if (snow > 1) {
			this.setSnowLayers(1);
			int snowDown = snow -1;
			for (int i = 1; i <= 3; i ++) {
				EnumFacing[] horiz = EnumFacing.HORIZONTALS.clone();
				if (Math.random() > 0.5) {
					// Split between sides of the track
					ArrayUtils.reverse(horiz);
				}
				for (EnumFacing facing : horiz) {
					BlockPos ph = world.getPrecipitationHeight(pos.offset(facing, i));
					for (int j = 0; j < 3; j ++) {
						IBlockState state = world.getBlockState(ph);
						if (world.isAirBlock(ph) && !BlockUtil.isRail(world, ph.down())) {
							world.setBlockState(ph, Blocks.SNOW_LAYER.getDefaultState().withProperty(BlockSnow.LAYERS, snowDown));
							return;
						}
						if (world.getBlockState(ph).getBlock() == Blocks.SNOW) {
							ph = ph.up();
							continue;
						}
						if (world.getBlockState(ph).getBlock() == Blocks.SNOW_LAYER) {
							Integer currSnow = state.getValue(BlockSnow.LAYERS);
							if (currSnow == 8) {
								ph = ph.up();
								continue;
							}
							int toAdd = Math.min(8 - currSnow, snowDown);
							world.setBlockState(ph, state.withProperty(BlockSnow.LAYERS, currSnow + toAdd));
							snowDown -= toAdd;
							if (snowDown <= 0) {
								return;
							}
						}
						ph = ph.down();
					}
				}
			}
		}
	}
	
	@Override
	public double getTrackGauge() {
		TileRail parent = this.getParentTile();
		if (parent != null) {
			return parent.info.settings.gauge.value();
		}
		return 0;
	}
	
	@Override
	public Vec3d getNextPosition(Vec3d currentPosition, Vec3d motion) {
		TileRail tile;
		TileRail self = this instanceof TileRail ? (TileRail) this : this.getParentTile();
		
		if (self == null) {
			return currentPosition;
		}

		SwitchState state = SwitchUtil.getSwitchState(self, currentPosition);
		if (state == SwitchState.STRAIGHT) {
			tile = self.getParentTile();
		} else {
			tile = self;
		}

		if (tile == null) {
			return currentPosition;
		}
		
		double distanceMeters = motion.lengthVector();
		float rotationYaw = VecUtil.toYaw(motion);

		Vec3d nextPos = MovementTrack.nextPosition(world, currentPosition, tile, rotationYaw, distanceMeters);

		if (state != SwitchState.NONE && nextPos.distanceTo(currentPosition) > Math.abs(distanceMeters) * 2) {
			tile = self.getParentTile();
			if (tile != null) {
				Vec3d potential = MovementTrack.nextPosition(world, currentPosition, tile, rotationYaw, distanceMeters);
				if (potential.distanceTo(currentPosition) < nextPos.distanceTo(currentPosition)) {
					nextPos = potential;
				}
			}
		}

		return nextPos;
	}
	
	/*
	 * Capabilities tie ins
	 */
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if (this.getAugment() != null) {
			switch(this.getAugment()) {
			case FLUID_LOADER:
			case FLUID_UNLOADER:
			case WATER_TROUGH:
				return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
			case ITEM_LOADER:
			case ITEM_UNLOADER:
				return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
			case DETECTOR:
			case LOCO_CONTROL:
			case SPEED_RETARDER:
			case COUPLER:
				break;
			default:
				break;
			}
		}
		return super.hasCapability(capability, facing);
	}
	
	public <T extends EntityRollingStock> T getStockNearBy(Class<T> type, Capability<?> capability){
		AxisAlignedBB bb = new AxisAlignedBB(this.pos.south().west(), this.pos.up(3).east().north());
		List<T> stocks = this.world.getEntitiesWithinAABB(type, bb);
		for (T stock : stocks) {
			if (capability == null || stock.hasCapability(capability, null)) {
				if (augmentFilterID == null || augmentFilterID.equals(stock.getDefinitionID())) {
					return stock;
				}
			}
		}
		return null;
	}
	
	public EntityMoveableRollingStock getStockNearBy(Capability<?> capability){
		return getStockNearBy(EntityMoveableRollingStock.class, capability);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (this.getAugment() != null) {
			switch(this.getAugment()) {
			case FLUID_LOADER:
			case FLUID_UNLOADER:
			case WATER_TROUGH:
				if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
					if (this.augmentTank == null) {
						this.createAugmentTank();
					}
					return (T) this.augmentTank;
				}
			case ITEM_LOADER:
			case ITEM_UNLOADER:
				if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
					EntityMoveableRollingStock stock = getStockNearBy(capability);
					if (stock != null) {
						return stock.getCapability(capability, null);
					}
					return (T) new ItemStackHandler(0);
				}
			case DETECTOR:
			case LOCO_CONTROL:
			case SPEED_RETARDER:
			case COUPLER:
				break;
			default:
				break;
			}
		}
		return super.getCapability(capability, facing);
	}
	
	private void balanceTanks() {
		for (EnumFacing facing : EnumFacing.HORIZONTALS) {
			TileRailBase neighbor = TileRailBase.get(world, pos.offset(facing));
			if (neighbor != null && neighbor.augmentTank != null) {
				if (neighbor.augmentTank.getFluidAmount() + 1 < augmentTank.getFluidAmount()) {
					transferAllFluid(augmentTank, neighbor.augmentTank, (augmentTank.getFluidAmount() - neighbor.augmentTank.getFluidAmount())/2);
					this.markDirty();
				}
			}
		}
	}
	
	private void createAugmentTank() {
		switch(this.augment) {
		case FLUID_LOADER:
		case FLUID_UNLOADER:
			this.augmentTank = new FluidTank(1000);
			break;
		case WATER_TROUGH:
			this.augmentTank = new FluidTank(FluidRegistry.WATER, 0, 1000) {
				@Override
				protected void onContentsChanged() {
					balanceTanks();
					markDirty();
				}
			};
			break;
		default:
			break;
		}
	}
	
	public void transferAllFluid(IFluidHandler source, IFluidHandler dest, int maxQuantity) {
		FluidStack possibleDrain = source.drain(maxQuantity, false);
		if (possibleDrain == null || possibleDrain.amount == 0) {
			return;
		}
		int filled = dest.fill(possibleDrain, true);
		source.drain(filled, true);
	}
	
	public void transferAllItems(IItemHandler source, IItemHandler dest, int numstacks) {
		for (int slot = 0; slot < source.getSlots(); slot++) {
			ItemStack stack = source.getStackInSlot(slot);
			if (stack.isEmpty()) {
				continue;
			}
			int orig_count = stack.getCount();
			stack = ItemHandlerHelper.insertItem(dest, stack, false);
			if (stack.getCount() != orig_count) {
				source.extractItem(slot, orig_count - stack.getCount(), false);
				numstacks --;
			}
			if (numstacks <= 0) {
				return;
			}
		}
	}
	
	private <T> List<T> getCapsNearby(Capability<T> cap) {
		List<T> found = new ArrayList<T>();
		
		for (EnumFacing facing : EnumFacing.values()) {
			BlockPos npos = pos.offset(facing);
			if (world.isAirBlock(npos) || BlockUtil.isIRRail(world, npos)) {
				continue;
			}
			TileEntity nte = world.getTileEntity(npos);
			if (nte == null) {
				continue;
			}
			if (nte.hasCapability(cap, facing.getOpposite())) {
				found.add(nte.getCapability(cap, facing.getOpposite()));
			}
		}
		
		return found;
	}

	@Override
	public void update() {
		if (this.world.isRemote) {
			return;
		}
		
		ticksExisted += 1;
		
		if (Config.ConfigDebug.snowMeltRate != 0 && this.snowLayers != 0) {
			if ((int)(Math.random() * Config.ConfigDebug.snowMeltRate * 10) == 0) {
				if (!world.isRaining()) {
					this.setSnowLayers(this.snowLayers -= 1);
				}
			}
		}
		
		if (ticksExisted > 1 && (ticksExisted % (20 * 5) == 0 || blockUpdate)) {
			// Double check every 5 seconds that the master is not gone
			// Wont fire on first due to incr above
			blockUpdate = false;
			

			if (this.getParent() == null || !world.isBlockLoaded(this.getParent())) {
				return;
			}

			if (this.getParentTile() == null) {
				// Fire update event
				if (BlockRailBase.tryBreakRail(world, pos)) {
					getWorld().destroyBlock(pos, true);
				}
				return;
			}
			
			if (this instanceof TileRail) {
				double floating = ((TileRail)this).percentFloating();
				if (floating > ConfigBalance.trackFloatingPercent) {
					if (BlockRailBase.tryBreakRail(world, pos)) {
						getWorld().destroyBlock(pos, true);
					}
					return;
				}
			}
		}
		
		if (this.augment == null) {
			return;
		}
		
		Capability<IFluidHandler> fluid_cap = CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
		Capability<IItemHandler> item_cap = CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
		EntityMoveableRollingStock stock;
		IFluidHandler stock_fluid;
		IItemHandler stock_items;

		try {
			switch (this.augment) {
			case ITEM_LOADER:
				stock = this.getStockNearBy(item_cap);
				if (stock == null) {
					break;
				}
				stock_items = stock.getCapability(item_cap, null);
				for (IItemHandler neighbor : getCapsNearby(item_cap)) {
					transferAllItems(neighbor, stock_items, 1);
				}
				break;
			case ITEM_UNLOADER:
				stock = this.getStockNearBy(item_cap);
				if (stock == null) {
					break;
				}
				stock_items = stock.getCapability(item_cap, null);
				for (IItemHandler neighbor : getCapsNearby(item_cap)) {
					transferAllItems(stock_items, neighbor, 1);
				}
				break;
			case FLUID_LOADER:
				if (this.augmentTank == null) {
					this.createAugmentTank();
				}
				
				stock = this.getStockNearBy(fluid_cap);
				if (stock == null) {
					break;
				}
				
				stock_fluid = stock.getCapability(fluid_cap, null);
				transferAllFluid(augmentTank, stock_fluid, 100);
				for (IFluidHandler neighbor : getCapsNearby(fluid_cap)) {
					transferAllFluid(neighbor, stock_fluid, 100);
				}
				
				break;
			case FLUID_UNLOADER:
				if (this.augmentTank == null) {
					this.createAugmentTank();
				}
				
				stock = this.getStockNearBy(fluid_cap);
				if (stock == null) {
					break;
				}
				
				stock_fluid = stock.getCapability(fluid_cap, null);
				transferAllFluid(stock_fluid, augmentTank, 100);				
				for (IFluidHandler neighbor : getCapsNearby(fluid_cap)) {
					transferAllFluid(stock_fluid, neighbor, 100);
				}
				
				break;
			case WATER_TROUGH:
				if (this.augmentTank == null) {
					this.createAugmentTank();
				}
				Tender tender = this.getStockNearBy(Tender.class, fluid_cap);
				if (tender != null) {
					transferAllFluid(this.augmentTank, tender.getCapability(fluid_cap, null), waterPressureFromSpeed(tender.getCurrentSpeed().metric()));
				} else if (this.ticksExisted % 20 == 0) {
					balanceTanks();
				}
				break;
			case LOCO_CONTROL:
				Locomotive loco = this.getStockNearBy(Locomotive.class, null);
				if (loco != null) {
					int power = RedstoneUtil.getPower(world, pos);
					
					switch(controlMode) {
					case THROTTLE_FORWARD:
						loco.setThrottle(power/15f);
						break;
					case THROTTLE_REVERSE:
						loco.setThrottle(-power/15f);
						break;
					case BRAKE:
						loco.setAirBrake(power/15f);
						break;
					case HORN:
						loco.setHorn(40, null);
						break;
					case COMPUTER:
						//NOP
						break;
					}
				}
				break;
			case DETECTOR:
				stock = this.getStockNearBy(null);
				int currentRedstone = redstoneLevel;
				int newRedstone = 0;
	
				switch (this.redstoneMode ) {
				case SIMPLE:
					newRedstone = stock != null ? 15 : 0;
					break;
				case SPEED:
					newRedstone = stock != null ? (int)Math.floor(Math.abs(stock.getCurrentSpeed().metric())/10) : 0;
					break;
				case PASSENGERS:
					newRedstone = stock != null ? Math.min(15, stock.getPassengers().size() + stock.staticPassengers.size()) : 0;
					break;
				case CARGO:
					if (stock != null && stock instanceof Freight) {
						newRedstone = stock != null ? ((Freight)stock).getPercentCargoFull()*15/100 : 0;
					}
					break;
				case LIQUID:
					if (stock != null && stock instanceof FreightTank) {
						newRedstone = stock != null ? ((FreightTank)stock).getPercentLiquidFull()*15/100 : 0;
					}
					break;
				}
				
				
				if (newRedstone != currentRedstone) {
					this.redstoneLevel = newRedstone;
					this.markDirty(); //TODO overkill
				}
				break;
			case COUPLER:
				stock = this.getStockNearBy(null);
				int power = RedstoneUtil.getPower(world, pos);
				if (stock != null && stock instanceof EntityCoupleableRollingStock && power > 0) {
					EntityCoupleableRollingStock couplable = (EntityCoupleableRollingStock)stock;
					switch (couplerMode) {
					case ENGAGED:
						for (CouplerType coupler : CouplerType.values()) {
							couplable.setCouplerEngaged(coupler, true);
						}
						break;
					case DISENGAGED:
						for (CouplerType coupler : CouplerType.values()) {
							couplable.setCouplerEngaged(coupler, false);
						}
						break;
					}
					break;
				}
			default:
				break;
			}
		} catch (Exception ex) {
			ImmersiveRailroading.catching(ex);
		}
	}
	
	public int getRedstoneLevel() {
		return this.redstoneLevel;
	}
	
	public double getTankLevel() {
		return this.augmentTank == null ? 0 : (double)this.augmentTank.getFluidAmount() / this.augmentTank.getCapacity(); 
	}
	
	private static int waterPressureFromSpeed(double speed) {
		if (speed < 0) {
			return 0;
		}
		return (int) ((speed * speed) / 200);
	}
}
