package cam72cam.immersiverailroading.multiblock;

import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.blocks.BlockTypes_MetalsAll;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

public class BoilerRollerMultiblock extends Multiblock {
	private static MultiblockComponent steel = new MultiblockComponent(Blocks.IRON_BLOCK);
	public static final String NAME = "BOILER_MACHINE";
	private static final BlockPos center = new BlockPos(2,0,0);
	private static final BlockPos power = new BlockPos(2,5,0);
	
	private static MultiblockComponent[][][] componentGenerator() {
		MultiblockComponent[] bed = new MultiblockComponent[] {
				steel, steel, steel, steel, steel, steel
		};
		
		MultiblockComponent[][][] result = new MultiblockComponent[][][] {
			{ bed },
			{ bed },
		};
		return result;
	}

	public BoilerRollerMultiblock() {
		super(NAME, componentGenerator());
	}
	
	@Override
	public BlockPos placementPos() {
		return new BlockPos(2, 0, 0);
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
		
		private boolean isCenter(BlockPos offset) {
			return offset.equals(center);
		}

		@Override
		public boolean isRender(BlockPos offset) {
			return isCenter(offset);
		}

		@Override
		public int getInvSize(BlockPos offset) {
			return 0;
		}

		@Override
		public void tick(BlockPos offset) {
		}

		@Override
		public boolean canInsertItem(BlockPos pos, int slot, ItemStack stack) {
			return false;
		}

		@Override
		public boolean isOutputSlot(BlockPos pos, int slot) {
			return false;
		}

		@Override
		public int getSlotLimit(BlockPos offset, int slot) {
			return 0;
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
