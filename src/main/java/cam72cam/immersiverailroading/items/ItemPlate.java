package cam72cam.immersiverailroading.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.library.PlateType;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.text.TextColor;

public class ItemPlate extends CustomItem {
	public ItemPlate() {
		super(ImmersiveRailroading.MODID, "item_plate");
	}

	@Override
	public int getStackSize() {
		return 64;
	}

	@Override
	public List<CreativeTab> getCreativeTabs() {
		return Collections.singletonList(ItemTabs.MAIN_TAB);
	}


	@Override
	public List<ItemStack> getItemVariants(CreativeTab tab) {
		List<ItemStack> items = new ArrayList<>();
        if (tab == null || tab.equals(ItemTabs.MAIN_TAB))
        {
        	for (PlateType plate : PlateType.values()) {
        		ItemStack stack = new ItemStack(this, 1);
        		if (plate != PlateType.BOILER) {
					Data data = new Data(stack);
					data.type = plate;
					data.gauge = Gauge.from(Gauge.STANDARD);
					data.write();
	                items.add(stack);
        		} else {
		        	for (EntityRollingStockDefinition def : DefinitionManager.getDefinitions()) {
		        		if (def.getItemComponents().contains(ItemComponentType.BOILER_SEGMENT) ) {
			        		stack = stack.copy();
							Data data = new Data(stack);
							data.type = plate;
							data.gauge = Gauge.from(Gauge.STANDARD);
							data.def = def;
							data.write();
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
		Data data = new Data(stack);
		if (data.type == PlateType.BOILER) {
			if (data.def != null) {
				return TextColor.RESET.wrap(data.type.toString() + " " + data.def.name());
			}
		} else {
			return TextColor.RESET.wrap(data.type.toString());
		}
		return null;
	}
	
	@Override
	public List<String> getTooltip(ItemStack stack)
    {
    	return Collections.singletonList(GuiText.GAUGE_TOOLTIP.toString(new Data(stack).gauge));
    }

    public static class Data extends ItemDataSerializer {
		@TagField("plate")
		public PlateType type;

		@TagField("defID")
		public EntityRollingStockDefinition def;

		@TagField("gauge")
		public Gauge gauge;

		public Data(ItemStack stack) {
			super(stack);

			if (gauge == null) {
				gauge = def != null ? def.recommended_gauge : Gauge.from(Gauge.STANDARD);
			}
			if (type == null) {
				type = PlateType.SMALL;
			}
		}
	}
}

