package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.item.ItemStack;
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
			boundingBox = IBoundingBox.ORIGIN.grow(new Vec3d(length, length, length));
		}
		return boundingBox;
	}
	
	@Override
	public double getRenderDistance() {
		return 8*32;
	}

	public void setSwitchState(SwitchState state) {
		if (state != info.switchState) {
			info = new RailInfo(info.settings, info.placementInfo, info.customInfo, state, info.switchForced, info.tablePos);
			this.markDirty();
		}
	}

	public void nextTablePos(boolean back) {
		float delta = 360 / (64f);
		double tablePos = ((int)(info.tablePos / delta)) * delta + (back ? delta : -delta);
		info = new RailInfo(info.settings, info.placementInfo, info.customInfo, info.switchState, info.switchForced, tablePos);
		this.markDirty();
		List<EntityCoupleableRollingStock> ents = getWorld().getEntities((EntityCoupleableRollingStock stock) -> stock.getPosition().distanceTo(new Vec3d(getPos())) < info.settings.length, EntityCoupleableRollingStock.class);
		for(EntityCoupleableRollingStock stock : ents) {
			stock.states.clear();
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
			if (drops != null && drops.size() != 0) {
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
			tracks = (info.settings.type == TrackItems.SWITCH ? info.withType(TrackItems.STRAIGHT) : info).getBuilder(getWorld(), new Vec3i(info.placementInfo.placementPosition).add(getPos())).getTracksForFloating();
			// This is just terrible
			Vec3i offset = getPos().subtract(tracks.get(0).getPos());
			tracks = (info.settings.type == TrackItems.SWITCH ? info.withType(TrackItems.STRAIGHT) : info).getBuilder(getWorld(), new Vec3i(info.placementInfo.placementPosition).add(getPos().add(offset))).getTracksForFloating();
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
