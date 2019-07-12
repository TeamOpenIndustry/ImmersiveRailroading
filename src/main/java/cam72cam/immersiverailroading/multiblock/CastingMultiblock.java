package cam72cam.immersiverailroading.multiblock;

import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.CraftingMachineMode;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.util.ItemCastingCost;
import cam72cam.immersiverailroading.util.ParticleUtil;
import cam72cam.mod.energy.IEnergy;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.BoundingBox;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Hand;
import cam72cam.mod.world.World;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;

public class CastingMultiblock extends Multiblock {
	private static MultiblockComponent STONE = new MultiblockComponent(Fuzzy.STONE_BRICK);
	private static MultiblockComponent SAND = new MultiblockComponent(Fuzzy.SAND);
	public static final String NAME = "CASTING";
	private static final Vec3i render = new Vec3i(3,3,7);
	private static final Vec3i fluid = new Vec3i(3,3,3);
	private static final Vec3i craft = new Vec3i(3,2,3);
	private static final Vec3i output = new Vec3i(3,2,14);
	private static final Vec3i power = new Vec3i(3,7,0);
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
	public Vec3i placementPos() {
		return new Vec3i(3, 0, 0);
	}

	@Override
	protected MultiblockInstance newInstance(World world, Vec3i origin, Rotation rot) {
		return new CastingInstance(world, origin, rot);
	}
	public class CastingInstance extends MultiblockInstance {
		
		public CastingInstance(World world, Vec3i origin, Rotation rot) {
			super(world, origin, rot);
		}

		@Override
		public boolean onBlockActivated(Player player, Hand hand, Vec3i offset) {
			TileMultiblock outTe = getTile(output);
			if (outTe == null) {
				return false;
			}
			TileMultiblock craftTe = getTile(craft);
			if (craftTe == null) {
				return false;
			}
			if (!outTe.getContainer().get(0).isEmpty()) {
				if (world.isServer) {
					world.dropItem(outTe.getContainer().get(0), player.getPosition());
					outTe.getContainer().set(0, ItemStack.EMPTY);
				}
			} else {
				if (world.isClient) {
					Vec3i pos = getPos(craft);
					player.internal.openGui(ImmersiveRailroading.instance, GuiTypes.CASTING.ordinal(), world.internal, pos.x, pos.y, pos.z);
				}
			}
			return true;
		}

		@Override
		public boolean isRender(Vec3i offset) {
			return render.equals(offset);
		}

		@Override
		public int getInvSize(Vec3i offset) {
			return output.equals(offset) ? 1 : 0;
		}

		@Override
		public void tick(Vec3i offset) {
			
			TileMultiblock powerTe = getTile(power);
			
			if (powerTe == null) {
				return;
			}
			
			IEnergy energy = powerTe.getEnergy(null);
			
			if (world.isClient) {
				if (offset.z > 7 && offset.y > 1 && isPouring()) {
					Vec3d pos = new Vec3d(getPos(offset)).add(0, 1, 0).add(0.5, 0.5, 0.5);
					if (Math.random() < 0.01) {
						ParticleUtil.spawnParticle(world.internal, EnumParticleTypes.SMOKE_NORMAL, pos.internal);
						ParticleUtil.spawnParticle(world.internal, EnumParticleTypes.SMOKE_NORMAL, pos.internal);
					}
					if (Math.random() < 0.001) {
						world.internal.playSound(pos.x, pos.y, pos.z, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1, 0.25f, false);
					}
				}
				
				return;
			}
			
			if (offset.equals(fluid)) {
				TileMultiblock fluidTe = getTile(fluid);
				if (fluidTe == null) {
					return;
				}

				List<ItemStack> dropped = world.getDroppedItems(IBoundingBox.from(getPos(offset.add(0, 1, 0))).grow(new Vec3d(3, 0, 3)));
				for (ItemStack stack : dropped) {
					ItemStack craftStack = stack.copy();
					int cost = ItemCastingCost.getCastCost(craftStack);
					if (cost != ItemCastingCost.BAD_CAST_COST) {
						cost /= craftStack.getCount();

						while (stack.getCount() != 0 && fluidTe.getCraftProgress() < max_volume + cost) {
							if (!hasPower()) {
								break;
							}
							energy.extractEnergy(32, false);
							stack.shrink(1);
							fluidTe.setCraftProgress(fluidTe.getCraftProgress() + cost);
						}
					} else {
						if (fluidTe.getCraftProgress() > 0) {
							stack.setCount(0);
						}
					}
				}
                /* TODO
				List<EntityLivingBase> living = world.getEntitiesWithinAABB(EntityLivingBase.class, bb.expand(0,2.5,0));
				for (EntityLivingBase alive : living) {
					alive.attackEntityFrom(new DamageSource("immersiverailroading:casting"), 5);
				}
				*/
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

				if (! outTe.getContainer().get(0).isEmpty()) {
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
					outTe.getContainer().set(0, item.copy());
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
		public boolean canInsertItem(Vec3i offset, int slot, ItemStack stack) {
			return false;
		}

		@Override
		public boolean isOutputSlot(Vec3i offset, int slot) {
			return false;
		}

		@Override
		public int getSlotLimit(Vec3i offset, int slot) {
			return output.equals(offset) ? 1 : 0;
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
