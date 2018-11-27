package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemTrackBlueprint;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.net.PreviewRenderPacket;
import cam72cam.immersiverailroading.proxy.ChunkManager;
import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.track.BuilderCubicCurve;
import cam72cam.immersiverailroading.track.BuilderSwitch;
import cam72cam.immersiverailroading.track.IIterableTrack;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileRailPreview extends SyncdTileEntity implements ITickable {
	private int ticksAlive;
	private RailInfo info;

	public static TileRailPreview get(IBlockAccess world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		return te instanceof TileRailPreview ? (TileRailPreview) te : null;
	}

	private ItemStack item;
	private PlacementInfo placementInfo;
	private PlacementInfo customInfo;
	
	@Override
	@SideOnly(Side.CLIENT)
	public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared()
	{
		return Double.MAX_VALUE;
	}

	public ItemStack getItem() {
		return this.item;
	}
	
	public void setup(ItemStack stack, PlacementInfo info) {
		this.item = stack.copy();
		this.placementInfo = info;
		this.markDirty();
	}

	public void setItem(ItemStack stack) {
		this.item = stack.copy();
		this.markDirty();
	}

	public void setCustomInfo(PlacementInfo info) {
		this.customInfo = info;
		if (customInfo != null) {
			RailSettings settings = ItemTrackBlueprint.settings(item);
			double lx = Math.abs(customInfo.placementPosition.x - placementInfo.placementPosition.x);
			double lz = Math.abs(customInfo.placementPosition.z - placementInfo.placementPosition.z);
			double length;
			switch (settings.type) {
				case TURN:
					length = (lx + lz )/2+1;
					length *= 4d/settings.quarters;
					settings = settings.withLength((int) Math.round(length));
					break;
				case STRAIGHT:
				case SLOPE:
					length = Math.max(lx, lz) + 1;
					settings = settings.withLength((int) Math.round(length));
			}

			ItemTrackBlueprint.settings(item, settings);
		}
		this.markDirty();
	}
	
	public void setPlacementInfo(PlacementInfo info) {
		this.placementInfo = info;
		this.markDirty();
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		
		item = new ItemStack(nbt.getCompoundTag("item"));
		//TODO nbt legacy
		/*
		yawHead = nbt.getFloat("yawHead");
		hitX = nbt.getFloat("hitX");
		hitY = nbt.getFloat("hitY");
		hitZ = nbt.getFloat("hitZ");
		 */
		
		placementInfo = new PlacementInfo(nbt.getCompoundTag("placementInfo"));
		if (nbt.hasKey("customInfo")) {
			customInfo = new PlacementInfo(nbt.getCompoundTag("customInfo"));
		}
		info = new RailInfo(world, item, placementInfo, customInfo);
	}
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setTag("item", item.serializeNBT());
		nbt.setTag("placementInfo", placementInfo.toNBT());
		if (customInfo != null) {
			nbt.setTag("customInfo", customInfo.toNBT());
		}
		
		return super.writeToNBT(nbt);
	}
	
	public RailInfo getRailRenderInfo() {
		if (hasWorld() && info.world == null) {
			info = new RailInfo(world, item, placementInfo, customInfo);
		}
		return info;
	}

	public void markDirty() {
		super.markDirty();
        info = new RailInfo(world, item, placementInfo, customInfo);
        if (isMulti()) {
			ImmersiveRailroading.net.sendToAll(new PreviewRenderPacket(this));
		}
	}

	public boolean isMulti() {
		if (info.getBuilder() instanceof IIterableTrack) {
			return ((IIterableTrack)info.getBuilder()).getSubBuilders() != null;
		}
		return false;
	}

	@Override
	public void update() {
		if (!world.isRemote && isMulti()) {
			ChunkManager.flagEntityPos(world, pos);

			if (this.ticksAlive % 20 == 0) {
				ImmersiveRailroading.net.sendToAll(new PreviewRenderPacket(this));
			}
			this.ticksAlive ++;
		}
	}
}
