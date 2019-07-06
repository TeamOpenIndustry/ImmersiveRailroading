package cam72cam.immersiverailroading.multiblock;

import java.util.List;
import java.util.Map;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.OreHelper;

import java.util.ArrayList;
import java.util.HashMap;

import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.text.PlayerMessage;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public abstract class Multiblock {
	// z y x
	private final MultiblockComponent[][][] components;
	private final String name;
	protected final List<BlockPos> componentPositions;
	
	protected static final MultiblockComponent AIR = new MultiblockComponent();
	protected static final MultiblockComponent STEEL() {
		return new MultiblockComponent(OreHelper.IR_STEEL_BLOCK);
	}
	
	protected static final MultiblockComponent CASING() {
		return new MultiblockComponent(OreHelper.IR_CASTING_CASING);
	}
	
	protected static final MultiblockComponent L_ENG() {
		return new MultiblockComponent(OreHelper.IR_LIGHT_ENG);
	}
	protected static final MultiblockComponent H_ENG() {
		return new MultiblockComponent(OreHelper.IR_HEAVY_ENG);
	}
	protected static final MultiblockComponent S_SCAF() {
		return new MultiblockComponent(OreHelper.IR_SCAFFOLDING);
	}

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
					if (!world.isRemote) {
						instance(world, origin, rot).onCreate();
					}
					return true;
				}
			}
		}
		return false;
	}
	
	public abstract BlockPos placementPos();
	public void place(World world, Player player, BlockPos pos, Rotation rot) {
		Map<String, Integer> missing = new HashMap<String, Integer>();
		BlockPos origin = pos.subtract(this.placementPos().rotate(rot));
		for (BlockPos offset : this.componentPositions) {
			MultiblockComponent component = lookup(offset);
			BlockPos compPos = origin.add(offset.rotate(rot));
			if (!component.valid(world, compPos)) {
				if (!world.isAirBlock(compPos)) {
					if (BlockUtil.canBeReplaced(cam72cam.mod.world.World.get(world), new Vec3i(compPos), false)) {
						world.destroyBlock(compPos, true);
					} else {
						player.sendMessage(ChatText.INVALID_BLOCK.getMessage(compPos.getX(), compPos.getY(), compPos.getZ()));
						return;
					}
				}
			}
		}
		
		for (BlockPos offset : this.componentPositions) {
			MultiblockComponent component = lookup(offset);
			BlockPos compPos = origin.add(offset.rotate(rot));
			if (!component.valid(world, compPos)) {
				if (!component.place(world, player.internal, compPos)) {
					if (!missing.containsKey(component.name)) {
						missing.put(component.name, 0);
					}
					missing.put(component.name, missing.get(component.name)+1);
				}
			}
		}
		
		if (missing.size() != 0) {
			player.sendMessage(ChatText.STOCK_MISSING.getMessage());
			for (String name : missing.keySet()) {
				player.sendMessage(PlayerMessage.direct(String.format("  - %d x %s", missing.get(name), name)));
			}
		}
	}
	
	public Map<BlockPos, IBlockState> blueprint() {
		Map<BlockPos, IBlockState> result = new HashMap<BlockPos, IBlockState>();
		for (BlockPos offset : this.componentPositions) {
			MultiblockComponent component = lookup(offset);
			result.put(offset, component.def);
		}
		return result;
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
				
				world.setBlockState(pos, IRBlocks.BLOCK_MULTIBLOCK.internal.getDefaultState());
				TileMultiblock te = cam72cam.mod.world.World.get(world).getTileEntity(new Vec3i(pos), TileMultiblock.class);
				
				te.configure(name, cam72cam.mod.math.Rotation.from(rot), new Vec3i(offset), origState);
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
				TileMultiblock te = cam72cam.mod.world.World.get(world).getTileEntity(new Vec3i(pos), TileMultiblock.class);
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
			TileMultiblock te = cam72cam.mod.world.World.get(world).getTileEntity(new Vec3i(getPos(offset)), TileMultiblock.class);
			if (te == null) {
				if (!world.isRemote) {
					ImmersiveRailroading.warn("Multiblock TE is null: %s %s %s %s", getPos(offset), offset, world.isRemote, this.getClass());
				}
				return null;
			}
			if (!te.isLoaded()) {
				if (!world.isRemote) {
					ImmersiveRailroading.info("Multiblock is still loading: %s %s %s %s", getPos(offset), offset, world.isRemote, this.getClass());
				}
				return null;
			}
			return te;
		}
	}
}
