package cam72cam.immersiverailroading.multiblock;

import java.util.function.Function;

import cam72cam.immersiverailroading.util.OreHelper.OreAbstract;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Hand;
import cam72cam.mod.world.World;

public class MultiblockComponent {
	private final Function<ItemStack, Boolean> itemCheck;
	public final String name;
	protected final ItemStack def;

	public MultiblockComponent() {
		def = ItemStack.EMPTY;
		name = "none";
		this.itemCheck = ItemStack::isEmpty;
	}
	
	@SuppressWarnings("deprecation")
	public MultiblockComponent(OreAbstract ore) {
		ItemStack stack = ore.example();
		this.name = stack.getDisplayName();
		this.def = stack;
		this.itemCheck = (ItemStack tstack) -> ore.matches(tstack, false);
	}

	public MultiblockComponent(Fuzzy fuzzy) {
		this.name = fuzzy.example().getDisplayName();
		this.def = fuzzy.example();
		this.itemCheck = fuzzy::matches;
	}
	
	public boolean valid(World world, Vec3i pos) {
		return itemCheck.apply(world.getItemStack(pos));
	}

	public boolean place(World world, Player player, Vec3i pos) {
		if (player.isCreative()) {
			if (def != null) {
				world.setBlock(pos, def);
			}
			return true;
		} else {
			IInventory inv = player.getInventory();
			for (int slot = 0; slot < inv.getSlotCount(); slot++) {
				ItemStack stack = inv.get(slot);
				if (itemCheck.apply(stack)) {
					
					int count = stack.getCount();
					
					ItemStack backup = player.getHeldItem(Hand.PRIMARY).copy();
					player.setHeldItem(Hand.PRIMARY, stack.copy());
					ClickResult result = player.clickBlock(Hand.PRIMARY, pos, new Vec3d(0.5, 0, 0.5));
					player.setHeldItem(Hand.PRIMARY, backup);
					
					if (result == ClickResult.ACCEPTED) {
						if (inv.get(slot).getCount() == count) {
							//Decrement inv slot if not already decremented
							stack.setCount(stack.getCount() - 1);
							inv.set(slot, stack);
						}
						return true;
					}
				}
			}
			return false;
		}
	}
}
