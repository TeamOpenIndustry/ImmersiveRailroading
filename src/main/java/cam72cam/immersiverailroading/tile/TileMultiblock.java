package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.multiblock.Multiblock;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileMultiblock extends SyncdTileEntity implements ITickable {
	
	public static TileMultiblock get(IBlockAccess world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		return te instanceof TileMultiblock ? (TileMultiblock) te : null;
	}
	
	private IBlockState replaced;
	private BlockPos main;
	private BlockPos offset;
	
	//Main
	private Rotation rotation;
	private String name;
	private long renderTicks;
	
	public void setAux(BlockPos main, BlockPos offset, IBlockState replaced) {
		this.main = main;
		this.offset = offset;
		this.replaced = replaced;
	}

	public void setMain(String name, Rotation rot) {
		this.name = name;
		this.rotation = rot;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt = super.writeToNBT(nbt);
		
		if (name != null) {
			nbt.setString("name", name);
			nbt.setInteger("rotation", rotation.ordinal());
		}
		nbt.setTag("replaced", NBTUtil.writeBlockState(new NBTTagCompound(), replaced));
		nbt.setTag("main", NBTUtil.createPosTag(main));
		nbt.setTag("offset", NBTUtil.createPosTag(offset));
		
		return nbt;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		
		if (nbt.hasKey("name")) {
			name = nbt.getString("name");
			rotation = Rotation.values()[nbt.getInteger("rotation")];
		}
		replaced = NBTUtil.readBlockState(nbt.getCompoundTag("replaced"));
		main = NBTUtil.getPosFromTag(nbt.getCompoundTag("main"));
		offset = NBTUtil.getPosFromTag(nbt.getCompoundTag("offset"));
	}

	@Override
	public void update() {
		this.renderTicks += 1;
	}

    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
    	return INFINITE_EXTENT_AABB;
    }
	
	public TileMultiblock getMain() {
		return get(world, main);
	}
	
	public BlockPos getOrigin() {
		return pos.subtract(offset);
	}
	
	public Multiblock getMultiblock() {
		TileMultiblock teMain = getMain();
		if (teMain != null) {
			return MultiblockRegistry.get(teMain.name);
		}
		return null;
	}
	
	public String getName() {
		TileMultiblock mt = getMain();
		if (mt != null) {
			return mt.name;
		}
		return name;
	}
	
	public long getRenderTicks() {
		return this.renderTicks;
	}

	/*
	 * Block Functions to pass on to the multiblock
	 */
	public void breakBlock() {
		TileMultiblock teMain = getMain();
		if (teMain != null) {
			Multiblock mb = MultiblockRegistry.get(teMain.name);
			mb.onBreak(world, getOrigin(), teMain.rotation);
		}
	}

	public boolean onBlockActivated(EntityPlayer player, EnumHand hand) {
		TileMultiblock teMain = getMain();
		if (teMain != null) {
			Multiblock mb = MultiblockRegistry.get(teMain.name);
			return mb.onBlockActivated(world, getOrigin(), teMain.rotation, player, hand, pos);
		}
		return false;
	}
	
	/*
	 * Event Handlers
	 */
	
	public void onBreak() {
		world.removeTileEntity(pos);
		world.setBlockState(pos, replaced);
	}

	public boolean isRender() {
		if (offset == null) {
			return false;
		}
		//HACK
		return offset.getX() == 2 && offset.getY() == 0 && offset.getZ() == 0;
	}

	public double getRotation() {
		TileMultiblock mt = getMain();
		if (mt == null) {
			return 0;
		}
		return mt.rotation.rotate(EnumFacing.EAST).getHorizontalAngle();
	}
}
