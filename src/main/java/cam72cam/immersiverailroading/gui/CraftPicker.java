package cam72cam.immersiverailroading.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.items.ItemRollingStockComponent;
import cam72cam.immersiverailroading.items.ItemTabs;
import cam72cam.immersiverailroading.library.CraftingType;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public class CraftPicker extends GuiScreen {
	private ItemPickerGUI stockSelector;
	private ItemPickerGUI itemSelector;
	private NonNullList<ItemStack> items;
	private Consumer<ItemStack> onChoose;
	
	public CraftPicker(ItemStack current, CraftingType craftType, Consumer<ItemStack> onChoose) {
		this.onChoose = onChoose;
		this.items = NonNullList.create();
		
        ImmersiveRailroading.ITEM_ROLLING_STOCK_COMPONENT.getSubItems(ItemTabs.COMPONENT_TAB, items);
        
        NonNullList<ItemStack> stock = NonNullList.create();
        
        if (craftType == CraftingType.HAMMER) {
	        stock.add(new ItemStack(ImmersiveRailroading.ITEM_LARGE_WRENCH, 1));
	        stock.add(new ItemStack(ImmersiveRailroading.ITEM_HOOK, 1));
	        stock.add(new ItemStack(ImmersiveRailroading.ITEM_RAIL_BLOCK, 1));
	        ImmersiveRailroading.ITEM_AUGMENT.getSubItems(ItemTabs.MAIN_TAB, stock);
        }
        
        ImmersiveRailroading.ITEM_ROLLING_STOCK.getSubItems(ItemTabs.STOCK_TAB, stock);

		stockSelector = new ItemPickerGUI(stock, this::onStockExit);
		List<ItemStack> toRemove = new ArrayList<ItemStack>();
		for (ItemStack itemStock : stock) {
			boolean hasComponent = false;
			for (ItemStack item : items) {
				if (isPartOf(itemStock, item)) {
					if (ItemRollingStockComponent.getComponentType(item).crafting == craftType) {
						hasComponent = true;
						break;
					}
				}
			}
			if (!hasComponent) {
				toRemove.add(itemStock);
				continue;
			}
			if (isPartOf(itemStock, current)) {				
				stockSelector.choosenItem = itemStock;
			}
		}
		stock.removeAll(toRemove);
		
		toRemove = new ArrayList<ItemStack>();
		for (ItemStack item : items) {
			if (ItemRollingStockComponent.getComponentType(item).crafting != craftType) {
				toRemove.add(item);
			}
		}
		items.removeAll(toRemove);
		
		itemSelector = new ItemPickerGUI(NonNullList.create(), this::onItemExit);
		if (current != null && current.getItem() == ImmersiveRailroading.ITEM_ROLLING_STOCK_COMPONENT) {
			itemSelector.choosenItem = current;
		}
	}
	
	private boolean isPartOf(ItemStack stock, ItemStack item) {
		if (stock == null || item == null) {
			return false;
		}
		
    	if (stock.getItem() != ImmersiveRailroading.ITEM_ROLLING_STOCK) {
    		return false;
    	}
    	if (item.getItem() != ImmersiveRailroading.ITEM_ROLLING_STOCK_COMPONENT) {
    		return false;
    	}
    	return ItemRollingStockComponent.getDefinitionID(item).equals(ItemRollingStock.getDefinitionID(stock));
    }
	
	private void setupItemSelector() {
		NonNullList<ItemStack> filteredItems = NonNullList.create();
		for (ItemStack item : items) {
			if (isPartOf(stockSelector.choosenItem, item)) {
				filteredItems.add(item);
			}
		}
		itemSelector.setItems(filteredItems);
	}
	
	private void onStockExit(ItemStack stack) {
		if (stack == null) {
			onChoose.accept(null);
		} else {
			this.setupItemSelector();
			if (itemSelector.hasOptions()) {
        		this.mc.displayGuiScreen(itemSelector);
			} else {
				this.itemSelector.choosenItem = null;
	    		onChoose.accept(stack);
			}
		}
	}
	
	private void onItemExit(ItemStack stack) {
		if (stack == null) {
    		this.mc.displayGuiScreen(stockSelector);
		} else {
			onChoose.accept(stack);
		}
	}
    
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
    	if (stockSelector.choosenItem != null) {
    		setupItemSelector();
			if (itemSelector.hasOptions()) {
				this.mc.displayGuiScreen(itemSelector);
				return;
			}
    	}
		this.mc.displayGuiScreen(stockSelector);
    }
}
