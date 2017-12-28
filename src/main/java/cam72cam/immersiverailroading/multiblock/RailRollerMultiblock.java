package cam72cam.immersiverailroading.multiblock;

import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.blocks.BlockTypes_MetalsAll;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;

public class RailRollerMultiblock extends Multiblock {
	private static MultiblockComponent steel = new MultiblockComponent(Blocks.IRON_BLOCK);
	public static final String NAME = "RAIL_MACHINE";
	private static final BlockPos render = new BlockPos(2,0,0);
	private static final BlockPos crafter = new BlockPos(2,1,14);
	private static final BlockPos input = new BlockPos(2,0,0);
	private static final BlockPos output = new BlockPos(2,0,29);
	private static final BlockPos power = new BlockPos(1,4,14);
	
	private static MultiblockComponent[][][] componentGenerator() {
		MultiblockComponent[][][] result = new MultiblockComponent[30][][];
		
		MultiblockComponent[] bed = new MultiblockComponent[] {
				steel, steel, steel, steel, steel
		};
		for (int i = 0; i < 30; i ++) {
			if (i >= 11 && i <= 18) {
				if (i >= 13 && i <=16) {
					if (i == 14) {
						result[i] = new MultiblockComponent[][] { bed, bed, bed, { AIR, steel, steel, steel, AIR}, { AIR, steel, AIR, AIR, AIR } };
					} else {
						result[i] = new MultiblockComponent[][] { bed, bed, bed, { AIR, steel, steel, steel, AIR} };
					}
				} else {
					result[i] = new MultiblockComponent[][] { bed, bed, bed };
				}
			} else {
				result[i] = new MultiblockComponent[][] { bed };
			}
		}
		
		return result;
	}

	public RailRollerMultiblock() {
		super(NAME, componentGenerator());
	}
	
	@Override
	public BlockPos placementPos() {
		return input;
	}

	@Override
	protected MultiblockInstance newInstance(World world, BlockPos origin, Rotation rot) {
		return new PlateRollerInstance(world, origin, rot);
	}
	public class PlateRollerInstance extends MultiblockInstance {
		
		public PlateRollerInstance(World world, BlockPos origin, Rotation rot) {
			super(world, origin, rot);
		}

		@Override
		public boolean onBlockActivated(EntityPlayer player, EnumHand hand, BlockPos offset) {
			return false;
		}

		@Override
		public boolean isRender(BlockPos offset) {
			return render.equals(offset);
		}

		@Override
		public int getInvSize(BlockPos offset) {
			return offset.equals(input) || offset.equals(output) ? 1 : 0;
		}

		@Override
		public void tick(BlockPos offset) {
			if (!offset.equals(crafter)) {
				return;
			}
			TileMultiblock craftingTe = getTile(offset);
			if (craftingTe == null) {
				ImmersiveRailroading.warn("INVALID MULTIBLOCK TILE AT ", getPos(offset));
				return;
			}
			
			TileMultiblock powerTe = getTile(power);
			if (powerTe == null) {
				ImmersiveRailroading.warn("INVALID MULTIBLOCK TILE AT ", getPos(power));
				return;
			}
			
			TileMultiblock inputTe = getTile(input);
			if (inputTe == null) {
				ImmersiveRailroading.warn("INVALID MULTIBLOCK TILE AT ", getPos(input));
				return;
			}
			
			TileMultiblock outputTe = getTile(output);
			if (outputTe == null) {
				ImmersiveRailroading.warn("INVALID MULTIBLOCK TILE AT ", getPos(output));
				return;
			}
			
			if (!hasPower()) {
				return;
			}
			
			if (world.isRemote) {
				if (craftingTe.getRenderTicks() % 10 == 0 && craftingTe.getCraftProgress() != 0) {
					world.playSound(craftingTe.getPos().getX(), craftingTe.getPos().getY(), craftingTe.getPos().getZ(), SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 1.0f, 0.2f, false);
				}
				return;
			}
			
			// Decrement craft progress down to 0
			if (craftingTe.getCraftProgress() != 0) {
				IEnergyStorage energy = powerTe.getCapability(CapabilityEnergy.ENERGY, null);
				energy.extractEnergy(32, false);
				craftingTe.setCraftProgress(Math.max(0, craftingTe.getCraftProgress() - 1));
			}
			
			float progress = craftingTe.getCraftProgress();
			
			ItemStackHandler container = craftingTe.getContainer();
			
			ItemStack input = container.getStackInSlot(0);
			ItemStack output = container.getStackInSlot(1);
			
			
			if (progress == 0) {
				// Try to start crafting
				if (input.isItemEqual(steelBlock()) && output.isEmpty() && !craftingTe.getCraftItem().isEmpty()) {
					input.setCount(input.getCount() - 1);
					container.setStackInSlot(0, input);;
					progress = 100;
					craftingTe.setCraftProgress(100);
				}
			}
			
			if (progress == 1) {
				// Stop crafting
				container.setStackInSlot(1, craftingTe.getCraftItem().copy());
			}
		}

		@Override
		public boolean canInsertItem(BlockPos offset, int slot, ItemStack stack) {
			return offset.equals(input) && stack.equals(steelBlock());
		}

		@Override
		public boolean isOutputSlot(BlockPos offset, int slot) {
			return offset.equals(output);
		}

		@Override
		public int getSlotLimit(BlockPos offset, int slot) {
			return offset.equals(input) || offset.equals(output) ? 1 : 0;
		}

		@Override
		public boolean canRecievePower(BlockPos offset) {
			return offset.equals(power);
		}

		public boolean hasPower() {
			TileMultiblock powerTe = getTile(power);
			if (powerTe == null) {
				return false;
			}
			IEnergyStorage energy = powerTe.getCapability(CapabilityEnergy.ENERGY, null);
			return energy.getEnergyStored() > 32;
			
		}
		
		public ItemStack steelBlock() {
			return new ItemStack(IEContent.blockStorage,1, BlockTypes_MetalsAll.STEEL.getMeta());
		}
	}
}
