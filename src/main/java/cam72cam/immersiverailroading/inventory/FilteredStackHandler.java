package cam72cam.immersiverailroading.inventory;

import java.util.HashMap;
import java.util.Map;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.ItemStackHandler;

public class FilteredStackHandler extends ItemStackHandler {
	public Map<Integer, SlotFilter> filter = new HashMap<>();
	public SlotFilter defaultFilter = SlotFilter.ANY;

    public FilteredStackHandler(int i) {
    	super(i);
    	super.checkSlot = this::checkSlot;
	}
    
    private boolean checkSlot(int slot, ItemStack stack) {
    	if (stack.isEmpty()) {
    		return true;
    	}
    	SlotFilter chosen = defaultFilter;
    	if (filter.containsKey(slot)) {
    		chosen = filter.get(slot);
    	}

    	return chosen.apply(stack);
    }

}
