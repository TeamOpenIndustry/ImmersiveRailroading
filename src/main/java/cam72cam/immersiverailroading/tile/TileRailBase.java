package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.Config.ConfigBalance;
import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.entity.physics.Simulation;
import cam72cam.immersiverailroading.entity.physics.SimulationState;
import cam72cam.immersiverailroading.items.ItemRailAugment;
import cam72cam.immersiverailroading.items.ItemTrackExchanger;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.model.part.Door;
import cam72cam.immersiverailroading.physics.MovementTrack;
import cam72cam.immersiverailroading.thirdparty.trackapi.BlockEntityTrackTickable;
import cam72cam.immersiverailroading.util.*;
import cam72cam.mod.block.IRedstoneProvider;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.fluid.FluidTank;
import cam72cam.mod.fluid.ITank;
import cam72cam.mod.item.*;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.sound.Audio;
import cam72cam.mod.sound.SoundCategory;
import cam72cam.mod.sound.StandardSound;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.Facing;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.immersiverailroading.thirdparty.trackapi.ITrack;
import cam72cam.mod.util.SingleCache;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

import static cam72cam.immersiverailroading.entity.Locomotive.AUTOMATED_PLAYER;

public class TileRailBase extends BlockEntityTrackTickable implements IRedstoneProvider {
	@TagField("parent")
	protected Vec3i parent;
	@TagField("height")
	private float bedHeight = 0;
	@TagField("railHeight")
	private float railHeight = 0;
	@TagField("augment")
	private Augment augment;
	@TagField("augmentFilterID")
	private String augmentFilterID;
	@TagField("snowLayers")
	private int snowLayers = 0;
	@TagField("flexible")
	protected boolean flexible = false;
	private boolean willBeReplaced = false;
	@TagField("replaced")
	private TagCompound replaced;
	private boolean skipNextRefresh = false;
	public ItemStack railBedCache = null;
	private final FluidTank emptyTank = new FluidTank(null, 0);
	private final IInventory emptyInventory = new ItemStackHandler(0);
	private int redstoneLevel = 0;
	@TagField("redstoneMode")
	private StockDetectorMode detectorMode = StockDetectorMode.SIMPLE;
	@TagField("controlMode")
	private LocoControlMode controlMode = LocoControlMode.THROTTLE;
	@TagField("couplerMod")
	private CouplerAugmentMode couplerMode = CouplerAugmentMode.ENGAGED;
	@TagField("redstoneSensitivity")
	private RedstoneMode redstoneMode = RedstoneMode.ENABLED;
	private int ticksExisted;
	public boolean blockUpdate;
	private Gauge augmentGauge;
	@TagField("stockTag")
	private String stockTag;
	private EntityMoveableRollingStock overhead;
	@TagField("pushPull")
	private boolean pushPull = true;

