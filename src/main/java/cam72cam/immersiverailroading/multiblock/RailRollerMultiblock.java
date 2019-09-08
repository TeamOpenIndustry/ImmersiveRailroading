package cam72cam.immersiverailroading.multiblock;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.energy.IEnergy;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.sound.Audio;
import cam72cam.mod.sound.SoundCategory;
import cam72cam.mod.sound.StandardSound;
import cam72cam.mod.util.Hand;
import cam72cam.mod.world.World;

public class RailRollerMultiblock extends Multiblock {
	public static final String NAME = "RAIL_MACHINE";
	private static final Vec3i render = new Vec3i(0,0,0);
	private static final Vec3i crafter = new Vec3i(1,1,15);
	private static final Vec3i input = new Vec3i(1,0,0);
	private static final Vec3i output = new Vec3i(1,0,29);
	private static final Vec3i power = new Vec3i(1,2,15);
	
	private static Fuzzy[][][] componentGenerator() {
		Fuzzy[][][] result = new Fuzzy[30][][];
		
		Fuzzy[] bed = new Fuzzy[] {
				S_SCAF(), S_SCAF()
		};
		for (int i = 0; i < 30; i ++) {
			if (i >= 11 && i <= 18) {
				result[i] = new Fuzzy[][] { {
					L_ENG(), L_ENG()
				}, {
					H_ENG(), H_ENG()
				}, {
					L_ENG(), L_ENG()
				}};
			} else {
				result[i] = new Fuzzy[][] { bed };
			}
		}
		
		return result;
	}

	public RailRollerMultiblock() {
		super(NAME, componentGenerator());
	}
	
	@Override
	public Vec3i placementPos() {
		return input;
	}

	@Override
	protected MultiblockInstance newInstance(World world, Vec3i origin, Rotation rot) {
		return new RailRollerInstance(world, origin, rot);
	}
	public class RailRollerInstance extends MultiblockInstance {
		
		public RailRollerInstance(World world, Vec3i origin, Rotation rot) {
			super(world, origin, rot);
		}

		@Override
		public boolean onBlockActivated(Player player, Hand hand, Vec3i offset) {
			if (world.isClient) {
				return false;
			}
			if (!player.isCrouching()) {
				ItemStack held = player.getHeldItem(hand);
				if (held.isEmpty() && outputFull()) {
					TileMultiblock outputTe = getTile(output);
					if (outputTe == null) {
						return false;
					}

					ItemStack outstack = outputTe.getContainer().get(0);
					world.dropItem(outstack, player.getPosition());
					outputTe.getContainer().set(0, ItemStack.EMPTY);
				} else if (held.is(IRItems.ITEM_CAST_RAIL)) {
					TileMultiblock inputTe = getTile(input);
					if (inputTe == null) {
						return false;
					}
					if (inputTe.getContainer().get(0).isEmpty()) {
						ItemStack inputStack = held.copy();
						inputStack.setCount(1);
						inputTe.getContainer().set(0, inputStack);
						held.shrink(1);
						player.setHeldItem(hand, held);
					}
				}
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
					Audio.playSound(craftingTe.pos, StandardSound.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 1, 0.2f);
				}
				return;
			}
			
			// Decrement craft progress down to 0
			if (craftingTe.getCraftProgress() != 0) {
				IEnergy energy = powerTe.getEnergy(null);
				energy.extractEnergy(32, false);
				craftingTe.setCraftProgress(Math.max(0, craftingTe.getCraftProgress() - 1));
			}
			
			float progress = craftingTe.getCraftProgress();
			
			ItemStack input = inputTe.getContainer().get(0);
			ItemStack output = outputTe.getContainer().get(0);
			
			
			if (progress == 0) {
				// Try to start crafting
				if (input.is(IRItems.ITEM_CAST_RAIL) && output.isEmpty()) {
					progress = 100;
					craftingTe.setCraftProgress(100);
				}
			}
			
			if (progress == 1) {
				// Stop crafting
				ItemStack out = new ItemStack(IRItems.ITEM_RAIL, 10);
				ItemGauge.set(out, ItemGauge.get(input));
				outputTe.getContainer().set(0, out);
				input.shrink(1);
				inputTe.getContainer().set(0, input);
			}
		}

		@Override
		public boolean canInsertItem(Vec3i offset, int slot, ItemStack stack) {
			return offset.equals(input) && stack.is(IRItems.ITEM_CAST_RAIL);
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
			return powerTe.getEnergy(null).getEnergyStored() > 32;

		}

		public int getCraftProgress() {
			TileMultiblock craftingTe = getTile(crafter);
			if (craftingTe == null) {
				return 0;
			}
			return craftingTe.getCraftProgress();
		}

		public boolean outputFull() {
			TileMultiblock outputTe = getTile(output);
			if (outputTe == null) {
				return false;
			}
			return !outputTe.getContainer().get(0).isEmpty();
		}
	}
}
