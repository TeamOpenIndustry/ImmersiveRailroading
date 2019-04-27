package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.TagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class TileRail extends TileRailBase {

	public RailInfo info;
	private List<ItemStack> drops;

	@Override
	@SideOnly(Side.CLIENT)
	public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
		if (info == null) {
			return new net.minecraft.util.math.AxisAlignedBB(0, 0, 0, 0, 0, 0);
		}
		int length = info.settings.length;
		if (info.settings.type == TrackItems.CUSTOM && !info.customInfo.placementPosition.equals(info.placementInfo.placementPosition)) {
			length = (int) info.customInfo.placementPosition.distanceTo(info.placementInfo.placementPosition);
		}
		return new net.minecraft.util.math.AxisAlignedBB(-length, -length, -length, length, length, length).offset(pos.internal);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return Math.pow(8*32, 2);
	}

	public void setSwitchState(SwitchState state) {
		if (state != info.switchState) {
			info = new RailInfo(info.world, info.settings, info.placementInfo, info.customInfo, state, info.switchForced, info.tablePos);
			this.markDirty();
		}
	}

	public void nextTablePos(boolean back) {
		double tablePos = (info.tablePos + 1.0/info.settings.length * (back ? 1 : -1)) % 8;
		info = new RailInfo(info.world, info.settings, info.placementInfo, info.customInfo, info.switchState, info.switchForced, tablePos);
		this.markDirty();
		
		List<EntityCoupleableRollingStock> ents = world.internal.getEntitiesWithinAABB(EntityCoupleableRollingStock.class, new net.minecraft.util.math.AxisAlignedBB(-info.settings.length, 0, -info.settings.length, info.settings.length, 5, info.settings.length).offset(this.getPos()));
		for(EntityCoupleableRollingStock stock : ents) {
			stock.triggerResimulate();
		}
	}

	@Override
	public void load(TagCompound nbt) {
		super.load(nbt);

		this.drops = new ArrayList<>();
		if (nbt.hasKey("drops")) {
			TagCompound dropNBT = nbt.get("drops");
			int count = dropNBT.getInteger("count");
			for (int i = 0; i < count; i++) {
				drops.add(new ItemStack(dropNBT.get("drop_" + i)));
			}
		}

		if (nbt.hasKey("info")) {
			info = new RailInfo(world, pos, nbt.get("info"));
		} else {
			// LEGACY
			// TODO REMOVE 2.0

			TrackItems type = TrackItems.valueOf(nbt.getString("type"));
			int length = nbt.getInteger("length");
			int quarters = nbt.getInteger("turnQuarters");
			ItemStack railBed = new ItemStack(nbt.get("railBed"));
			Gauge gauge = Gauge.from(nbt.getDouble("gauge"));

			if (type == TrackItems.SWITCH) {
				quarters = 4;
			}

			TagCompound newPositionFormat = new TagCompound();
			newPositionFormat.setDouble("x", nbt.getDouble("placementPositionX"));
			newPositionFormat.setDouble("y", nbt.getDouble("placementPositionY"));
			newPositionFormat.setDouble("z", nbt.getDouble("placementPositionZ"));
			nbt.set("placementPosition", newPositionFormat);

            PlacementInfo placementInfo = new PlacementInfo(nbt, pos);
            placementInfo = new PlacementInfo(placementInfo.placementPosition, placementInfo.direction, placementInfo.yaw, null);

			SwitchState switchState = SwitchState.values()[nbt.getInteger("switchState")];
			SwitchState switchForced = SwitchState.values()[nbt.getInteger("switchForced")];
			double tablePos = nbt.getDouble("tablePos");

			RailSettings settings = new RailSettings(gauge, "default", type, length, quarters, TrackPositionType.FIXED, TrackDirection.NONE, railBed, cam72cam.mod.item.ItemStack.EMPTY, false, false);
			info = new RailInfo(world, settings, placementInfo, null, switchState, switchForced, tablePos);
		}
	}

	@Override
	public void save(TagCompound nbt) {
		nbt.set("info", info.toNBT(pos));
		if (drops != null && drops.size() != 0) {
			TagCompound dropNBT = new TagCompound();
			dropNBT.setInteger("count", drops.size());
			for (int i = 0; i < drops.size(); i++) {
				dropNBT.set("drop_" + i, drops.get(i).toTag());
			}
			nbt.set("drops", dropNBT);
		}
		super.save(nbt);
	}

	public void setDrops(List<ItemStack> drops) {
		this.drops = drops;
	}
	public void spawnDrops() {
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

		if (info.world == null) {
			return 0;
		}

		for (TrackBase track : info.getBuilder(pos).getTracksForRender()) {
			Vec3i tpos = track.getPos();
			total++;

			if (!world.isBlockLoaded(tpos)) {
				return 0;
			}
			if (!track.isDownSolid()) {
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
}
