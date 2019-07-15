package cam72cam.immersiverailroading.items;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemDefinition;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.items.nbt.ItemPlateType;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.library.PlateType;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.text.TextColor;

public class ItemPlate extends ItemBase {
	public ItemPlate() {
		super(ImmersiveRailroading.MODID, "item_plate", 64, ItemTabs.MAIN_TAB);
	}
	
	@Override
	public List<ItemStack> getItemVariants(CreativeTab tab) {
		List<ItemStack> items = new ArrayList<>();
        if (tab == null || tab.equals(ItemTabs.MAIN_TAB))
        {
        	for (PlateType plate : PlateType.values()) {
        		ItemStack stack = new ItemStack(this, 1);
        		ItemPlateType.set(stack, plate);
        		ItemGauge.set(stack, Gauge.from(Gauge.STANDARD));
        		
        		if (plate != PlateType.BOILER) {
        			applyCustomName(stack);
	                items.add(stack);
        		} else {
		        	for (String defID : DefinitionManager.getDefinitionNames()) {
		        		EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
		        		if (def.getItemComponents().contains(ItemComponentType.BOILER_SEGMENT) ) {
			        		stack = stack.copy();
			        		ItemDefinition.setID(stack, defID);
							applyCustomName(stack);
			                items.add(stack);
		        		}
		        	}
        		}
        	}
        }
        return items;
    }
	
	@Override
    public String getCustomName(ItemStack stack) {
		PlateType plate = ItemPlateType.get(stack);
		if (plate == PlateType.BOILER) {
			EntityRollingStockDefinition def = ItemDefinition.get(stack);
			if (def != null) {
				return TextColor.RESET.wrap(plate.toString() + " " + def.name());
			}
		} else {
			return TextColor.RESET.wrap(plate.toString());
		}
		return null;
	}
	
	@Override
	public void addInformation(ItemStack stack, List<String> tooltip)
    {
        tooltip.add(GuiText.GAUGE_TOOLTIP.toString(ItemGauge.get(stack)));
    }
}

