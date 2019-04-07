package cam72cam.immersiverailroading.multiblock;

import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.CraftingMachineMode;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.util.ItemCastingCost;
import cam72cam.immersiverailroading.util.ParticleUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

public class CastingMultiblock extends Multiblock {
	private static MultiblockComponent STONE = new MultiblockComponent(Blocks.STONEBRICK);
	private static MultiblockComponent SAND = new MultiblockComponent(Blocks.SAND);
	public static final String NAME = "CASTING";
	private static final BlockPos render = new BlockPos(3,3,7);
	private static final BlockPos fluid = new BlockPos(3,3,3);
	private static final BlockPos craft = new BlockPos(3,2,3);
	private static final BlockPos output = new BlockPos(3,2,14);
	private static final BlockPos power = new BlockPos(3,7,0);
	public static final double max_volume = 5 * 4 * 4.5 * 9;

	private static MultiblockComponent[][][] cast_blueprint() {
		MultiblockComponent[][][] bp = new MultiblockComponent[7+16][][];
		for (int z = 0; z < 7; z++) {
			MultiblockComponent[] base = new MultiblockComponent[] { AIR, AIR, AIR, AIR, AIR, AIR, AIR };
			if (z > 0 && z < 6) {
				if (z > 1 && z < 5) {
					base = new MultiblockComponent[] { AIR, S_SCAF(), S_SCAF(), S_SCAF(), S_SCAF(), S_SCAF(), AIR };
				} else {
					base = new MultiblockComponent[] { AIR, AIR, S_SCAF(), S_SCAF(), S_SCAF(), AIR, AIR };
				}
			}
			

			MultiblockComponent[] top = new MultiblockComponent[] { AIR, AIR, CASING(), H_ENG(), CASING(), AIR, AIR };
			MultiblockComponent[] topfirst = new MultiblockComponent[] { AIR, AIR, CASING(), H_ENG(), CASING(), AIR, AIR };
			if (z > 0 && z < 6) {
				if (z > 1 && z < 5) {
					top = new MultiblockComponent[] { CASING(), AIR, AIR, AIR, AIR, AIR, CASING() };
					topfirst = new MultiblockComponent[] { CASING(), CASING(), CASING(), H_ENG(), CASING(), CASING(), CASING() };
				} else {
					top = new MultiblockComponent[] { AIR, CASING(), AIR, AIR, AIR, CASING(), AIR };
					topfirst = new MultiblockComponent[] { AIR, CASING(), CASING(), H_ENG(), CASING(), CASING(), AIR };
				}
			}

			bp[z] = new MultiblockComponent[8][];
			for (int y = 0; y < 8; y++) {
				if (y < 3) {
					bp[z][y] = base;
				} else if (y == 3) {
					bp[z][y] = topfirst;
				} else {
					bp[z][y] = top;
				}
			}
		}
		
		for (int z = 7; z < 7+16; z++) {
			if (z == 7) {
				bp[z] = new MultiblockComponent[][] {
					{ AIR, STONE, STONE, STONE, STONE, STONE, AIR },
					{ AIR, STONE, STONE, STONE, STONE, STONE, AIR },
					{ AIR, STONE, STONE, STONE, STONE, STONE, AIR },
					{ AIR, AIR, AIR, STEEL(), AIR, AIR, AIR },
				};
			} else if (z == 7+16-1) {
				bp[z] = new MultiblockComponent[][] {
					{ AIR, STONE, STONE, STONE, STONE, STONE, AIR },
					{ AIR, STONE, STONE, STONE, STONE, STONE, AIR },
					{ AIR, STONE, STONE, STONE, STONE, STONE, AIR },
				};
			} else {
				bp[z] = new MultiblockComponent[][] {
					{ AIR, STONE, SAND, SAND, SAND, STONE, AIR },
					{ AIR, STONE, SAND, SAND, SAND, STONE, AIR },
					{ AIR, STONE, SAND, SAND, SAND, STONE, AIR },
				};
			}
		}
		
		return bp;
	}
	
	public CastingMultiblock() {
		super(NAME, cast_blueprint());
	}
	
	@Override
	public BlockPos placementPos() {
		return new BlockPos(3, 0, 0);
	}

	@Override
	protected MultiblockInstance newInstance(World world, BlockPos origin, Rotation rot) {
		return new CastingInstance(world, origin, rot);
	}
	public class CastingInstance extends MultiblockInstance {
		
		public CastingInstance(World world, BlockPos origin, Rotation rot) {
			super(world, origin, rot);
		}

		@Override
		public boolean onBlockActivated(EntityPlayer player, EnumHand hand, BlockPos offset) {
			TileMultiblock outTe = getTile(output);
			if (outTe == null) {
				return false;
			}
			TileMultiblock craftTe = getTile(craft);
			if (craftTe == null) {
				return false;
			}
			if (!outTe.getContainer().getStackInSlot(0).isEmpty()) {
				if (!world.isRemote) {
					world.spawnEntity(new EntityItem(world, player.posX, player.posY, player.posZ, outTe.getContainer().getStackInSlot(0)));
					outTe.getContainer().setStackInSlot(0, ItemStack.EMPTY);
				}
			} else {
				if (world.isRemote) {
					BlockPos pos = getPos(craft);
					player.openGui(ImmersiveRailroading.instance, GuiTypes.CASTING.ordinal(), world, pos.getX(), pos.getY(), pos.getZ());
				}
			}
			return true;
		}

