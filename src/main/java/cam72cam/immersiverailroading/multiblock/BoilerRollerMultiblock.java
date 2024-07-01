package cam72cam.immersiverailroading.multiblock;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.ItemPlate;
import cam72cam.immersiverailroading.items.ItemRollingStockComponent;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.library.PlateType;
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
import cam72cam.mod.world.World;

public class BoilerRollerMultiblock extends Multiblock {
	private static FuzzyProvider slab = () -> Fuzzy.STONE_SLAB;
	public static final String NAME = "BOILER_MACHINE";
	private static final Vec3i render = new Vec3i(2,0,0);
	private static final Vec3i power = new Vec3i(5,0,3);
	private static final Vec3i crafting = new Vec3i(2,0,4);
	
	private static FuzzyProvider[][][] componentGenerator() {
		FuzzyProvider[] bed = new FuzzyProvider[] {
				L_ENG(), L_ENG(), L_ENG(), L_ENG(), H_ENG(), H_ENG()
		};
		FuzzyProvider[] table = new FuzzyProvider[] {
				slab, slab, slab, slab, AIR, AIR
		};
		
		FuzzyProvider[][][] result = new FuzzyProvider[][][] {
			{ table },
			{ table },
			{ table },
			{ bed },
			{ bed },
			{ table },
			{ table },
			{ table },
		};
		return result;
	}

	public BoilerRollerMultiblock() {
		super(NAME, componentGenerator());
	}
	
	@Override
	public Vec3i placementPos() {
		return new Vec3i(2, 0, 0);
	}

	@Override
	protected MultiblockInstance newInstance(World world, Vec3i origin, Rotation rot) {
		return new BoilerRollerInstance(world, origin, rot);
	}
	public class BoilerRollerInstance extends MultiblockInstance {
		
		public BoilerRollerInstance(World world, Vec3i origin, Rotation rot) {
			super(world, origin, rot);
		}

		@Override
		public boolean onBlockActivated(Player player, Player.Hand hand, Vec3i offset) {
			if (world.isClient) {
				return false;
			}
			
			if (!player.isCrouching()) {
				ItemStack held = player.getHeldItem(hand);
				if (held.isEmpty()) {
					TileMultiblock craftTe = getTile(crafting);
					if (craftTe == null) {
						return false;
					}
					
					ItemStack outstack = craftTe.getContainer().get(1);
					if (!outstack.isEmpty()) {
						world.dropItem(outstack, player.getPosition());

						craftTe.getContainer().set(1, ItemStack.EMPTY);
					}
				} else if (held.is(IRItems.ITEM_PLATE) && new ItemPlate.Data(held).type == PlateType.BOILER) {
					TileMultiblock craftTe = getTile(crafting);
					if (craftTe == null) {
						return false;
					}
					if (craftTe.getContainer().get(0).isEmpty()) {
						ItemStack inputStack = held.copy();
						inputStack.setCount(1);
						craftTe.getContainer().set(0, inputStack);
						held.shrink(1);
						player.setHeldItem(hand, held);
					}
				}
			}
			return false;
		}

		@Override
		public boolean isRender(Vec3i offset) {
			return offset.equals(render);
		}

		@Override
		public int getInvSize(Vec3i offset) {
			return offset.equals(crafting) ? 2 : 0;
		}

        @Override
        public int getTankCapability(Vec3i offset) {
            return 0;
        }

        @Override
		public void tick(Vec3i offset) {
			if (!offset.equals(crafting)) {
				return;
			}
			TileMultiblock craftTe = getTile(crafting);
			if (craftTe == null) {
				return;
			}
			
			TileMultiblock powerTe = getTile(power);
			if (powerTe == null) {
				return;
			}
			
			if (!hasPower()) {
				return;
			}
			
			if (world.isClient) {
				if (craftTe.getRenderTicks() % 10 == 0 && craftTe.getCraftProgress() != 0) {
					Audio.playSound(world, craftTe.getPos(), StandardSound.BLOCK_ANVIL_PLACE, SoundCategory.MASTER, 1 * ConfigSound.SoundCategories.Machines.category(), 0.2f);
				}
				return;
			}
			
			// Decrement craft progress down to 0
			if (craftTe.getCraftProgress() != 0) {
				IEnergy energy = powerTe.getEnergy(null);
				energy.extract(powerRequired(), false);
				craftTe.setCraftProgress(Math.max(0, craftTe.getCraftProgress() - 1));
			}
			
			float progress = craftTe.getCraftProgress();
			
			ItemStack input = craftTe.getContainer().get(0);
			ItemStack output = craftTe.getContainer().get(1);
			
			
			if (progress == 0) {
				// Try to start crafting
				if (input.is(IRItems.ITEM_PLATE) && new ItemPlate.Data(input).type == PlateType.BOILER && output.isEmpty()) {
					progress = 100;
					craftTe.setCraftProgress(100);
				}
			}
			
			if (progress == 1) {
				// Stop crafting
				ItemStack out = new ItemStack(IRItems.ITEM_ROLLING_STOCK_COMPONENT, 1);
				ItemPlate.Data source = new ItemPlate.Data(input);
				ItemRollingStockComponent.Data data = new ItemRollingStockComponent.Data(out);
				data.def = source.def;
				data.gauge = source.gauge;
				data.componentType = ItemComponentType.BOILER_SEGMENT;
				data.write();
				craftTe.getContainer().set(1, out);
				input.shrink(1);
				craftTe.getContainer().set(0, input);
			}
		}

		@Override
		public boolean canInsertItem(Vec3i pos, int slot, ItemStack stack) {
			//TODO
			return false;
		}

        @Override
        public boolean canReceiveFluid(Vec3i offset) {
            return false;
        }

        @Override
        public boolean isItemOutputSlot(Vec3i offset, int slot) {
            //TODO
            return false;
        }

        @Override
        public boolean isFluidOutputSlot(Vec3i offset) {
            return false;
        }

		@Override
		public int getSlotLimit(Vec3i offset, int slot) {
			return offset.equals(crafting) ? 1 : 0;
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
		
		public boolean hasInput() {
			TileMultiblock craftTe = getTile(crafting);
			if (craftTe == null) {
				return false;
			}
			return !craftTe.getContainer().get(0).isEmpty();
		}

		public boolean hasOutput() {
			TileMultiblock craftTe = getTile(crafting);
			if (craftTe == null) {
				return false;
			}
			return !craftTe.getContainer().get(1).isEmpty();
		}

		private int powerRequired() {
			return (int) Math.ceil(32 * Config.ConfigBalance.machinePowerFactor);
		}
	}
}
