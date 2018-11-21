package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileRailPreview extends SyncdTileEntity {
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
		if (hasTileData || !world.isRemote) {
			RailInfo info = new RailInfo(world, item, placementInfo, customInfo);
			return info;
		}
		return null;
	}
}
