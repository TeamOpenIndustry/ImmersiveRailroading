package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.entity.physics.Simulation;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.MathUtil;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;

import java.util.ArrayList;
import java.util.List;

public class TileRail extends TileRailBase {
	@TagField("info")
	public RailInfo info;

	@TagField("tableIndex")
	private int tableIndex;

	@TagField(value = "drops", typeHint = ItemStack.class, mapper = DropsMapper.class)
	private List<ItemStack> drops;

	private IBoundingBox boundingBox;
	@Override
    public IBoundingBox getRenderBoundingBox() {
		if (info == null) {
			return IBoundingBox.ORIGIN;
		}
		if (boundingBox == null) {
			int length = info.settings.length;
			if (info.settings.type == TrackItems.CUSTOM && !info.customInfo.placementPosition.equals(info.placementInfo.placementPosition)) {
				length = (int) info.customInfo.placementPosition.distanceTo(info.placementInfo.placementPosition);
			}
			if (info.settings.type == TrackItems.TRANSFERTABLE) {
				//It is rectangular and length&width may differ a lot
				length = Math.max(info.settings.length, info.settings.transfertableEntrySpacing * info.settings.transfertableEntryCount);
			}
			boundingBox = IBoundingBox.ORIGIN.grow(new Vec3d(length, length, length));
		}
		return boundingBox;
	}
	
	@Override
	public double getRenderDistance() {
		return ConfigGraphics.TrackRenderDistance;
	}

	public void setSwitchState(SwitchState state) {
		if (state != info.switchState) {
			info = info.with(b -> b.switchState = state);
			this.markDirty();
		}
	}

	public void setTurnTablePosition(float angle) {

		angle = ((angle % 360) + 360) % 360;
		int slotsPerCircle = Config.ConfigBalance.AnglePlacementSegmentation * 4;
		// 0 -> slotsPerCircle
		int dest = Math.round((angle / (360f / slotsPerCircle)));

		// This is probably stupidly overcomplicated, but it works...
		int delta = MathUtil.deltaMod(tableIndex, dest, slotsPerCircle);
		int deltaOpp = MathUtil.deltaMod(tableIndex + slotsPerCircle/2, dest, slotsPerCircle);
		if (Math.abs(MathUtil.deltaMod(0, delta + slotsPerCircle, slotsPerCircle)) < Math.abs(MathUtil.deltaMod(0, deltaOpp + slotsPerCircle, slotsPerCircle))) {
			dest = (tableIndex + delta) % slotsPerCircle;
		} else {
			dest = (tableIndex + deltaOpp) % slotsPerCircle;
		}

		// Force 180
		if (dest == tableIndex) {
			dest = dest + slotsPerCircle/2;
		}

		// Normalize
		dest = (dest + slotsPerCircle) % slotsPerCircle;

		this.tableIndex = dest;
	}

	public void clickOnTransferTable(TileRail parent, Vec3i pos){
		int halfGauge = (int) Math.floor((parent.info.settings.gauge.value() * 1.1 + 0.5) / 2);
		int width = parent.info.settings.transfertableEntrySpacing * (parent.info.settings.transfertableEntryCount - 1) + halfGauge + 2;
		Vec3i mainOffset = new Vec3i(-width / 2, 1, parent.info.settings.length/2)
				             .rotate(Rotation.from(parent.info.placementInfo.facing()));

		Vec3i offset = pos.subtract(parent.getPos().subtract(mainOffset)).rotate(Rotation.from(parent.info.placementInfo.facing().getOpposite()));

		this.tableIndex = Math.max(0, Math.min(info.settings.transfertableEntryCount - 1, Math.round(Math.abs((float) offset.x) / this.info.settings.transfertableEntrySpacing)));
	}

	@Override
	public void update() {
		super.update();

		if(getWorld().isServer && info != null && info.settings.type.isTable()){
			boolean shouldUpdate = false;
			if (info.settings.type == TrackItems.TURNTABLE) {
				int slotsPerCircle = Config.ConfigBalance.AnglePlacementSegmentation * 4;
				float desiredPosition = (360f / slotsPerCircle) * tableIndex;
				double speed = Config.ConfigBalance.TurnTableSpeed;
				if (desiredPosition != info.tablePos) {
					if (Math.abs(desiredPosition - info.tablePos) < speed) {
						info = info.with(b -> b.tablePos = desiredPosition);
					} else {
						// Again, this math is horrific and is probably wayyyyy overcomplicated
						double dp = MathUtil.deltaAngle(info.tablePos + speed, desiredPosition);
						double dn = MathUtil.deltaAngle(info.tablePos - speed, desiredPosition);
						dp = MathUtil.deltaAngle(0, dp + 360);
						dn = MathUtil.deltaAngle(0, dn + 360);
						double delta = Math.abs(dp) < Math.abs(dn) ? speed : -speed;
						info = info.with(b -> b.tablePos = (((b.tablePos + delta) % 360) + 360) % 360);
					}
					shouldUpdate = true;
				}
			} else {
				//Must be transfer table
				float desiredPosition = tableIndex * info.settings.transfertableEntrySpacing;
				double speed = Config.ConfigBalance.TransferTableSpeed;
				if (desiredPosition != info.tablePos) {
					if (Math.abs(desiredPosition - info.tablePos) < speed * 2) {
						info = info.with(b -> b.tablePos = desiredPosition);
					} else {
						double delta = desiredPosition - info.tablePos < 0 ? -speed : speed;
						info = info.with(b -> b.tablePos += delta);
					}
					shouldUpdate = true;
				}
			}
			if(shouldUpdate){
				this.markDirty();
				int maxRange = Math.max(info.settings.length, info.settings.transfertableEntrySpacing * info.settings.transfertableEntryCount);
				List<EntityCoupleableRollingStock> ents = getWorld().getEntities((EntityCoupleableRollingStock stock) -> stock.getPosition().distanceTo(new Vec3d(getPos())) < maxRange, EntityCoupleableRollingStock.class);
				for(EntityCoupleableRollingStock stock : ents) {
					stock.states.forEach(state -> state.dirty = true);
					Simulation.forceQuickUpdates = true;
				}
			}
		}
	}

