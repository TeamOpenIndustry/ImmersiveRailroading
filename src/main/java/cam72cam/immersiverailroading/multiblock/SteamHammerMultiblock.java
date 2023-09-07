package cam72cam.immersiverailroading.multiblock;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.items.ItemRollingStockComponent;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.ItemStackHandler;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.sound.Audio;
import cam72cam.mod.sound.SoundCategory;
import cam72cam.mod.sound.StandardSound;
import cam72cam.mod.world.World;

public class SteamHammerMultiblock extends Multiblock {
	private static FuzzyProvider piston = () -> Fuzzy.PISTON;
	public static final String NAME = "STEAM_HAMMER";
	private static final Vec3i center = new Vec3i(2,0,0);
	private static final Vec3i power = new Vec3i(2,5,0);

	public SteamHammerMultiblock() {
		super(NAME, new FuzzyProvider[][][] { // Z
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
	public Vec3i placementPos() {
		return new Vec3i(2, 0, 0);
	}

	@Override
	protected MultiblockInstance newInstance(World world, Vec3i origin, Rotation rot) {
		return new SteamHammerInstance(world, origin, rot);
	}
	public class SteamHammerInstance extends MultiblockInstance {
		
		public SteamHammerInstance(World world, Vec3i origin, Rotation rot) {
			super(world, origin, rot);
		}

		@Override
		public boolean onBlockActivated(Player player, Player.Hand hand, Vec3i offset) {
			if (isCenter(offset)) {
				if (world.isServer) {
					Vec3i pos = getPos(offset);
					GuiTypes.STEAM_HAMMER.open(player, pos);
				}
				return true;
			}
			return false;
		}
		
		private boolean isCenter(Vec3i offset) {
			return offset.equals(center);
		}

		@Override
		public boolean isRender(Vec3i offset) {
			return isCenter(offset);
		}

		@Override
		public int getInvSize(Vec3i offset) {
			return isCenter(offset) ? 2 : 0;
		}

		@Override
		public void tick(Vec3i offset) {
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
			
			if (world.isClient) {
				if (te.getRenderTicks() % 10 == 0 && te.getCraftProgress() != 0) {
					Audio.playSound(world, te.getPos(), StandardSound.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 1 * ConfigSound.SoundCategories.Machines.category(), 0.2f);
				}
				return;
			}
			
			// Decrement craft progress down to 0
			if (te.getCraftProgress() != 0) {
				powerTe.getEnergy(null).extract(powerRequired(), false);
				te.setCraftProgress(Math.max(0, te.getCraftProgress() - 1));
			}
			
			float progress = te.getCraftProgress();

			ItemStackHandler container = te.getContainer();
			
			ItemStack input = container.get(0);
			ItemStack output = container.get(1);

			if (progress == 0) {
				// Try to start crafting
				if (!input.isEmpty() && new ItemRollingStockComponent.Data(input).rawCast && output.isEmpty()) {
					te.setCraftProgress(100);
				}
			}
			
			if (progress == 1) {
				// Stop crafting
				ItemStack out = input.copy();
				out.setCount(1);
				ItemRollingStockComponent.Data data = new ItemRollingStockComponent.Data(out);
				data.rawCast = false;
				data.write();
				container.set(1, out);
				input.shrink(1);
				container.set(0, input);
				progress = 100;
			}
		}

		@Override
		public boolean canInsertItem(Vec3i offset, int slot, ItemStack stack) {
			return slot == 0 && new ItemRollingStockComponent.Data(stack).rawCast;
		}

		@Override
		public boolean isOutputSlot(Vec3i offset, int slot) {
			return slot == 1;
		}

		@Override
		public int getSlotLimit(Vec3i offset, int slot) {
			return isCenter(offset) ? 1 : 0;
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
