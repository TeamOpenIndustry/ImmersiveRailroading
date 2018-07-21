package cam72cam.immersiverailroading.inventory;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public class FilteredStackHandler extends ItemStackHandler {
	public Map<Integer, SlotFilter> filter = new HashMap<Integer, SlotFilter>();
	public SlotFilter defaultFilter = SlotFilter.ANY;

    public FilteredStackHandler(int i) {
    	super(i);
	}
    
    public boolean checkSlot(int slot, @Nonnull ItemStack stack) {
    	SlotFilter chosen = defaultFilter;
    	if (filter.containsKey(slot)) {
    		chosen = filter.get(slot);
    	}
    	
    	return chosen.apply(stack);
    }

	@Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
    	if (checkSlot(slot, stack)) {
    		super.setStackInSlot(slot, stack);
    	}
    }
    
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
    	return checkSlot(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
    }
}