	private static class DropsMapper implements TagMapper<List<ItemStack>> {
		@Override
		public TagAccessor<List<ItemStack>> apply(Class<List<ItemStack>> type, String fieldName, TagField tag) {
			return new TagAccessor<>(
					(nbt, drops) -> {
						// TODO replace with standard List serializer
						if (drops != null && !drops.isEmpty()) {
							TagCompound dropNBT = new TagCompound();
							dropNBT.setInteger("count", drops.size());
							for (int i = 0; i < drops.size(); i++) {
								dropNBT.set("drop_" + i, drops.get(i).toTag());
							}
							nbt.set("drops", dropNBT);
						}
					},
					nbt -> {
						List<ItemStack> drops = new ArrayList<>();
						if (nbt.hasKey("drops")) {
							TagCompound dropNBT = nbt.get("drops");
							int count = dropNBT.getInteger("count");
							for (int i = 0; i < count; i++) {
								drops.add(new ItemStack(dropNBT.get("drop_" + i)));
							}
						}
						return drops;
					}
			);
		}
	}

	public void setDrops(List<ItemStack> drops) {
		this.drops = drops;
	}
	public List<ItemStack> getDrops () {
		return this.drops;
	}
	public void spawnDrops() {
		spawnDrops(new Vec3d(this.getPos()));
	}

	public void spawnDrops(Vec3d pos) {
		if (getWorld().isServer) {
			if (drops != null && !drops.isEmpty()) {
				for(ItemStack drop : drops) {
					getWorld().dropItem(drop, pos);
				}
				drops = new ArrayList<>();
			}
		}
	}

	private List<TrackBase> tracks;
	public double percentFloating() {
		int floating = 0;
		int total = 0;

		if (info.settings == null) {
			return 0;
		}

		if (tracks == null) {
			tracks = (info.settings.type == TrackItems.SWITCH ? info.withSettings(b -> b.type = TrackItems.STRAIGHT) : info).getBuilder(getWorld(), new Vec3i(info.placementInfo.placementPosition).add(getPos())).getTracksForFloating();
			// This is just terrible
			Vec3i offset = getPos().subtract(tracks.get(0).getPos());
			tracks = (info.settings.type == TrackItems.SWITCH ? info.withSettings(b -> b.type = TrackItems.STRAIGHT) : info).getBuilder(getWorld(), new Vec3i(info.placementInfo.placementPosition).add(getPos().add(offset))).getTracksForFloating();
		}


		for (TrackBase track : tracks) {
			Vec3i tpos = track.getPos();
			total++;

			if (!getWorld().isBlockLoaded(tpos) || !((getWorld().isBlock(tpos, IRBlocks.BLOCK_RAIL) || getWorld().isBlock(tpos, IRBlocks.BLOCK_RAIL_GAG)))) {
				return 0;
			}
			if (!track.isDownSolid(false)) {
				floating++;
			}
		}
		return floating / (double)total;
	}

	public void markAllDirty() {
		if (info.settings == null) {
			return;
		}
		percentFloating(); // initialize track cache

		for (TrackBase track : tracks) {
			Vec3i tpos = track.getPos();
			TileRailBase be = getWorld().getBlockEntity(tpos, TileRailBase.class);
			if (be != null) {
				be.railBedCache = info.settings.railBed;
				be.cachedGauge = info.settings.gauge.value();
				be.markDirty();
			}
		}
	}

	@Override
	public double getTrackGauge() {
		if (info == null) {
			return 0;
		}
		return info.settings.gauge.value();
	}


	@Override
	public void onBreak() {
		this.spawnDrops();
		super.onBreak();
	}

    @Override
    public boolean clacks() {
		if (info == null) {
			return false;
		}
		return DefinitionManager.getTrack(info.settings.track).clack;
    }

	@Override
	public float getBumpiness() {
		if (info == null) {
			return 1;
		}
		return DefinitionManager.getTrack(info.settings.track).bumpiness;
	}

	@Override
	public boolean isCog() {
		if (info == null) {
			return false;
		}
		return DefinitionManager.getTrack(info.settings.track).cog;
	}

	@Override
	public ItemStack getRenderRailBed() {
		if (info == null) {
			return null;
		}
		return info.settings.railBed;
	}
}
