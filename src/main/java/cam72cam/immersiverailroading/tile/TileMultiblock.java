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
	private BlockPos offset;
	private Rotation rotation;
	private String name;
	private long renderTicks;
	
	public void configure(String name, Rotation rot, BlockPos offset, IBlockState replaced) {
		this.name = name;
		this.rotation = rot;
		this.offset = offset;
		this.replaced = replaced;
		markDirty();
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt = super.writeToNBT(nbt);

		nbt.setString("name", name);
		nbt.setInteger("rotation", rotation.ordinal());
		nbt.setTag("replaced", NBTUtil.writeBlockState(new NBTTagCompound(), replaced));
		nbt.setTag("offset", NBTUtil.createPosTag(offset));
		
		return nbt;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		
		name = nbt.getString("name");
		rotation = Rotation.values()[nbt.getInteger("rotation")];
		replaced = NBTUtil.readBlockState(nbt.getCompoundTag("replaced"));
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
	
	public BlockPos getOrigin() {
		return pos.subtract(offset);
	}
	
	public Multiblock getMultiblock() {
		return MultiblockRegistry.get(name);
	}
	
	public String getName() {
		return name;
	}
	
	public long getRenderTicks() {
		return this.renderTicks;
	}

	/*
	 * Block Functions to pass on to the multiblock
	 */
	public void breakBlock() {
		Multiblock mb = MultiblockRegistry.get(name);
		mb.onBreak(world, getOrigin(), rotation);
	}

	public boolean onBlockActivated(EntityPlayer player, EnumHand hand) {
		Multiblock mb = MultiblockRegistry.get(name);
		return mb.onBlockActivated(world, getOrigin(), rotation, player, hand, pos);
	}
	
	/*
	 * Event Handlers
	 */
	
	public void onBreak() {
		world.removeTileEntity(pos);
		world.setBlockState(pos, replaced);
	}

	public boolean isRender() {
		//HACK
		return offset.getX() == 2 && offset.getY() == 0 && offset.getZ() == 0;
	}

	public double getRotation() {
		return rotation.rotate(EnumFacing.EAST).getHorizontalAngle();
	}
}
