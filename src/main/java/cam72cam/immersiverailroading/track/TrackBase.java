package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.track.BuilderBase.PosRot;
import cam72cam.immersiverailroading.util.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public abstract class TrackBase {
	public BuilderBase builder;

	protected int rel_x;
	protected int rel_y;
	protected int rel_z;
	private EnumFacing rel_rotation;
	private float height;

	protected Block block;

	private boolean flexible = false;

	private BlockPos parent;

	public boolean solidNotRequired;

	public TrackBase(BuilderBase builder, int rel_x, int rel_y, int rel_z, Block block, EnumFacing rel_rotation) {
		this.builder = builder;
		this.rel_x = rel_x;
		this.rel_y = rel_y;
		this.rel_z = rel_z;
		this.rel_rotation = rel_rotation;
		this.block = block;
	}

	@SuppressWarnings("deprecation")
	public boolean canPlaceTrack() {
		PosRot pos = getPos();
		IBlockState down = builder.world.getBlockState(pos.down());
		boolean downOK = (down.isTopSolid() || !Config.ConfigDamage.requireSolidBlocks && !builder.world.isAirBlock(pos.down())) || 
				(BlockUtil.canBeReplaced(builder.world, pos.down(), false) && builder.info.railBedFill.getItem() != Items.AIR) ||
				solidNotRequired || BlockUtil.isIRRail(builder.world, pos);
		return BlockUtil.canBeReplaced(builder.world, pos, flexible || builder.overrideFlexible) && downOK;
	}

	public TileEntity placeTrack() { 
		PosRot pos = getPos();
		System.out.println("1");
		System.out.println(builder.info.embankmentHeight);
		if (builder.info.railBedFill.getItem() != Items.AIR && BlockUtil.canBeReplaced(builder.world, pos.down(), false)) {
			System.out.println("2");
			if (builder.info.embankmentHeight > 0) {
				System.out.println("3");
				for (int x = 1; x < builder.info.embankmentHeight; x++) {
					System.out.println("4");
					if (BlockUtil.canBeReplaced(builder.world, pos.down(x), false)) {
						builder.world.setBlockState(pos.down(x), BlockUtil.itemToBlockState(builder.info.railBedFill));
					} else {
						break;
					}
					if (BlockUtil.canBeReplaced(builder.world, pos.down(x), false)) {
							builder.world.setBlockState(pos.down(x), BlockUtil.itemToBlockState(builder.info.railBedFill));
							System.out.println("5");
					}
					for (int i = 1; i < x; i++) {
						System.out.println("6");
						for (EnumFacing dir : EnumFacing.HORIZONTALS) {
							builder.world.setBlockState(pos.offset(dir, i).down(x), BlockUtil.itemToBlockState(builder.info.railBedFill));
						}
					}
				}
			} else {
				builder.world.setBlockState(pos.down(), BlockUtil.itemToBlockState(builder.info.railBedFill));
			}
		}
		
		NBTTagCompound replaced = null;
		
		IBlockState state = builder.world.getBlockState(pos);
		Block removed = state.getBlock();
		TileRailBase te = null;
		if (removed != null) {
			if (removed instanceof BlockRailBase) {
				te = TileRailBase.get(builder.world, pos);
				if (te != null) {					
					replaced = te.serializeNBT();
				}
			} else {				
				removed.dropBlockAsItem(builder.world, pos, state, 0);
			}
		}
		
		if (te != null) {
			te.setWillBeReplaced(true);
		}
		builder.world.setBlockState(pos, getBlockState(), 3);
		if (te != null) {
			te.setWillBeReplaced(false);
		}
		
		TileRailBase tr = TileRailBase.get(builder.world, pos);
		tr.setReplaced(replaced);
		if (parent != null) {
			tr.setParent(parent);
		} else {
			tr.setParent(builder.getParentPos());
		}
		tr.setHeight(getHeight());
		return tr;
	}
	
	public IBlockState getBlockState() {
		return block.getDefaultState();
	}
	public EnumFacing getFacing() {
		return getPos().getRotation();
	}

	public void moveTo(TrackBase trackBase) {
		rel_x = trackBase.rel_x;
		rel_y = trackBase.rel_y;
		rel_z = trackBase.rel_z;
	}

	
	public PosRot getPos() {
		return builder.convertRelativePositions(rel_x, rel_y, rel_z, rel_rotation);
	}
	
	public void setHeight(float height) {
		this.height = height;
	}
	public float getHeight() {
		return height;
	}
	public Gauge getGauge() {
		return builder.gauge;
	}

	public void setFlexible() {
		this.flexible  = true;
	}

	public boolean isFlexible() {
		return this.flexible;
	}

	public void overrideParent(BlockPos blockPos) {
		this.parent = builder.convertRelativePositions(blockPos.getX(), blockPos.getY(), blockPos.getZ(), rel_rotation);
	}
}
