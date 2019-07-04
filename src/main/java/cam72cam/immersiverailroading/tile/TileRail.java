package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class TileRail extends TileRailBase {

	public static TileRail get(IBlockAccess world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		return te instanceof TileRail ? (TileRail) te : null;
	}

	public RailInfo info;
	private List<ItemStack> drops;

	@Override
	@SideOnly(Side.CLIENT)
	public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
		if (info == null) {
			return new AxisAlignedBB(0, 0, 0, 0, 0, 0);
		}
		int length = info.settings.length;
		if (info.settings.type == TrackItems.CUSTOM && !info.customInfo.placementPosition.equals(info.placementInfo.placementPosition)) {
			length = (int) info.customInfo.placementPosition.distanceTo(info.placementInfo.placementPosition);
		}
		return new AxisAlignedBB(-length, -length, -length, length, length, length).offset(pos);
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
		
		List<EntityCoupleableRollingStock> ents = world.getEntitiesWithinAABB(EntityCoupleableRollingStock.class, new AxisAlignedBB(-info.settings.length, 0, -info.settings.length, info.settings.length, 5, info.settings.length).offset(this.getPos()));
		for(EntityCoupleableRollingStock stock : ents) {
			stock.triggerResimulate();
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		this.drops = new ArrayList<ItemStack>();
		if (nbt.hasKey("drops")) {
			NBTTagCompound dropNBT = nbt.getCompoundTag("drops");
			int count = dropNBT.getInteger("count");
			for (int i = 0; i < count; i++) {
				drops.add(new ItemStack(dropNBT.getCompoundTag("drop_" + i)));
			}
		}

		if (nbt.hasKey("info")) {
			info = new RailInfo(world, pos, nbt.getCompoundTag("info"));
		} else {
			// LEGACY
			// TODO REMOVE 2.0

			TrackItems type = TrackItems.valueOf(nbt.getString("type"));
			int length = nbt.getInteger("length");
			int quarters = nbt.getInteger("turnQuarters");
			ItemStack railBed = new ItemStack(nbt.getCompoundTag("railBed"));
			Gauge gauge = Gauge.from(nbt.getDouble("gauge"));

			if (type == TrackItems.SWITCH) {
				quarters = 4;
			}

			NBTTagCompound newPositionFormat = new NBTTagCompound();
			newPositionFormat.setDouble("x", nbt.getDouble("placementPositionX"));
			newPositionFormat.setDouble("y", nbt.getDouble("placementPositionY"));
			newPositionFormat.setDouble("z", nbt.getDouble("placementPositionZ"));
			nbt.setTag("placementPosition", newPositionFormat);

            PlacementInfo placementInfo = new PlacementInfo(nbt, pos);
            placementInfo = new PlacementInfo(placementInfo.placementPosition, placementInfo.direction, placementInfo.yaw, null);

			SwitchState switchState = SwitchState.values()[nbt.getInteger("switchState")];
			SwitchState switchForced = SwitchState.values()[nbt.getInteger("switchForced")];
			double tablePos = nbt.getDouble("tablePos");

			RailSettings settings = new RailSettings(gauge, "default", type, length, quarters, TrackPositionType.FIXED, TrackDirection.NONE, railBed, ItemStack.EMPTY, false, false);
			info = new RailInfo(world, settings, placementInfo, null, switchState, switchForced, tablePos);
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setTag("info", info.toNBT(pos));
		if (drops != null && drops.size() != 0) {
			NBTTagCompound dropNBT = new NBTTagCompound();
			dropNBT.setInteger("count", drops.size());
			for (int i = 0; i < drops.size(); i++) {
				dropNBT.setTag("drop_" + i, drops.get(i).serializeNBT());
			}
			nbt.setTag("drops", dropNBT);
		}
		return super.writeToNBT(nbt);
	}

	public void setDrops(List<ItemStack> drops) {
		this.drops = drops;
	}
	public void spawnDrops() {
		if (!world.isRemote) {
			if (drops != null && drops.size() != 0) {
				for(ItemStack drop : drops) {
					world.spawnEntity(new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), drop));
				}
				drops = new ArrayList<ItemStack>();
			}
		}
	}

	public double percentFloating() {
		int floating = 0;
		int total = 0;

		if (info.world == null) {
			return 0;
		}

		for (TrackBase track : info.getBuilder(new BlockPos(info.placementInfo.placementPosition)).getTracksForRender()) {
			BlockPos tpos = track.getPos();
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
