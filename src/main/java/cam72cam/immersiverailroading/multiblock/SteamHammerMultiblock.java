package cam72cam.immersiverailroading.multiblock;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemRawCast;
import cam72cam.immersiverailroading.library.GuiTypes;
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

public class SteamHammerMultiblock extends Multiblock {
	private static MultiblockComponent piston = new MultiblockComponent(Blocks.PISTON);
	public static final String NAME = "STEAM_HAMMER";
	private static final BlockPos center = new BlockPos(2,0,0);
	private static final BlockPos power = new BlockPos(2,5,0);

	public SteamHammerMultiblock() {
		super(NAME, new MultiblockComponent[][][] { // Z
			{ // Y
				{ //X
					L_ENG(), AIR, STEEL(), AIR, L_ENG()
				},
				{
					L_ENG(), AIR, AIR, AIR, L_ENG()
				},
				{
					L_ENG(), L_ENG(), H_ENG(), L_ENG(), L_ENG()
				},
				{
					AIR, L_ENG(), H_ENG(), L_ENG(), AIR
				},
				{
					AIR, AIR, piston, AIR, AIR
				},
				{
					AIR, AIR, H_ENG(), AIR, AIR
				}
			}
		});
	}
	
	@Override
	public BlockPos placementPos() {
		return new BlockPos(2, 0, 0);
	}

	@Override
	protected MultiblockInstance newInstance(World world, BlockPos origin, Rotation rot) {
		return new SteamHammerInstance(world, origin, rot);
	}
	public class SteamHammerInstance extends MultiblockInstance {
		
		public SteamHammerInstance(World world, BlockPos origin, Rotation rot) {
			super(world, origin, rot);
		}

		@Override
		public boolean onBlockActivated(EntityPlayer player, EnumHand hand, BlockPos offset) {
			if (isCenter(offset)) {
				if (!world.isRemote) {
					BlockPos pos = getPos(offset);
					player.openGui(ImmersiveRailroading.instance, GuiTypes.STEAM_HAMMER.ordinal(), world, pos.getX(), pos.getY(), pos.getZ());
				}
				return true;
			}
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
			return isCenter(offset) ? 2 : 0;
		}

		@Override
		public void tick(BlockPos offset) {
			if (!isCenter(offset)) {
				return;
			}
			TileMultiblock te = getTile(offset);
			if (te == null) {
				return;
			}
			
			TileMultiblock powerTe = getTile(power);
			if (powerTe == null) {
				return;
			}
			
			if (!hasPower()) {
				return;
			}
			
			if (world.isRemote) {
				if (te.getRenderTicks() % 10 == 0 && te.getCraftProgress() != 0) {
					world.playSound(te.getPos().getX(), te.getPos().getY(), te.getPos().getZ(), SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 1.0f, 0.2f, false);
				}
				return;
			}
			
			// Decrement craft progress down to 0
			if (te.getCraftProgress() != 0) {
				IEnergyStorage energy = powerTe.getCapability(CapabilityEnergy.ENERGY, null);
				energy.extractEnergy(32, false);
				te.setCraftProgress(Math.max(0, te.getCraftProgress() - 1));
			}
			
			float progress = te.getCraftProgress();
			
			ItemStackHandler container = te.getContainer();
			
			ItemStack input = container.getStackInSlot(0);
			ItemStack output = container.getStackInSlot(1);
			
			
			if (progress == 0) {
				// Try to start crafting
				if (!input.isEmpty() && ItemRawCast.get(new cam72cam.mod.item.ItemStack(input)) && output.isEmpty()) {
					te.setCraftProgress(100);
				}
			}
			
			if (progress == 1) {
				// Stop crafting
				ItemStack out = input.copy();
				out.setCount(1);
				ItemRawCast.set(new cam72cam.mod.item.ItemStack(out), false);
				container.setStackInSlot(1, out);
				input.shrink(1);
				container.setStackInSlot(0, input);;
				progress = 100;
			}
		}

		@Override
		public boolean canInsertItem(BlockPos offset, int slot, ItemStack stack) {
			return slot == 0 && ItemRawCast.get(new cam72cam.mod.item.ItemStack(stack));
		}

		@Override
		public boolean isOutputSlot(BlockPos offset, int slot) {
			return slot == 1;
		}

		@Override
		public int getSlotLimit(BlockPos offset, int slot) {
			return isCenter(offset) ? 1 : 0;
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
