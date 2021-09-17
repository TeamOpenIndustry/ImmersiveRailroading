package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.items.ItemRollingStockComponent;
import cam72cam.immersiverailroading.items.ItemTabs;
import cam72cam.immersiverailroading.library.CraftingType;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.gui.helpers.ItemPickerGUI;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CraftPicker {
	private ItemPickerGUI stockSelector;
	private ItemPickerGUI itemSelector;
	private List<ItemStack> items;
	private Consumer<ItemStack> onChoose;

	public static void showCraftPicker(IScreenBuilder screen, ItemStack current, CraftingType craftType, Consumer<ItemStack> onChoose) {
		new CraftPicker(screen, current, craftType, onChoose);
	}
	
	private CraftPicker(IScreenBuilder screen, ItemStack current, CraftingType craftType, Consumer<ItemStack> onChoose) {
		this.onChoose = stack -> {
			screen.show();
			onChoose.accept(stack);
		};
		this.items = new ArrayList<>(IRItems.ITEM_ROLLING_STOCK_COMPONENT.getItemVariants());

        List<ItemStack> stock = new ArrayList<>();

        stock.addAll(IRItems.ITEM_ROLLING_STOCK.getItemVariants(ItemTabs.LOCOMOTIVE_TAB));
        stock.addAll(IRItems.ITEM_ROLLING_STOCK.getItemVariants(ItemTabs.PASSENGER_TAB));
        stock.addAll(IRItems.ITEM_ROLLING_STOCK.getItemVariants(ItemTabs.STOCK_TAB));

		List<ItemStack> toRemove = new ArrayList<ItemStack>();
		for (ItemStack item : items) {
			ItemRollingStockComponent.Data data = new ItemRollingStockComponent.Data(item);
			ItemComponentType comp = data.componentType;
			if (comp.isWooden(data.def)) {
				toRemove.add(item);
				continue;
			}
			boolean isCastable = craftType == CraftingType.CASTING && comp.crafting == CraftingType.CASTING_HAMMER;
			if (comp.crafting != craftType && !isCastable) {
				toRemove.add(item);
			}
		}
		items.removeAll(toRemove);


		stockSelector = new ItemPickerGUI(stock, this::onStockExit);
		toRemove = new ArrayList<>();
		for (ItemStack itemStock : stock) {
			boolean hasComponent = false;
			for (ItemStack item : items) {
				if (isPartOf(itemStock, item)) {
					hasComponent = true;
					break;
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
		
		if (craftType == CraftingType.CASTING) {
        	stock.add(new ItemStack(IRItems.ITEM_CAST_RAIL, 1));
        	stock.add(IRFuzzy.steelIngotOrFallback().example());
        	stock.add(IRFuzzy.steelBlockOrFallback().example());
	        stock.addAll(IRItems.ITEM_AUGMENT.getItemVariants(ItemTabs.MAIN_TAB));
		}
		stockSelector.setItems(stock);
		
		itemSelector = new ItemPickerGUI(new ArrayList<>(), this::onItemExit);
		if (current != null && current.is(IRItems.ITEM_ROLLING_STOCK_COMPONENT)) {
			itemSelector.choosenItem = current;
		}

		// Draw/init
		if (stockSelector.choosenItem != null) {
			setupItemSelector();
			if (itemSelector.hasOptions()) {
				itemSelector.show();
				return;
			}
		}
		stockSelector.show();
	}
	
	private boolean isPartOf(ItemStack stock, ItemStack item) {
		if (stock == null || item == null) {
			return false;
		}
		
    	if (!stock.is(IRItems.ITEM_ROLLING_STOCK)) {
    		return false;
    	}
    	if (!item.is(IRItems.ITEM_ROLLING_STOCK_COMPONENT)) {
    		return false;
    	}
    	return new ItemRollingStockComponent.Data(item).def == new ItemRollingStock.Data(stock).def;
    }
	
	private void setupItemSelector() {
		List<ItemStack> filteredItems = new ArrayList<>();
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
				itemSelector.show();
			} else {
				this.itemSelector.choosenItem = null;
	    		onChoose.accept(stack);
			}
		}
	}
	
	private void onItemExit(ItemStack stack) {
		if (stack == null) {
			stockSelector.show();
		} else {
			onChoose.accept(stack);
		}
	}
}