	// HAXXX
	private int keepParentsLoaded = 0;

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
		if (parent != null && parent.info != null) {
			gauge = parent.info.settings.gauge.value();
		}
		if (this.getParentReplaced() != null && getWorld() != null) {
			parent = getWorld().getBlockEntity(this.getParentReplaced(), TileRail.class);
            if (parent != null && parent.info != null) {
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
		if (getParentTile() != null) {
			augmentGauge = getParentTile().info.settings.gauge;
			if (ConfigDebug.defaultAugmentComputer && augment != null) {
				switch (augment) {
					case DETECTOR:
						detectorMode = StockDetectorMode.COMPUTER;
						break;
					case LOCO_CONTROL:
						controlMode = LocoControlMode.COMPUTER;
						break;
				}
			}
		}
		setAugmentFilter(null);
		redstoneMode = RedstoneMode.ENABLED;
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

	public PlayerMessage nextAugmentRedstoneMode(boolean isPiston) {
		if (this.augment == null) {
			return null;
		}
		switch (this.augment) {
			case DETECTOR:
				detectorMode = StockDetectorMode.values()[((detectorMode.ordinal() + 1) % (StockDetectorMode.values().length))];
				return PlayerMessage.translate(detectorMode.toString());
			case LOCO_CONTROL:
				controlMode = LocoControlMode.values()[((controlMode.ordinal() + 1) % (LocoControlMode.values().length))];
				return PlayerMessage.translate(controlMode.toString());
			case COUPLER:
				if (isPiston) {
					couplerMode = CouplerAugmentMode.values()[((couplerMode.ordinal() + 1) % (CouplerAugmentMode.values().length))];
					return PlayerMessage.translate(couplerMode.toString());
				}
				// Fall through to redstone control setting
			case ITEM_LOADER:
			case ITEM_UNLOADER:
			case FLUID_LOADER:
			case FLUID_UNLOADER:
				if (isPiston) {
					this.pushPull = !this.pushPull;
					return PlayerMessage.translate("immersiverailroading:augment.pushpull." + (this.pushPull ? "enabled" : "disabled"));
				} else {
					this.redstoneMode = RedstoneMode.values()[(redstoneMode.ordinal() + 1) % RedstoneMode.values().length];
					return PlayerMessage.translate(redstoneMode.toString());
				}
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
	
	public void handleSnowTick() {
		if (this.snowLayers < (ConfigDebug.deepSnow ? 8 : 1)) {
			this.snowLayers += 1;
			this.markDirty();
		}
	}

	private final SingleCache<Vec3i, Vec3i> parentCache = new SingleCache<>(parent -> parent.add(getPos()));
	public Vec3i getParent() {
		if (parent == null) {
			if (ticksExisted > 5 && getWorld().isServer) {
				ImmersiveRailroading.warn("Invalid block without parent");
				// Might be null during init
				getWorld().setToAir(getPos());
			}
			return null;
		}
		// Assume if pos changes (piston? WE?) the TE is re-initialized
		return parentCache.get(parent);
	}
	public void setParent(Vec3i pos) {
		this.parent = pos.subtract(this.getPos());
	}
	
	public boolean isFlexible() {
		return this.flexible || !(this instanceof TileRail);
	}
	
	public ItemStack getRenderRailBed() {
		if (railBedCache == null && getParent() != null && getWorld().isBlockLoaded(getParent())) {
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
	}
	
	@Override
	public void load(TagCompound nbt) {
		int version = 0;
		if (nbt.hasKey("version")) {
			version = nbt.getInteger("version");
		}
		switch(version) {
		case 0:
			//NOP
		case 1:
			parent = parent.subtract(getPos());
		case 2:
			// Nothing in base
		case 3:
			if (!nbt.hasKey("railHeight")) {
				railHeight = bedHeight;
			}
		}
	}
	@Override
	public void save(TagCompound nbt) {
		nbt.setInteger("version", 4);
	}

	public TileRail getParentTile() {
		if (this.getParent() == null) {
			return null;
		}

		if (Thread.currentThread().getName().contains("ImmersiveRailroading")) {
			keepParentsLoaded = 20;
			if (!getWorld().isBlockLoaded(getParent())) {
				// We can't load chunks on any of the "IR" threads
				ImmersiveRailroading.warn("Unable to load chunks (getParentTile) on custom IR threads!");
				Simulation.chunksStillLoading = true;
				return null;
			}
		}

		TileRail te = getWorld().getBlockEntity(this.getParent(), TileRail.class);
		if (te == null || te.info == null) {
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
					Vec3i ph = getWorld().getPrecipitationHeight(getPos().offset(facing, i));
					for (int j = 0; j < 3; j ++) {
						if (getWorld().isAir(ph) && !ITrack.isRail(getWorld(), ph.down())) {
							getWorld().setSnowLevel(ph, snowDown);
							return;
						}
						int currSnow = getWorld().getSnowLevel(ph);
						if (currSnow > 0 && currSnow < 8) {
							int toAdd = Math.min(8 - currSnow, snowDown);
							getWorld().setSnowLevel(ph, currSnow + toAdd);
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

	protected Double cachedGauge = null;
	@Override
	public double getTrackGauge() {
		if (cachedGauge == null && getParent() != null) {
			TileRail parent = this.getParentTile();
			if (parent != null) {
				cachedGauge = parent.info.settings.gauge.value();
			}
		}
		return cachedGauge != null ? cachedGauge : 0;
	}

	@Override
	public Vec3d getNextPosition(Vec3d currentPosition, Vec3d motion) {
		float rotationYaw = VecUtil.toWrongYaw(motion);
		Vec3d nextPos = currentPosition;
		Vec3d predictedPos = currentPosition.add(motion);
		boolean hasSwitchSet = false;

		TileRailBase self = this;
		TileRail tile = this instanceof TileRail ? (TileRail) this : this.getParentTile();

		if (tile == null) {
			// Can happen due to track in progress of breaking
			return currentPosition;
		}

		double distanceMeters = motion.length();
		if (distanceMeters > MovementTrack.maxDistance) {
			return MovementTrack.nextPosition(getWorld(), currentPosition, tile, rotationYaw, distanceMeters);
		}

		while(tile != null) {
			SwitchState state = SwitchUtil.getSwitchState(tile, currentPosition);

			if (state == SwitchState.STRAIGHT) {
				tile = tile.getParentTile();
			}


			Vec3d potential = MovementTrack.nextPositionDirect(getWorld(), currentPosition, tile, rotationYaw, distanceMeters);
			if (potential != null) {
				// If the track veers onto the curved leg of a switch, try that (with angle limitation)
				// If two overlapped switches are both set, we could have a weird situation, but it's a incredibly unlikely edge case
				if (state == SwitchState.TURN) {
					float other = VecUtil.toWrongYaw(potential.subtract(currentPosition));
					double diff = MathUtil.trueModulus(other - rotationYaw, 360);
					diff = Math.min(360-diff, diff);
					if (diff < 2.5) {
						hasSwitchSet = true;
						nextPos = potential;
					}
				}
				// If we are not on a switch curve and closer to our target (or are on the first iteration)
				if (!hasSwitchSet && potential.distanceToSquared(predictedPos) < nextPos.distanceToSquared(predictedPos) ||
						currentPosition == nextPos) {
					nextPos = potential;
				}
			}

			if (self.getReplaced() == null) {
				break;
			}

			if (self.getParentTile() == null) {
				// Still loading
				ImmersiveRailroading.warn("Unloaded parent at %s", self.getParent());
				break;
			}

            tile = null;
			Vec3i currentParent = self.getParentTile().getParent();
			for (TagCompound data = self.getReplaced(); data != null; data = self.getReplaced()) {
				self = (TileRailBase) getWorld().reconstituteBlockEntity(data);
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
		if (overhead == null) {
			return null;
		}
		if (augmentFilterID != null && !augmentFilterID.equals(overhead.getDefinitionID())) {
			return null;
		}
		if (stockTag != null && stockTag.equals(overhead.tag)) {
			return null;
		}

		return overhead.as(type);
	}

	private boolean canOperate() {
		switch (this.redstoneMode) {
			case ENABLED:
				return true;
			case REQUIRED:
				return getWorld().getRedstone(getPos()) > 0;
			case INVERTED:
				return getWorld().getRedstone(getPos()) == 0;
			case DISABLED:
			default:
				return false;
		}
	}

	@Override
	public IInventory getInventory(Facing side) {
		if (this.getAugment() != null) {
			switch (this.getAugment()) {
				case ITEM_LOADER:
				case ITEM_UNLOADER:
					if (canOperate()) {
						Freight freight = getStockNearBy(Freight.class);
						if (freight != null && !freight.isDead()) {
							return freight.cargoItems;
						}
					}
					// placeholder for connections
					return this.emptyInventory;
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
					if (canOperate()) {
						FreightTank stock = getStockNearBy(FreightTank.class);
						if (stock != null) {
							return stock.theTank;
						}
					}
					// placeholder for connections
                    return this.emptyTank;
			}
		}
		return null;
	}

	@Override
	public void update() {
		if (this.getWorld().isClient) {
			return;
		}

		if (keepParentsLoaded > 0) {
			this.keepParentsLoaded--;

			TileRailBase self = this;
			TileRail tile = this instanceof TileRail ? (TileRail) this : this.getParentTile();

			// This terrible logic was copied from getNextPosition and should be fixed in kind
			while(tile != null) {
				SwitchState state = SwitchUtil.getSwitchState(tile, null);

				if (state == SwitchState.STRAIGHT) {
					tile = tile.getParentTile();
				}

				if (self.getReplaced() == null) {
					break;
				}

				if (self.getParentTile() == null) {
					// Still loading
					ImmersiveRailroading.warn("Unloaded parent at %s", self.getParent());
					break;
				}

				tile = null;
				Vec3i currentParent = self.getParentTile().getParent();
				for (TagCompound data = self.getReplaced(); data != null; data = self.getReplaced()) {
					self = (TileRailBase) getWorld().reconstituteBlockEntity(data);
					if (self == null) {
						break;
					}
					if (!currentParent.equals(self.getParent())) {
						tile = self.getParentTile();
						break;
					}
				}
			}
		}
		
		ticksExisted += 1;

		if (((int) (Math.random() * ConfigDebug.snowAccumulateRate * 10) == 0)) {
			if (getWorld().isSnowing(getPos()) && getWorld().canSeeSky(getPos().up())) {
				this.handleSnowTick();
			}
		}
		if (ConfigDebug.snowMeltRate != 0 && this.snowLayers != 0) {
			if ((int) (Math.random() * ConfigDebug.snowMeltRate * 10) == 0) {
				if (!getWorld().isSnowing(getPos())) {
					this.setSnowLayers(this.snowLayers -= 1);
				}
			}
		}

		if (ticksExisted > 5 && blockUpdate || (ticksExisted % (20 * 5) == 0 && ticksExisted > (20 * 20))) {
			// Double check every 5 seconds that the master is not gone
			// Wont fire on first due to incr above
			blockUpdate = false;

			if (this.getParent() == null || !getWorld().isBlockLoaded(this.getParent())) {
				return;
			}

			if (this.getParentTile() == null) {
				// Fire update event
				if (IRBlocks.BLOCK_RAIL_GAG.tryBreak(getWorld(), getPos(), null)) {
					getWorld().breakBlock(getPos());
				}
				return;
			} else {
				augmentGauge = getParentTile().info.settings.gauge;
			}
			
			if (Config.ConfigDamage.requireSolidBlocks && this instanceof TileRail && getWorld().isBlock(getPos(), IRBlocks.BLOCK_RAIL)) {
				double floating = ((TileRail)this).percentFloating();
				if (floating > ConfigBalance.trackFloatingPercent) {
					if (this.tryBreak(null)) {
						getWorld().breakBlock(getPos());
					}
					return;
				}
			}
		}
		
		if (this.augment == null) {
			return;
		}

		if (overhead != null && ticksExisted % 5 == 0) {
			SimulationState state = overhead.getCurrentState();
			if (state == null || !state.trackToUpdate.contains(getPos())) {
				overhead = null;
			}
		}

		if (this.ticksExisted % 20 == 0) {
			switch (augment) {
				case ITEM_LOADER:
				case ITEM_UNLOADER:
				case FLUID_LOADER:
				case FLUID_UNLOADER:
					// Fire off update event (looking at you ImmersiveEngineering)
					this.markDirty();
			}
		}

		if (!canOperate()) {
			return;
		}

		try {
			switch (this.augment) {
            case ITEM_LOADER:
			if (pushPull) {
				Freight freight = this.getStockNearBy(Freight.class);
				if (freight == null) {
					break;
				}
				for (Facing side : Facing.values()) {
					IInventory inventory = getWorld().getInventory(getPos().offset(side));
					if (inventory != null) {
						inventory.transferAllTo(freight.cargoItems);
					}
				}
			}
			break;
			case ITEM_UNLOADER:
			if (pushPull) {
				Freight freight = this.getStockNearBy(Freight.class);
				if (freight == null) {
					break;
				}
				for (Facing side : Facing.values()) {
					IInventory inventory = getWorld().getInventory(getPos().offset(side));
					if (inventory != null) {
						inventory.transferAllFrom(freight.cargoItems);
					}
				}
			}
			break;
			case FLUID_LOADER:
			if (pushPull) {
				FreightTank stock = this.getStockNearBy(FreightTank.class);
				if (stock == null) {
					break;
				}
                for (Facing side : Facing.values()) {
                	List<ITank> tanks = getWorld().getTank(getPos().offset(side));
                	if (tanks != null) {
                		tanks.forEach(tank -> stock.theTank.drain(tank, 100, false));
					}
				}
			}
			break;
			case FLUID_UNLOADER:
			if (pushPull) {
				FreightTank stock = this.getStockNearBy(FreightTank.class);
				if (stock == null) {
					break;
				}
                for (Facing side : Facing.values()) {
                    List<ITank> tanks = getWorld().getTank(getPos().offset(side));
                    if (tanks != null) {
						tanks.forEach(tank -> stock.theTank.fill(tank, 100, false));
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
				freight.cargoItems}
                */
				break;
			case LOCO_CONTROL: {
				Locomotive loco = this.getStockNearBy(Locomotive.class);
				if (loco != null) {
					int power = getWorld().getRedstone(getPos());

					switch (controlMode) {
						case THROTTLE:
							loco.setThrottle(power / 15f);
							break;
						case REVERSER:
							loco.setReverser((power / 14f - 0.5f) * 2);
							break;
						case BRAKE:
							loco.setTrainBrake(power / 15f);
							break;
						case HORN:
							loco.setHorn(40, AUTOMATED_PLAYER);
							break;
						case BELL:
							loco.setBell(10 * power);
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

				switch (this.detectorMode) {
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
				break;
			case ACTUATOR: {
				EntityRollingStock stock = this.getStockNearBy(EntityRollingStock.class);
				if (stock != null) {
					float value = getWorld().getRedstone(getPos())/15f;
					for (Door d : stock.getDefinition().getModel().getDoors()) {
						if (d.type == Door.Types.EXTERNAL) {
							stock.setControlPosition(d, value);
						}
					}
				}
			}
				break;
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

	public Vec3i getParentReplaced() {
		if (this.replaced == null) {
			return null;
		}
		if (!this.replaced.hasKey("parent")) {
			return null;
		}
		return new Vec3i(this.replaced.getLong("parent")).add(getPos());
	}

	/**
	 * @return the newly applied state
	 */
	public SwitchState cycleSwitchForced() {
		TileRail tileSwitch = this.findSwitchParent();
		SwitchState newForcedState = SwitchState.NONE;

		if (tileSwitch != null) {
			newForcedState = SwitchState.values()[( tileSwitch.info.switchForced.ordinal() + 1 ) % SwitchState.values().length];
			setSwitchForced(newForcedState);
		}

		return newForcedState;
	}

	public void setSwitchForced(SwitchState newForcedState) {
		TileRail tileSwitch = this.findSwitchParent();

		if (tileSwitch != null && newForcedState != tileSwitch.info.switchForced) {
			tileSwitch.info =  tileSwitch.info.with(b -> b.switchForced = newForcedState);
		}
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
		if (cur.getPos().equals(cur.getParentTile().getPos())) {
			return null;
		}

		return findSwitchParent(cur.getParentTile());
	}

	/* NEW STUFF */

	private final SingleCache<Double, IBoundingBox> boundingBox =
			new SingleCache<>(height -> IBoundingBox.ORIGIN.expand(new Vec3d(1, height, 1)));
	@Override
	public IBoundingBox getBoundingBox() {
		if (this instanceof TileRailGag && (getParent() == null || !getWorld().isBlockLoaded(getParent()))) {
			// Accessing TEs (parent) in chunks that are currently loading can cause problems
			return boundingBox.get(getFullHeight() + 0.1);
		}
		return boundingBox.get(getFullHeight() + 0.1 * (getTrackGauge() / Gauge.STANDARD));
	}

	@Override
	public void onBreak() {
		if (this instanceof TileRail) {
			((TileRail) this).spawnDrops();
		}
		if (this.augment != null && this.augmentGauge != null) {
			ItemStack stack = new ItemStack(IRItems.ITEM_AUGMENT, 1);
			ItemRailAugment.Data data = new ItemRailAugment.Data(stack);
			data.augment = this.augment;
			data.gauge = this.augmentGauge;
			data.write();
			getWorld().dropItem(stack, getPos());
		}

		breakParentIfExists();
	}

	@Override
	public boolean onClick(Player player, Player.Hand hand, Facing facing, Vec3d hit) {
		ItemStack stack = player.getHeldItem(hand);
		if (stack.is(IRItems.ITEM_TRACK_EXCHANGER) && player.hasPermission(Permissions.EXCHANGE_TRACK)) {
			TileRail tileRail = this.getParentTile();
			ItemTrackExchanger.Data stackData = new ItemTrackExchanger.Data(stack);
			String track = stackData.track;
			ItemStack railBed = stackData.railBed;
			Gauge gauge = stackData.gauge;
			if (!track.equals(tileRail.info.settings.track) || !railBed.equals(tileRail.info.settings.railBed) || !gauge.equals(tileRail.info.settings.gauge)) {
				RailInfo info = tileRail.info.withSettings(b -> {
					b.track = track;
					b.railBed = railBed;
					b.gauge = gauge;
				});
				Audio.playSound(getWorld(), getPos(), StandardSound.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 0.3f, 0.2f);
				if (!player.isCreative()) {
					List<ItemStack> drops = tileRail.getDrops();
					List<ItemStack> newDrops = info.build(player, tileRail.getPos(), false);
					if (newDrops != null) { //cancel if player doesn't have all required items
						tileRail.info = info;

						if (drops != null) {
							for (ItemStack drop : drops) {
								getWorld().dropItem(drop, player.getPosition());
							}
						}
						tileRail.setDrops(newDrops);
						tileRail.markAllDirty();
					}
				} else {
					tileRail.info = info;
					tileRail.markAllDirty();
				}
			}
			return true;
		}
		if (stack.is(Fuzzy.NAME_TAG) && player.hasPermission(Permissions.AUGMENT_TRACK)) {
			if (getWorld().isServer) {
				if (player.isCrouching()) {
					stockTag = null;
					player.sendMessage(ChatText.RESET_AUGMENT_FILTER.getMessage());
				} else {
					stockTag = stack.getDisplayName();
					player.sendMessage(ChatText.SET_AUGMENT_FILTER.getMessage(stockTag));
				}
			}
			return true;
		}
		if (player.hasPermission(Permissions.AUGMENT_TRACK) && (stack.is(Fuzzy.REDSTONE_TORCH) || stack.is(Fuzzy.REDSTONE_DUST) || stack.is(Fuzzy.PISTON))) {
			PlayerMessage next = this.nextAugmentRedstoneMode(stack.is(Fuzzy.PISTON));
			if (next != null) {
				if (this.getWorld().isServer) {
					player.sendMessage(next);
				}
				return true;
			}
		}
		if (stack.is(Fuzzy.SNOW_LAYER)) {
			if (this.getWorld().isServer) {
				this.handleSnowTick();
			}
			return true;
		}
		if (stack.is(Fuzzy.SNOW_BLOCK)) {
			if (this.getWorld().isServer) {
				for (int i = 0; i < 8; i ++) {
					this.handleSnowTick();
				}
			}
			return true;
		}
		if (stack.isValidTool(ToolType.SHOVEL)) {
			if (this.getWorld().isServer) {
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

		TileRail parent = this.getParentTile();
		if (parent == null) {
			return stack;
		}
		parent.info.settings.write(stack);
		return stack;
	}

	@Override
	public void onNeighborChange(Vec3i neighbor) {
		TileRailBase te = this;

		if (getWorld().isClient) {
			return;
		}

		blockUpdate = true;

		TagCompound data = te.getReplaced();
		while (true) {
			TileRail teParent = te.getParentTile();
			if (teParent != null && teParent.getParentTile() != null) {
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
			te = (TileRailBase) getWorld().reconstituteBlockEntity(data);
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
			getWorld().setToAir(parent.getPos());
			//}
		}
	}

	@Override
	public boolean tryBreak(Player player) {
		if (player != null && !player.hasPermission(Permissions.BREAK_TRACK)) {
			return false;
		}
		try {
			TileRailBase rail = this;
			if (rail.getReplaced() != null) {
				// new object here is important
				TileRailGag newGag = (TileRailGag) getWorld().reconstituteBlockEntity(rail.getReplaced());
				if (newGag == null) {
					return true;
				}

				while(true) {
					if (newGag.getParent() != null && getWorld().hasBlockEntity(newGag.getParent(), TileRail.class)) {
						getWorld().setBlockEntity(getPos(), newGag);
						rail.breakParentIfExists();
						return false;
					}
					// Only do replacement if parent still exists

					TagCompound data = newGag.getReplaced();
					if (data == null) {
						break;
					}

					newGag = (TileRailGag) getWorld().reconstituteBlockEntity(data);
					if (newGag == null) {
						break;
					}
				}
			}
		} catch (StackOverflowError ex) {
			ImmersiveRailroading.error("Invalid recursive rail block at %s", getPos());
			ImmersiveRailroading.catching(ex);
			getWorld().setToAir(getPos());
		}
		return true;
	}

    public boolean clacks() {
		return getParent() != null && getParentTile().clacks();
    }

	public float getBumpiness() {
		return getParent() != null ? getParentTile().getBumpiness() : 1;
	}

	public boolean isCog() {
		return getParentTile() != null ? getParentTile().isCog() : false;
	}

	public int getTicksExisted() {
		return ticksExisted;
	}

    public void stockOverhead(EntityMoveableRollingStock stock) {
		this.overhead = stock;
    }
}
