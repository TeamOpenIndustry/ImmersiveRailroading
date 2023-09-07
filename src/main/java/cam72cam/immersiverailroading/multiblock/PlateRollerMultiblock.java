package cam72cam.immersiverailroading.multiblock;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.mod.energy.IEnergy;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.sound.Audio;
import cam72cam.mod.sound.SoundCategory;
import cam72cam.mod.sound.StandardSound;
import cam72cam.mod.world.World;

public class PlateRollerMultiblock extends Multiblock {
	public static final String NAME = "PLATE_MACHINE";
	private static final Vec3i render = new Vec3i(2,0,0);
	private static final Vec3i crafter = new Vec3i(2,0,14);
	private static final Vec3i input = new Vec3i(2,0,0);
	private static final Vec3i output = new Vec3i(2,0,29);
	private static final Vec3i power = new Vec3i(1,4,14);
	
	private static FuzzyProvider[][][] componentGenerator() {
		FuzzyProvider[][][] result = new FuzzyProvider[30][][];
		
		FuzzyProvider[] bed = new FuzzyProvider[] {
				L_ENG(), S_SCAF(), S_SCAF(), S_SCAF(), L_ENG()
		};
		FuzzyProvider[] mid = new FuzzyProvider[] {
				L_ENG(), AIR, AIR, AIR, L_ENG()
		};
		FuzzyProvider[] top = new FuzzyProvider[] {
				H_ENG(), H_ENG(), H_ENG(), H_ENG(), H_ENG()
		};
		for (int i = 0; i < 30; i ++) {
			if (i >= 11 && i <= 18) {
				if (i >= 13 && i <=16) {
					if (i == 14) {
						result[i] = new FuzzyProvider[][] { bed, mid, top, { AIR, L_ENG(), L_ENG(), L_ENG(), AIR}, { AIR, H_ENG(), AIR, AIR, AIR } };
					} else {
						result[i] = new FuzzyProvider[][] { bed, mid, top, { AIR, L_ENG(), L_ENG(), L_ENG(), AIR} };
					}
				} else {
					result[i] = new FuzzyProvider[][] { bed, mid, top };
				}
			} else {
				result[i] = new FuzzyProvider[][] { bed };
			}
		}
		
		return result;
	}

	public PlateRollerMultiblock() {
		super(NAME, componentGenerator());
	}
	
	@Override
	public Vec3i placementPos() {
		return new Vec3i(2, 0, 0);
	}

	@Override
	protected MultiblockInstance newInstance(World world, Vec3i origin, Rotation rot) {
		return new PlateRollerInstance(world, origin, rot);
	}
	public class PlateRollerInstance extends MultiblockInstance {
		
		public PlateRollerInstance(World world, Vec3i origin, Rotation rot) {
			super(world, origin, rot);
		}

		@Override
		public boolean onBlockActivated(Player player, Player.Hand hand, Vec3i offset) {
			if (!player.isCrouching()) {
				ItemStack held = player.getHeldItem(hand);
				if (held.isEmpty()) {
					TileMultiblock outputTe = getTile(output);
					if (outputTe == null) {
						return false;
					}
					
					if (!outputTe.getContainer().get(0).isEmpty()) {
						if (world.isServer) {
							ItemStack outstack = outputTe.getContainer().get(0);
							world.dropItem(outstack, player.getPosition());
							outputTe.getContainer().set(0, ItemStack.EMPTY);
						}
						return true;
					}
				} else if (IRFuzzy.steelBlockOrFallback().matches(held)) {
					TileMultiblock inputTe = getTile(input);
					if (inputTe == null) {
						return false;
					}
					if (inputTe.getContainer().get(0).isEmpty()) {
						if (world.isServer) {
							ItemStack inputStack = held.copy();
							inputStack.setCount(1);
							inputTe.getContainer().set(0, inputStack);
							held.shrink(1);
							player.setHeldItem(hand, held);
						}
					}
					return true;
				}
				
				if (world.isClient) {
					Vec3i pos = getPos(crafter);
					GuiTypes.PLATE_ROLLER.open(player, pos);
				}
				return true;
			}
			return false;
		}

		@Override
		public boolean isRender(Vec3i offset) {
			return render.equals(offset);
		}

		@Override
		public int getInvSize(Vec3i offset) {
			return offset.equals(input) || offset.equals(output) ? 1 : 0;
		}

		@Override
		public void tick(Vec3i offset) {
			if (!offset.equals(crafter)) {
				return;
			}
			TileMultiblock craftingTe = getTile(crafter);
			if (craftingTe == null) {
				return;
			}
			
			TileMultiblock powerTe = getTile(power);
			if (powerTe == null) {
				return;
			}
			
			TileMultiblock inputTe = getTile(input);
			if (inputTe == null) {
				return;
			}
			
			TileMultiblock outputTe = getTile(output);
			if (outputTe == null) {
				return;
			}
			
			if (!hasPower()) {
				return;
			}
			
			if (world.isClient) {
				if (craftingTe.getRenderTicks() % 10 == 0 && craftingTe.getCraftProgress() != 0) {
					Audio.playSound(world, craftingTe.getPos(), StandardSound.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 1 * ConfigSound.SoundCategories.Machines.category(), 0.2f);
				}
				return;
			}
			
			// Decrement craft progress down to 0
			if (craftingTe.getCraftProgress() != 0) {
				IEnergy energy = powerTe.getEnergy(null);
				energy.extract(powerRequired(), false);
				craftingTe.setCraftProgress(Math.max(0, craftingTe.getCraftProgress() - 1));
			}

			float progress = craftingTe.getCraftProgress();
			
			ItemStack input = inputTe.getContainer().get(0);
			ItemStack output = outputTe.getContainer().get(0);
			
			
			if (progress == 0) {
				// Try to start crafting
				if (IRFuzzy.steelBlockOrFallback().matches(input) && output.isEmpty() && !craftingTe.getCraftItem().isEmpty()) {
					input.setCount(input.getCount() - 1);
					inputTe.getContainer().set(0, input);;
					progress = 100;
					craftingTe.setCraftProgress(100);
				}
			}
			
			if (progress == 1) {
				// Stop crafting
				outputTe.getContainer().set(0, craftingTe.getCraftItem().copy());
			}
		}

		@Override
		public boolean canInsertItem(Vec3i offset, int slot, ItemStack stack) {
			return offset.equals(input) && IRFuzzy.steelBlockOrFallback().matches(stack);
		}

		@Override
		public boolean isOutputSlot(Vec3i offset, int slot) {
			return offset.equals(output);
		}

		@Override
		public int getSlotLimit(Vec3i offset, int slot) {
			return offset.equals(input) || offset.equals(output) ? 1 : 0;
		}

		@Override
		public boolean canRecievePower(Vec3i offset) {
			return offset.equals(power);
		}

		public boolean hasPower() {
			TileMultiblock powerTe = getTile(power);
			if (powerTe == null) {
				return false;
			}
			return powerTe.getEnergy(null).getCurrent() >= powerRequired();
		}
		private int powerRequired() {
			return (int) Math.ceil(32 * Config.ConfigBalance.machinePowerFactor);
		}
	}
}
