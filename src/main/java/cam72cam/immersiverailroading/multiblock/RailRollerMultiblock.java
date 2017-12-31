package cam72cam.immersiverailroading.multiblock;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
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
		return new RailRollerInstance(world, origin, rot);
	}
	public class RailRollerInstance extends MultiblockInstance {
		
		public RailRollerInstance(World world, BlockPos origin, Rotation rot) {
			super(world, origin, rot);
		}

		@Override
		public boolean onBlockActivated(EntityPlayer player, EnumHand hand, BlockPos offset) {
			if (!player.isSneaking()) {
				//TODO place and pick items
			}
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
			TileMultiblock craftingTe = getTile(crafter);
			if (craftingTe == null) {
				ImmersiveRailroading.warn("INVALID MULTIBLOCK TILE AT ", getPos(crafter));
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
			
			ItemStack input = inputTe.getContainer().getStackInSlot(0);
			ItemStack output = outputTe.getContainer().getStackInSlot(0);
			
			
			if (progress == 0) {
				// Try to start crafting
				if (input.getItem() == ImmersiveRailroading.ITEM_CAST_RAIL && output.isEmpty()) {
					progress = 100;
					craftingTe.setCraftProgress(100);
				}
			}
			
			if (progress == 1) {
				// Stop crafting
				ItemStack out = new ItemStack(ImmersiveRailroading.ITEM_RAIL, 10);
				ItemGauge.set(out, ItemGauge.get(input));
				outputTe.getContainer().setStackInSlot(0, out);
				input.shrink(1);
				inputTe.getContainer().setStackInSlot(0, input);;
			}
		}

		@Override
		public boolean canInsertItem(BlockPos offset, int slot, ItemStack stack) {
			return offset.equals(input) && stack.getItem() == ImmersiveRailroading.ITEM_CAST_RAIL;
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
	}
}