		@Override
		public boolean isRender(BlockPos offset) {
			return render.equals(offset);
		}

		@Override
		public int getInvSize(BlockPos offset) {
			return output.equals(offset) ? 1 : 0;
		}

		@Override
		public void tick(BlockPos offset) {
			
			TileMultiblock powerTe = getTile(power);
			
			if (powerTe == null) {
				return;
			}
			
			IEnergyStorage energy = powerTe.getCapability(CapabilityEnergy.ENERGY, null);
			
			if (world.isRemote) {
				if (offset.getZ() > 7 && offset.getY() > 1 && isPouring()) {
					Vec3d pos = new Vec3d(getPos(offset).add(0, 1, 0)).addVector(0.5, 0.5, 0.5);
					if (Math.random() < 0.01) {
						ParticleUtil.spawnParticle(world, EnumParticleTypes.SMOKE_NORMAL, pos);
						ParticleUtil.spawnParticle(world, EnumParticleTypes.SMOKE_NORMAL, pos);
					}
					if (Math.random() < 0.001) {
						world.playSound(pos.x, pos.y, pos.z, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1, 0.25f, false);
					}
				}
				
				return;
			}
			
			if (offset.equals(fluid)) {
				TileMultiblock fluidTe = getTile(fluid);
				if (fluidTe == null) {
					return;
				}
				AxisAlignedBB bb = new AxisAlignedBB(getPos(offset.add(0, 1, 0))).grow(3, 0, 3);
				List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, bb);
				for (EntityItem item : items) {
					if (!hasPower()) {
						break;
					}
					ItemStack stack = item.getItem();
					ItemStack craftStack = stack.copy();
					int cost = ItemCastingCost.getCastCost(craftStack);
					if (cost != ItemCastingCost.BAD_CAST_COST) {
						cost /= craftStack.getCount();

						while(stack.getCount() != 0 && fluidTe.getCraftProgress() < max_volume + cost) {
							if (!hasPower()) {
								break;
							}
							energy.extractEnergy(32, false);
							stack.shrink(1);
							fluidTe.setCraftProgress(fluidTe.getCraftProgress() + cost);
						}
					} else {
						if (fluidTe.getCraftProgress() > 0) {
							world.removeEntity(item);
						}
					}
				}
				List<EntityLivingBase> living = world.getEntitiesWithinAABB(EntityLivingBase.class, bb.expand(0,2.5,0));
				for (EntityLivingBase alive : living) {
					alive.attackEntityFrom(new DamageSource("immersiverailroading:casting"), 5);
				}
			}
			
			if (offset.equals(craft)) {
				if (!hasPower()) {
					return;
				}
				
				TileMultiblock fluidTe = getTile(fluid);
				if (fluidTe == null) {
					return;
				}
				TileMultiblock craftTe = getTile(craft);
				if (craftTe == null) {
					return;
				}
				TileMultiblock outTe = getTile(output);
				if (outTe == null) {
					return;
				}
				
				ItemStack item = craftTe.getCraftItem();
				if (item == null || item.isEmpty()) {
					return;
				}
				
				CraftingMachineMode mode = craftTe.getCraftMode();
				if (mode == CraftingMachineMode.STOPPED) {
					return;
				}

				if (! outTe.getContainer().getStackInSlot(0).isEmpty()) {
					return;
				}
				
				int cost = ItemCastingCost.getCastCost(item);
				if (cost == ItemCastingCost.BAD_CAST_COST) {
					return;
				}
				
				if (craftTe.getCraftProgress() >= cost) {
					craftTe.setCraftProgress(0);
					if (mode == CraftingMachineMode.SINGLE) {
						craftTe.setCraftMode(CraftingMachineMode.STOPPED);
					}
					outTe.getContainer().setStackInSlot(0, item.copy());
				} else {
					if (craftTe.getRenderTicks() % 10 == 0) {
						if (fluidTe.getCraftProgress() > 0) {
							// Drain
							fluidTe.setCraftProgress(fluidTe.getCraftProgress() - 1);
							craftTe.setCraftProgress(craftTe.getCraftProgress() + 1);
						}
					}
				}
			}
			
			if (offset.equals(power)) {
				energy.extractEnergy(32, false);
			}
		}

		@Override
		public boolean canInsertItem(BlockPos offset, int slot, ItemStack stack) {
			return false;
		}

		@Override
		public boolean isOutputSlot(BlockPos offset, int slot) {
			return false;
		}

		@Override
		public int getSlotLimit(BlockPos offset, int slot) {
			return output.equals(offset) ? 1 : 0;
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

		public boolean isPouring() {
			TileMultiblock craftTe = getTile(craft);
			if (craftTe == null) {
				return false;
			}
			TileMultiblock fluidTe = getTile(fluid);
			if (fluidTe == null) {
				return false;
			}
			return craftTe.getCraftProgress() > 0 && fluidTe.getCraftProgress() > 0;
		}

		public double getSteelLevel() {
			TileMultiblock fluidTe = getTile(fluid);
			if (fluidTe == null) {
				return 0;
			}
			return fluidTe.getCraftProgress() / max_volume;
		}
		
		public ItemStack getCraftItem() {
			TileMultiblock craftingTe = getTile(craft);
			if (craftingTe == null) {
				return ItemStack.EMPTY;
			}
			return craftingTe.getCraftItem();
		}
	}
}
