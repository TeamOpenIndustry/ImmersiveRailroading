package cam72cam.immersiverailroading.multiblock;

import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.blocks.BlockTypes_MetalsAll;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.net.MultiblockSelectCraftPacket;
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

public class PlateRollerMultiblock extends Multiblock {
	private static MultiblockComponent steel = new MultiblockComponent(Blocks.IRON_BLOCK);
	public static final String NAME = "PLATE_MACHINE";
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
		MultiblockComponent[] mid = new MultiblockComponent[] {
				steel, AIR, AIR, AIR, steel
		};
		for (int i = 0; i < 30; i ++) {
			if (i >= 11 && i <= 18) {
				if (i >= 13 && i <=16) {
					if (i == 14) {
						result[i] = new MultiblockComponent[][] { bed, mid, bed, { AIR, steel, steel, steel, AIR}, { AIR, steel, AIR, AIR, AIR } };
					} else {
						result[i] = new MultiblockComponent[][] { bed, mid, bed, { AIR, steel, steel, steel, AIR} };
					}
				} else {
					result[i] = new MultiblockComponent[][] { bed, mid, bed };
				}
			} else {
				result[i] = new MultiblockComponent[][] { bed };
			}
		}
		
		return result;
	}

	public PlateRollerMultiblock() {
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
			if (!player.isSneaking()) {
				if (world.isRemote) {
					BlockPos pos = getPos(offset);
					player.openGui(ImmersiveRailroading.instance, GuiTypes.PLATE_ROLLER.ordinal(), world, pos.getX(), pos.getY(), pos.getZ());
				}
				return true;
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
		}

		@Override
		public boolean canInsertItem(BlockPos offset, int slot, ItemStack stack) {
			return offset.equals(input) && stack.isItemEqual(steelBlock());
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
		
		public void setCraftItem(ItemStack stack) {
			ImmersiveRailroading.net.sendToServer(new MultiblockSelectCraftPacket(getPos(crafter), stack));
		}

		public ItemStack getCraftItem() {
			TileMultiblock craftingTe = getTile(crafter);
			if (craftingTe == null) {
				ImmersiveRailroading.warn("INVALID MULTIBLOCK TILE AT ", getPos(crafter));
				return ItemStack.EMPTY;
			}
			return craftingTe.getCraftItem();
		}
	}
}
