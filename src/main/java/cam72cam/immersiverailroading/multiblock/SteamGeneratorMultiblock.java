package cam72cam.immersiverailroading.multiblock;

import cam72cam.immersiverailroading.fluids.IRFluids;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;

public class SteamGeneratorMultiblock extends Multiblock{

	public static final String NAME = "STEAM_GENERATOR";
	private static final BlockPos center = new BlockPos(1, 0, 1);
	private static final BlockPos power = new BlockPos(1,2,1);
	//private static final BlockPos water = new BlockPos(-1, 0, 1);
	private static final BlockPos watercap = new BlockPos(0, 0, 1);
	private static final BlockPos steamcap = new BlockPos(1, 0, 0);
	private static final BlockPos output = new BlockPos(1, 0, -1);
	
	static MultiblockComponent[][][] structure = new MultiblockComponent[3][3][3];

	static
	{
		for(int h = 0; h < 3; h++)
			for(int l = 0; l < 3; l++)
				for(int w = 0; w < 3; w++)
					structure[h][l][w] = L_ENG();
	}
	
	public SteamGeneratorMultiblock() {
		super(NAME, structure);
	}

	@Override
	public BlockPos placementPos() {
		return new BlockPos(0, 0, 1);
	}

	@Override
	protected MultiblockInstance newInstance(World world, BlockPos origin, Rotation rot) {
		return new SteamGeneratorInstance(world, origin, rot);
	}
	public class SteamGeneratorInstance extends MultiblockInstance {

		public SteamGeneratorInstance(World world, BlockPos origin, Rotation rot) {
			super(world, origin, rot);
		}
		
		
		
		@Override
		public boolean onBlockActivated(EntityPlayer player, EnumHand hand, BlockPos offset) {
			IFluidHandler water = getTile(watercap).getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
			IFluidHandler steam = getTile(steamcap).getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
			
			player.sendMessage(new TextComponentString("Steam: " + String.valueOf(10000 - steam.fill(new FluidStack(IRFluids.FLUID_STEAM, 10000), false)) + " / 10000"));
			player.sendMessage(new TextComponentString("Water: " + String.valueOf(10000 - water.fill(new FluidStack(FluidRegistry.WATER, 10000), false)) + " / 10000"));
			//player.sendMessage(new TextComponentString("WaterHandler: " + " / " + waterHandler.toString()));
			player.sendMessage(new TextComponentString("Origin: " + origin));
			player.sendMessage(new TextComponentString("Water: " + getPos(watercap)));
			player.sendMessage(new TextComponentString("Steam: " + getPos(steamcap)));
			player.sendMessage(new TextComponentString("Power: " + getPos(power)));
			return  true;
		}

		@Override
		public int getInvSize(BlockPos offset) {
			return 1;
		}

		private boolean isCenter(BlockPos offset) {
			return offset.equals(center);
		}

		@Override
		public boolean isRender(BlockPos offset) {
			return isCenter(offset);
		}

		@Override
		public void tick(BlockPos offset) {
			
			TileMultiblock powerTe = getTile(power);
			if (powerTe == null) {
				return;
			}
			
			try {
				IFluidHandler water = getTile(watercap).getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
				IFluidHandler steam = getTile(steamcap).getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
				
				int waterFilled = 10000 - water.fill(new FluidStack(FluidRegistry.WATER, 10000), false);
				int steamFilled = 10000 - steam.fill(new FluidStack(IRFluids.FLUID_STEAM, 10000), false);
				
				if(steamFilled < 10000 && waterFilled >= 10 && hasPower()) {
					IEnergyStorage energy = powerTe.getCapability(CapabilityEnergy.ENERGY, null);
					energy.extractEnergy(100, false);
					
					water.drain(new FluidStack(FluidRegistry.WATER, 10), true);
					steam.fill(new FluidStack(IRFluids.FLUID_STEAM, 10), true);
				}
				
				BlockPos outputPos = origin.add(output);
				IFluidHandler output = FluidUtil.getFluidHandler(world, outputPos, EnumFacing.SOUTH);
				if(output != null && steamFilled <= 10 && output.fill(new FluidStack(IRFluids.FLUID_STEAM, 10), false) >= 10) {
					output.fill(steam.drain(10, true), true);
				}
				
				/*//Input Water
				//BlockPos inputPos = origin.add(3, 0, 1);
				
				IFluidHandler input = FluidUtil.getFluidHandler(world, getPos(water), EnumFacing.SOUTH);
				
				final FluidStack inputLiquid = input.drain(1, false);
				
				int filled = input.fill(new FluidStack(FluidRegistry.WATER, 1), false);
				
				if(input != null && filled >= 1) {
					System.out.println("drained" + filled);
					input.drain(inputLiquid, true);
					//tankWater.fillrc(new FluidStack(FluidRegistry.WATER, 20), true);
					///waterHandler.fill(new FluidStack(FluidRegistry.WATER, 20), true);
				}
				
				//Transform Water to Steam
				if(tankWater.getFluidAmount() >= 10 && tankSteam.getFluidAmount() < 16000 && hasPower()) {
					
					IEnergyStorage energy = powerTe.getCapability(CapabilityEnergy.ENERGY, null);
					energy.extractEnergy(100, false);
					tankWater.drain(new FluidStack(FluidRegistry.WATER, 10), true);
					tankSteam.fill(new FluidStack(IRFluids.FLUID_STEAM, 5), true);
				}
				
				//Output Steam
				//BlockPos outputPos = getPos(output);
				IFluidHandler output = FluidUtil.getFluidHandler(world, getPos(steam), EnumFacing.SOUTH);
			
				if(output != null && tankSteam.canDrain()) {
				
					int accepted = output.fill(tankSteam.getFluid(), false);
				
					if(accepted > 0){
						output.fill(new FluidStack(tankSteam.getFluid().getFluid(), 5), true);
						tankSteam.drain(5, true);
					}
			
				}*/
			} catch (Exception e) {
				
			}
		}

		@Override
		public boolean canInsertItem(BlockPos offset, int slot, ItemStack stack) {
			return true;
		}

		@Override
		public boolean isOutputSlot(BlockPos offset, int slot) {
			return false;
		}

		@Override
		public int getSlotLimit(BlockPos offset, int slot) {
			return 1;
		}

		@Override
		public boolean canRecievePower(BlockPos offset) {
			return offset.equals(power);
		}
		
		@Override
		public boolean canHandleFluids(BlockPos offset) {
			return offset.equals(watercap) || offset.equals(steamcap);
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
