package cam72cam.immersiverailroading.multiblock;

import java.util.List;
import java.util.Map;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileMultiblock;

import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public abstract class Multiblock {
	// z y x
	private final MultiblockComponent[][][] components;
	private final String name;
	protected final List<BlockPos> componentPositions;
	
	protected static final MultiblockComponent AIR = new MultiblockComponent(Blocks.AIR);

	protected Multiblock(String name, MultiblockComponent[][][] components) {
		this.name = name;
		this.components = components;
		componentPositions = new ArrayList<BlockPos>();
		for (int z = 0; z < components.length; z++) {
			MultiblockComponent[][] zcomp = components[z];
			for (int y = 0; y < components[z].length; y++) {
				MultiblockComponent[] ycomp = zcomp[y];
				for (int x = 0; x < ycomp.length; x++) {
					componentPositions.add(new BlockPos(x, y, z));
				}
			}
		}
	}
	
	protected MultiblockComponent lookup(BlockPos offset) {
		return components[offset.getZ()][offset.getY()][offset.getX()];
	}
	
	private boolean checkValid(IBlockAccess world, BlockPos origin, BlockPos offset, Rotation rot) {
		BlockPos pos = origin.add(offset.rotate(rot));
		MultiblockComponent component = lookup(offset);
		return component.valid(world, pos);
	}
	
	public boolean tryCreate(World world, BlockPos pos) {
		for (BlockPos activationLocation : this.componentPositions) {
			for (Rotation rot : Rotation.values()) {
				BlockPos origin = pos.subtract(activationLocation.rotate(rot));
				boolean valid = true;
				for (BlockPos offset : this.componentPositions) {
					valid = valid && checkValid(world, origin, offset, rot);
				}
				if (valid) {
					instance(world, origin, rot).onCreate();
					return true;
				}
			}
		}
		return false;
	}
	
	public abstract BlockPos placementPos();
	public void place(World world, EntityPlayer player, BlockPos pos, Rotation rot) {
		Map<MultiblockComponent, Integer> missing = new HashMap<MultiblockComponent, Integer>();
		BlockPos origin = pos.subtract(this.placementPos().rotate(rot));
		for (BlockPos offset : this.componentPositions) {
			MultiblockComponent component = lookup(offset);
			BlockPos compPos = origin.add(offset.rotate(rot));
			if (!component.valid(world, compPos) && world.isAirBlock(compPos)) {
				if (!component.place(world, player, compPos)) {
					if (!missing.containsKey(component)) {
						missing.put(component, 0);
					}
					missing.put(component, missing.get(component)+1);
				}
			}
		}
		
		if (missing.size() != 0) {
			player.sendMessage(new TextComponentString("Missing: "));
			for (MultiblockComponent comp : missing.keySet()) {
				//TODO localize
				player.sendMessage(new TextComponentString(String.format("  - %d x %s", missing.get(comp), comp.name)));
			}
		}
	}
	
	public MultiblockInstance instance(World world, BlockPos origin, Rotation rot) {
		return newInstance(world, origin, rot);
	}
	
	protected abstract MultiblockInstance newInstance(World world, BlockPos origin, Rotation rot);
	public abstract class MultiblockInstance {
		protected final World world;
		protected final BlockPos origin;
		protected final Rotation rot;
		
		public MultiblockInstance(World world, BlockPos origin, Rotation rot) {
			this.world = world;
			this.origin = origin;
			this.rot = rot;
		}
		
		public void onCreate() {
			for (BlockPos offset : componentPositions) {
				MultiblockComponent comp = lookup(offset);
				if (comp == AIR) {
					continue;
				}
				
				BlockPos pos = getPos(offset);
				IBlockState origState = world.getBlockState(pos);
				
				world.setBlockState(pos, ImmersiveRailroading.BLOCK_MULTIBLOCK.getDefaultState());
				TileMultiblock te = TileMultiblock.get(world, pos);
				
				te.configure(name, rot, offset, origState);
			}
		}
		public abstract boolean onBlockActivated(EntityPlayer player, EnumHand hand, BlockPos offset);
		public abstract int getInvSize(BlockPos offset);
		public abstract boolean isRender(BlockPos offset);
		public abstract void tick(BlockPos offset);
		public abstract boolean canInsertItem(BlockPos offset, int slot, ItemStack stack);
		public abstract boolean isOutputSlot(BlockPos offset, int slot);
		public abstract int getSlotLimit(BlockPos offset, int slot);
		public abstract boolean canRecievePower(BlockPos offset);
		public void onBreak() {
			for (BlockPos offset : componentPositions) {
				MultiblockComponent comp = lookup(offset);
				if (comp == AIR) {
					continue;
				}
				BlockPos pos = getPos(offset);
				TileMultiblock te = TileMultiblock.get(world, pos);
				if (te == null) {
					world.destroyBlock(pos, true);
					continue;
				}
				te.onBreak();
			}
		}
		
		/*
		 * Helpers
		 */
		protected BlockPos getPos(BlockPos offset) {
			return origin.add(offset.rotate(rot));
		}
		
		protected TileMultiblock getTile(BlockPos offset) {
			return TileMultiblock.get(world, getPos(offset));
		}
	}
}
