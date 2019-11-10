package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.Config.ConfigBalance;
import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.items.ItemTrackBlueprint;
import cam72cam.immersiverailroading.items.nbt.ItemTrackExchangerType;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.physics.MovementTrack;
import cam72cam.immersiverailroading.thirdparty.trackapi.BlockEntityTrackTickable;
import cam72cam.immersiverailroading.util.*;
import cam72cam.mod.block.IRedstoneProvider;
import cam72cam.mod.entity.Player;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.fluid.FluidStack;
import cam72cam.mod.fluid.FluidTank;
import cam72cam.mod.fluid.ITank;
import cam72cam.mod.item.*;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.util.TagCompound;
import cam72cam.immersiverailroading.thirdparty.trackapi.ITrack;
import org.apache.commons.lang3.ArrayUtils;

public class TileRailBase extends BlockEntityTrackTickable implements IRedstoneProvider {
	private Vec3i parent;
	private float bedHeight = 0;
	private float railHeight = 0;
	private Augment augment; 
	private String augmentFilterID;
	private int snowLayers = 0;
	protected boolean flexible = false;
	private boolean willBeReplaced = false; 
	private TagCompound replaced;
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

	public boolean isLoaded() {
		// TODO removeme
		return true;
	}

	public void setBedHeight(float height) {
		this.bedHeight = height;
	}
	public float getBedHeight() {
		if (this.replaced != null && this.replaced.hasKey("height")) {
			float replacedHeight = this.replaced.getFloat("height");
			return Math.min(this.bedHeight, replacedHeight);
		}
		return this.bedHeight;
	}
	public double getRenderGauge() {
		double gauge = 0;
		TileRail parent = this.getParentTile();
		if (parent != null) {
			gauge = parent.info.settings.gauge.value();
		}
		if (this.getParentReplaced() != null && world != null) {
			parent = world.getBlockEntity(this.getParentReplaced(), TileRail.class);
            if (parent != null) {
                gauge = Math.min(gauge, parent.info.settings.gauge.value());
            }
		}
		return gauge;
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
		if (definitionID != null && !definitionID.equals(augmentFilterID)) {
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

	public Vec3i getParent() {
		if (parent == null) {
			if (ticksExisted > 1 && world.isServer) {
				ImmersiveRailroading.warn("Invalid block without parent");
				// Might be null during init
				world.setToAir(pos);
			}
			return null;
		}
		return parent.add(pos);
	}
	public void setParent(Vec3i pos) {
		this.parent = pos.subtract(this.pos);
	}
	
	public boolean isFlexible() {
		return this.flexible || !(this instanceof TileRail);
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
	public void writeUpdate(TagCompound nbt) {
		if (this.getRenderRailBed() != null) {
			nbt.set("renderBed", this.getRenderRailBed().toTag());
		}
	}
	@Override
	public void readUpdate(TagCompound nbt) {
		if (nbt.hasKey("renderBed")) {
			this.railBedCache = new ItemStack(nbt.get("renderBed"));
		}
		if (this.augmentTank != null && this.augment == Augment.WATER_TROUGH) {
            /*
			int delta = clientLastTankAmount - this.augmentTank.getContents().getAmount();
			if (delta > 0) {
				// We lost water, do spray
				// TODO, this fires during rebalance which is not correct
				for (int i = 0; i < delta/10; i ++) {
					for (Facing facing : Facing.values()) {
						world.createParticle(ParticleType.WATER_SPLASH, pos.offset(facing).addVector(0.5, 0.5, 0.5));
					}
				}
				if (clientSoundTimeout < world.getTime()) {
					world.internal.playSound(pos.x, pos.y, pos.z, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1, 1, false);
					clientSoundTimeout = world.getTime() + 10;
				}
			}
            */
			clientLastTankAmount = this.augmentTank.getContents().getAmount();
		}
	}
	
	@Override
	public void load(TagCompound nbt) {
		int version = 0;
		if (nbt.hasKey("version")) {
			version = nbt.getInteger("version");
		}


		railHeight = bedHeight = nbt.getFloat("height");
		snowLayers = nbt.getInteger("snowLayers");
		flexible = nbt.getBoolean("flexible");
		if (nbt.hasKey("replaced")) {
			replaced = nbt.get("replaced");
		}

		if (nbt.hasKey("augment")) {
			augment = Augment.values()[nbt.getInteger("augment")];
		}

		parent = nbt.getVec3i("parent");

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
			augmentTank.read(nbt.get("augmentTank"));
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
	public void save(TagCompound nbt) {
		nbt.setVec3i("parent", parent);
		nbt.setFloat("height", bedHeight);
		nbt.setFloat("railHeight", railHeight);
		nbt.setInteger("snowLayers", snowLayers);
		nbt.setBoolean("flexible", flexible);
		if (replaced != null) {
			nbt.set("replaced", replaced);
		}

		if (augment != null) {
			nbt.setInteger("augment", this.augment.ordinal());
			if (augmentTank != null) {
				nbt.set("augmentTank", augmentTank.write(new TagCompound()));
			}
			if (augmentFilterID != null) {
				nbt.setString("augmentFilterID", augmentFilterID);
			}
		}
		nbt.setInteger("redstoneMode", redstoneMode.ordinal());
		nbt.setInteger("controlMode", controlMode.ordinal());
		nbt.setInteger("couplerMode", couplerMode.ordinal());

		nbt.setInteger("version", 3);
	}

	public TileRail getParentTile() {
		if (this.getParent() == null) {
			return null;
		}
		TileRail te = world.getBlockEntity(this.getParent(), TileRail.class);
		if (te == null || !te.isLoaded()) {
			return null;
		}
		return te;
	}
	public void setReplaced(TagCompound replaced) {
		this.replaced = replaced;
	}
	public TagCompound getReplaced() {
		return replaced;
	}

	/* TODO HACKS
	@Override
	public boolean shouldRefresh(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, net.minecraft.block.state.IBlockState oldState, net.minecraft.block.state.IBlockState newState) {
		// This works around a hack where Chunk does a removeTileEntity directly after calling breakBlock
		// We have already removed the original TE and are replacing it with one which goes with a new block 
		if (this.skipNextRefresh ) {
			return false;
		}
		return super.shouldRefresh(world, pos, oldState, newState);
	}
	*/
	
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
				Facing[] horiz = Facing.values().clone();
				if (Math.random() > 0.5) {
					// Split between sides of the track
					ArrayUtils.reverse(horiz);
				}
				for (Facing facing : horiz) {
					Vec3i ph = world.getPrecipitationHeight(pos.offset(facing, i));
					for (int j = 0; j < 3; j ++) {
						if (world.isAir(ph) && !ITrack.isRail(world, ph.down())) {
							world.setSnowLevel(ph, snowDown);
							return;
						}
						if (world.isSnowBlock(ph)) {
							ph = ph.up();
							continue;
						}
						int currSnow = world.getSnowLevel(ph);
						if (currSnow != 0) {
							if (currSnow == 8) {
								ph = ph.up();
								continue;
							}
							int toAdd = Math.min(8 - currSnow, snowDown);
							world.setSnowLevel(ph, currSnow + toAdd);
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
		double distanceMeters = motion.length();
		float rotationYaw = VecUtil.toWrongYaw(motion);
		Vec3d nextPos = currentPosition;

		TileRailBase self = this;
		TileRail tile = this instanceof TileRail ? (TileRail) this : this.getParentTile();

		while(tile != null) {
			SwitchState state = SwitchUtil.getSwitchState(tile, currentPosition);

			if (state == SwitchState.STRAIGHT) {
				tile = tile.getParentTile();
			}


			Vec3d potential = MovementTrack.nextPosition(world, currentPosition, tile, rotationYaw, distanceMeters);
			if (state == SwitchState.TURN) {
				float other = VecUtil.toWrongYaw(potential.subtract(currentPosition));
				double diff = MathUtil.trueModulus(other - rotationYaw, 360);
				diff = Math.min(360-diff, diff);
				if (diff < 30) {
					nextPos = potential;
					break;
				}
			} else {
				if (potential.distanceTo(currentPosition.add(motion)) < nextPos.distanceTo(currentPosition.add(motion)) ||
						currentPosition == nextPos) {
					nextPos = potential;
				}
			}

			if (self.getParentTile() == null) {
				// Still loading
				ImmersiveRailroading.warn("Unloaded parent at %s", self.getParent());
				break;
			}

            tile = null;
			Vec3i currentParent = self.getParentTile().getParent();
			for (TagCompound data = self.getReplaced(); data != null; data = self.getReplaced()) {
				self = (TileRailBase) world.reconstituteBlockEntity(data);
				if (self == null) {
					break;
				}
				if (!currentParent.equals(self.getParent())) {
					tile = self.getParentTile();
					break;
				}
			}
		}
		return nextPos;
	}
	
	/*
	 * Capabilities tie ins
	 */

	public <T extends EntityRollingStock> T getStockNearBy(Class<T> type){
		return world.getEntities((T stock) -> {
			if (augmentFilterID == null || augmentFilterID.equals(stock.getDefinitionID())) {
				return stock.getBounds().intersects(new Vec3d(this.pos.south().west()), new Vec3d(this.pos.up(3).east().north()));
			}
			return false;
		}, type).stream().findFirst().orElse(null);
	}

	@Override
	public IInventory getInventory(Facing side) {
		if (this.getAugment() != null) {
			switch (this.getAugment()) {
				case ITEM_LOADER:
				case ITEM_UNLOADER:
					Freight stock = getStockNearBy(Freight.class);
					if (stock != null) {
						return stock.cargoItems;
					}
					return new ItemStackHandler(0);
			}
		}
		return null;
	}

	@Override
	public ITank getTank(Facing side) {
		if (this.getAugment() != null) {
			switch (this.getAugment()) {
				case FLUID_LOADER:
				case FLUID_UNLOADER:
                    if (this.augmentTank == null) {
                        this.createAugmentTank();
                    }
                    return this.augmentTank;
			}
		}
		return null;
	}

	private void balanceTanks() {
		/*
		for (Facing facing : Facing.values()) {
			RailBase neighbor = world.getTileEntity(pos.offset(facing), RailBase.class);
			if (neighbor != null && neighbor.augmentTank != null) {
				if (neighbor.augmentTank.getContents().getAmount() + 1 < augmentTank.getContents().getAmount()) {
					transferAllFluid(augmentTank, neighbor.augmentTank, (augmentTank.getContents().getAmount() - neighbor.augmentTank.getContents().getAmount())/2);
					this.markDirty();
				}
			}
		}
		*/
	}

	private void createAugmentTank() {
		switch(this.augment) {
		case FLUID_LOADER:
		case FLUID_UNLOADER:
			this.augmentTank = new FluidTank(null, 1000);
			break;
		case WATER_TROUGH:
			this.augmentTank = new FluidTank(new FluidStack(Fluid.WATER, 0), 1000) {
				@Override
				public void onChanged() {
					balanceTanks();
					markDirty();
				}
			};
			break;
		default:
			break;
		}
	}

	@Override
	public void update() {
		if (this.world.isClient) {
			return;
		}
		
		ticksExisted += 1;
		
		if (ConfigDebug.snowMeltRate != 0 && this.snowLayers != 0) {
			if ((int)(Math.random() * ConfigDebug.snowMeltRate * 10) == 0) {
				if (!world.isPrecipitating()) {
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
				if (IRBlocks.BLOCK_RAIL_GAG.tryBreak(world, pos, null)) {
					world.breakBlock(pos);
				}
				return;
			}
			
			if (Config.ConfigDamage.requireSolidBlocks && this instanceof TileRail) {
				double floating = ((TileRail)this).percentFloating();
				if (floating > ConfigBalance.trackFloatingPercent) {
					if (IRBlocks.BLOCK_RAIL_GAG.tryBreak(world, pos, null)) {
						world.breakBlock(pos);
					}
					return;
				}
			}
		}
		
		if (this.augment == null) {
			return;
		}

		try {
			switch (this.augment) {
            case ITEM_LOADER:
			{
				Freight freight = this.getStockNearBy(Freight.class);
				if (freight == null) {
					break;
				}
				ItemStackHandler freight_items = freight.cargoItems;
				for (Facing side : Facing.values()) {
					IInventory inventory = world.getInventory(pos.offset(side));
					if (inventory != null) {
						inventory.transferAllTo(freight_items);
					}
				}
			}
			break;
			case ITEM_UNLOADER: {
				Freight freight = this.getStockNearBy(Freight.class);
				if (freight == null) {
					break;
				}
				ItemStackHandler freight_items = freight.cargoItems;
				for (Facing side : Facing.values()) {
					IInventory inventory = world.getInventory(pos.offset(side));
					if (inventory != null) {
						inventory.transferAllFrom(freight_items);
					}
				}
			}
			break;
			case FLUID_LOADER: {
				if (this.augmentTank == null) {
					this.createAugmentTank();
				}

				FreightTank stock = this.getStockNearBy(FreightTank.class);
				if (stock == null) {
					break;
				}
				augmentTank.tryFill(stock.theTank, 100, false);
                for (Facing side : Facing.values()) {
                	ITank tank = world.getTank(pos.offset(side));
                	if (tank != null) {
						stock.theTank.tryDrain(tank, 100, false);
					}
				}
			}
				break;
			case FLUID_UNLOADER: {
				if (this.augmentTank == null) {
					this.createAugmentTank();
				}

				FreightTank stock = this.getStockNearBy(FreightTank.class);
				if (stock == null) {
					break;
				}

				augmentTank.tryDrain(stock.theTank, 100, false);
                for (Facing side : Facing.values()) {
                    ITank tank = world.getTank(pos.offset(side));
                    if (tank != null) {
						stock.theTank.tryFill(tank, 100, false);
					}
				}
			}
				
				break;
			case WATER_TROUGH:
				/*
				if (this.augmentTank == null) {
					this.createAugmentTank();
				}
				Tender tender = this.getStockNearBy(Tender.class, fluid_cap);
				if (tender != null) {
					transferAllFluid(this.augmentTank, tender.getCapability(fluid_cap, null), waterPressureFromSpeed(tender.getCurrentSpeed().metric()));
				} else if (this.ticksExisted % 20 == 0) {
					balanceTanks();
				}
                */
				break;
			case LOCO_CONTROL: {
				Locomotive loco = this.getStockNearBy(Locomotive.class);
				if (loco != null) {
					int power = world.getRedstone(pos);

					switch (controlMode) {
						case THROTTLE_FORWARD:
							loco.setThrottle(power / 15f);
							break;
						case THROTTLE_REVERSE:
							loco.setThrottle(-power / 15f);
							break;
						case BRAKE:
							loco.setAirBrake(power / 15f);
							break;
						case HORN:
							loco.setHorn(40, null);
							break;
						case COMPUTER:
							//NOP
							break;
					}
				}
			}
				break;
			case DETECTOR: {
				EntityMoveableRollingStock stock = this.getStockNearBy(EntityMoveableRollingStock.class);
				int currentRedstone = redstoneLevel;
				int newRedstone = 0;

				switch (this.redstoneMode) {
					case SIMPLE:
						newRedstone = stock != null ? 15 : 0;
						break;
					case SPEED:
						newRedstone = stock != null ? (int) Math.floor(Math.abs(stock.getCurrentSpeed().metric()) / 10) : 0;
						break;
					case PASSENGERS:
						newRedstone = stock != null ? Math.min(15, stock.getPassengerCount()) : 0;
						break;
					case CARGO:
						newRedstone = 0;
						if (stock instanceof Freight) {
							newRedstone = ((Freight) stock).getPercentCargoFull() * 15 / 100;
						}
						break;
					case LIQUID:
						newRedstone = 0;
						if (stock instanceof FreightTank) {
							newRedstone = ((FreightTank) stock).getPercentLiquidFull() * 15 / 100;
						}
						break;
				}


				if (newRedstone != currentRedstone) {
					this.redstoneLevel = newRedstone;
					this.markDirty(); //TODO overkill
				}
			}
				break;
			case COUPLER: {
				EntityCoupleableRollingStock stock = this.getStockNearBy(EntityCoupleableRollingStock.class);
				if (stock != null) {
					switch (couplerMode) {
						case ENGAGED:
							for (CouplerType coupler : CouplerType.values()) {
								stock.setCouplerEngaged(coupler, true);
							}
							break;
						case DISENGAGED:
							for (CouplerType coupler : CouplerType.values()) {
								stock.setCouplerEngaged(coupler, false);
							}
							break;
					}
					break;
				}
			}
			default:
				break;
			}
		} catch (Exception ex) {
			ImmersiveRailroading.catching(ex);
		}
	}

	@Override
	public int getStrongPower(Facing facing) {
		return getAugment() == Augment.DETECTOR ? this.redstoneLevel : 0;
	}

	@Override
	public int getWeakPower(Facing facing) {
		return getAugment() == Augment.DETECTOR ? this.redstoneLevel : 0;
	}

	public double getTankLevel() {
		return this.augmentTank == null ? 0 : (double)this.augmentTank.getContents().getAmount() / this.augmentTank.getCapacity();
	}
	
	private static int waterPressureFromSpeed(double speed) {
		if (speed < 0) {
			return 0;
		}
		return (int) ((speed * speed) / 200);
	}

	public Vec3i getParentReplaced() {
		if (this.replaced == null) {
			return null;
		}
		if (!this.replaced.hasKey("parent")) {
			return null;
		}
		return new Vec3i(this.replaced.getLong("parent")).add(pos);
	}

	public SwitchState cycleSwitchForced() {
		TileRail tileSwitch = this.findSwitchParent();
		SwitchState newForcedState = SwitchState.NONE;

		if (tileSwitch != null) {
			newForcedState = SwitchState.values()[( tileSwitch.info.switchForced.ordinal() + 1 ) % SwitchState.values().length];
			tileSwitch.info = new RailInfo(world, tileSwitch.info.settings, tileSwitch.info.placementInfo, tileSwitch.info.customInfo, tileSwitch.info.switchState, newForcedState, tileSwitch.info.tablePos);
			tileSwitch.markDirty();
			this.markDirty();
			this.getParentTile().markDirty();
		}

		return newForcedState;
	}

	public boolean isSwitchForced() {
		TileRail tileSwitch = this.findSwitchParent();
		if (tileSwitch != null) {
			return tileSwitch.info.switchForced != SwitchState.NONE;
		} else {
			return false;
		}
	}

	/** Finds a parent of <code>this</code> whose type is TrackItems.SWITCH. Returns null if one doesn't exist
	 * @return parent Rail where parent.info.settings.type.equals(TrackItems.SWITCH) is true, if such a parent exists; null otherwise
	 */
	public TileRail findSwitchParent() {
		return findSwitchParent(this);
	}

	/** Finds a parent of <code>cur</code> whose type is TrackItems.SWITCH. Returns null if one doesn't exist
	 * @param cur RailBase whose parents are to be traversed
	 * @return parent Rail where parent.info.settings.type.equals(TrackItems.SWITCH) is true, if such a parent exists; null otherwise
	 */
	public TileRail findSwitchParent(TileRailBase cur) {
		if (cur == null) {
			return null;
		}

		if (cur instanceof TileRail) {
			TileRail curTR = (TileRail) cur;
			if (curTR.info.settings.type.equals(TrackItems.SWITCH)) {
				return curTR;
			}
		}

		// Prevent infinite recursion
		if (cur.pos.equals(cur.getParentTile().pos)) {
			return null;
		}

		return findSwitchParent(cur.getParentTile());
	}

	/* NEW STUFF */




	@Override
	public double getHeight() {
		return getFullHeight() +0.1 * (getTrackGauge() / Gauge.STANDARD);
	}

	@Override
	public void onBreak() {
		if (this instanceof TileRail) {
			((TileRail) this).spawnDrops();
		}

		breakParentIfExists();
	}

	@Override
	public boolean onClick(Player player, Hand hand, Facing facing, Vec3d hit) {
		ItemStack stack = player.getHeldItem(hand);
		if (stack.is(IRItems.ITEM_SWITCH_KEY)) {
			TileRail tileSwitch = this.findSwitchParent();
			if (tileSwitch != null) {
				SwitchState switchForced = this.cycleSwitchForced();
				if (this.world.isServer) {
					player.sendMessage(switchForced.equals(SwitchState.NONE) ? ChatText.SWITCH_UNLOCKED.getMessage() : ChatText.SWITCH_LOCKED.getMessage(switchForced.toString()));
				}
			}
		}
		if (stack.is(IRItems.ITEM_TRACK_EXCHANGER)) {
			TileRail tileRail = this.getParentTile();
			String track = ItemTrackExchangerType.get(stack);
			if (track != null && !track.equals(tileRail.info.settings.track)) {
				if (!player.isCreative()) {
					RailInfo info = tileRail.info.withTrack(track);
					if (info.build(player, false)) { //cancel if player doesn't have all required items
						//FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendPacketToAllPlayers( //we need to send the packet because this code is executed on the server side
						//		new SPacketSoundEffect(SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS,pos.getX(), pos.getY(), pos.getZ(), 1.0f, 0.2f));
						tileRail.info = info;

						tileRail.spawnDrops(player.getPosition());
						tileRail.setDrops(info.getBuilder(new Vec3i(info.placementInfo.placementPosition)).drops);
						tileRail.markDirty();
					}
				} else {
					//FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendPacketToAllPlayers(
					//		new SPacketSoundEffect(SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS,pos.getX(), pos.getY(), pos.getZ(), 1.0f, 0.2f));
					tileRail.info = tileRail.info.withTrack(track);
				}
			}
		}
		if (stack.is(Fuzzy.REDSTONE_TORCH)) {
			String next = this.nextAugmentRedstoneMode();
			if (next != null) {
				if (this.world.isServer) {
					player.sendMessage(PlayerMessage.direct(next));
				}
				return true;
			}
		}
		if (stack.is(Fuzzy.SNOW_LAYER)) {
			if (this.world.isServer) {
				this.handleSnowTick();
			}
			return true;
		}
		if (stack.is(Fuzzy.SNOW_BLOCK)) {
			if (this.world.isServer) {
				for (int i = 0; i < 8; i ++) {
					this.handleSnowTick();
				}
			}
			return true;
		}
		if (stack.isValidTool(ToolType.SHOVEL)) {
			if (this.world.isServer) {
				this.cleanSnow();
				this.setSnowLayers(0);
				stack.damageItem(1, player);
			}
			return true;
		}
		return false;
	}

	@Override
	public ItemStack onPick() {
		ItemStack stack = new ItemStack(IRItems.ITEM_TRACK_BLUEPRINT, 1);
		if (!this.isLoaded()) {
			return stack;
		}

		TileRail parent = this.getParentTile();
		if (parent == null) {
			return stack;
		}
		ItemTrackBlueprint.settings(stack, parent.info.settings);
		return stack;
	}

	@Override
	public void onNeighborChange(Vec3i neighbor) {
		TileRailBase te = this;

		if (world.isClient) {
			return;
		}

		blockUpdate = true;

		if (world.getItemStack(pos.up()).is(Fuzzy.SNOW_LAYER)) {
			if (handleSnowTick()) {
				world.setToAir(pos.up());
			}
		}

		TagCompound data = te.getReplaced();
		while (true) {
			if (te.getParentTile() != null && te.getParentTile().getParentTile() != null) {
				TileRail switchTile = te.getParentTile();
				if (te instanceof TileRail) {
					switchTile = (TileRail) te;
				}
				SwitchState state = SwitchUtil.getSwitchState(switchTile);
				if (state != SwitchState.NONE) {
					switchTile.setSwitchState(state);
				}
			}
			if (data == null) {
				break;
			}
			te = (TileRailBase) world.reconstituteBlockEntity(data);
			if (te == null) {
				break;
			}
			data = te.getReplaced();
		}
	}

	private void breakParentIfExists() {
		TileRail parent = getParentTile();
		if (parent != null && !getWillBeReplaced()) {
			parent.spawnDrops();
			//if (tryBreak(getWorld(), te.getPos())) {
			world.setToAir(parent.pos);
			//}
		}
	}

	@Override
	public boolean tryBreak(Player player) {
		try {
			TileRailBase rail = this;
			if (rail.getReplaced() != null) {
				// new object here is important
				TileRailGag newGag = (TileRailGag) world.reconstituteBlockEntity(rail.getReplaced());
				if (newGag == null) {
					return true;
				}

				while(true) {
					if (newGag.getParent() != null && world.hasBlockEntity(newGag.getParent(), TileRail.class)) {
						world.setBlockEntity(pos, newGag);
						rail.breakParentIfExists();
						return false;
					}
					// Only do replacement if parent still exists

					TagCompound data = newGag.getReplaced();
					if (data == null) {
						break;
					}

					newGag = (TileRailGag) world.reconstituteBlockEntity(data);
					if (newGag == null) {
						break;
					}
				}
			}
		} catch (StackOverflowError ex) {
			ImmersiveRailroading.error("Invalid recursive rail block at %s", pos);
			ImmersiveRailroading.catching(ex);
			world.setToAir(pos);
		}
		return true;
	}
}
