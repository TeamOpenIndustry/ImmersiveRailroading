package cam72cam.immersiverailroading.inventory;

import java.util.function.Function;

import cam72cam.immersiverailroading.util.BurnUtil;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;

public interface SlotFilter extends Function<ItemStack, Boolean> {
	public static final SlotFilter FLUID_CONTAINER = (ItemStack stack) -> FluidUtil.getFluidHandler(stack.copy()) != null;
	public static final SlotFilter BURNABLE = (ItemStack stack) -> BurnUtil.getBurnTime(stack) != 0;
	public static final SlotFilter NONE = ItemStack -> false;
	public static final SlotFilter ANY = ItemStack -> true;
}
