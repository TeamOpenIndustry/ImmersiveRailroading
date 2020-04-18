package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackItems;
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

	@Override
    public IBoundingBox getBoundingBox() {
		if (info == null) {
			return null;
		}
		int length = info.settings.length;
		if (info.settings.type == TrackItems.CUSTOM && !info.customInfo.placementPosition.equals(info.placementInfo.placementPosition)) {
			length = (int) info.customInfo.placementPosition.distanceTo(info.placementInfo.placementPosition);
		}
		return IBoundingBox.from(pos).grow(new Vec3d(length, length, length));
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
		
		List<EntityCoupleableRollingStock> ents = world.getEntities((EntityCoupleableRollingStock stock) -> stock.getPosition().distanceTo(new Vec3d(pos)) < info.settings.length, EntityCoupleableRollingStock.class);
		for(EntityCoupleableRollingStock stock : ents) {
			stock.triggerResimulate();
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
		spawnDrops(new Vec3d(this.pos));
	}

	public void spawnDrops(Vec3d pos) {
		if (world.isServer) {
			if (drops != null && drops.size() != 0) {
				for(ItemStack drop : drops) {
					world.dropItem(drop, pos);
				}
				drops = new ArrayList<>();
			}
		}
	}

	public double percentFloating() {
		int floating = 0;
		int total = 0;

		if (info.settings == null) {
			return 0;
		}

		for (TrackBase track : info.getBuilder(world, new Vec3i(info.placementInfo.placementPosition).add(pos)).getTracksForRender()) {
			Vec3i tpos = track.getPos();
			total++;

			if (!world.isBlockLoaded(tpos)) {
				return 0;
			}
			if (!track.isDownSolid(false)) {
				floating++;
			}
		}
		return floating / (double)total;
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
}
