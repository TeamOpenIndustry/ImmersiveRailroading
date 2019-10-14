package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.Config.ConfigBalance;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.nbt.ItemDefinition;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.items.nbt.ItemPlateType;
import cam72cam.immersiverailroading.library.CraftingType;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.PlateType;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.gui.Button;
import cam72cam.mod.gui.IScreen;
import cam72cam.mod.gui.IScreenBuilder;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.util.CollectionUtil;
import cam72cam.mod.util.Hand;

import java.util.List;

public class PlateRollerGUI implements IScreen {
	private Button gaugeButton;
	private Gauge gauge;
	
	private Button plateButton;
	private PlateType plate;

	private Button pickerButton;

	private TileMultiblock tile;
	private ItemStack currentItem;
	
	public PlateRollerGUI(TileMultiblock te) {
		this.tile = te;
		currentItem = te.getCraftItem();
		if (currentItem == null || currentItem.isEmpty()) {
			currentItem = new ItemStack(IRItems.ITEM_PLATE, 1);
		}
		
		gauge = ItemGauge.get(currentItem);
		plate = ItemPlateType.get(currentItem);
	}
	
	private void updatePickerButton() {
		EntityRollingStockDefinition def = ItemDefinition.get(currentItem.copy());
		if (def != null) {
			pickerButton.setText(GuiText.SELECTOR_PLATE_BOILER.toString(def.name()));
		}
	}

	@Override
	public void init(IScreenBuilder screen) {
		gaugeButton = new Button(screen, 0 - 100, -24 + 0 * 30, GuiText.SELECTOR_GAUGE.toString(gauge)) {
			@Override
			public void onClick(Hand hand) {
				if(!currentItem.isEmpty()) {
					EntityRollingStockDefinition def = ItemDefinition.get(currentItem);
					if (def != null && plate == PlateType.BOILER && ConfigBalance.DesignGaugeLock) {
						List<Gauge> validGauges = CollectionUtil.listOf(Gauge.from(def.recommended_gauge.value()));
						gauge = gauge.next(validGauges);
					} else {
						gauge = gauge.next();
					}
				}
				gaugeButton.setText(GuiText.SELECTOR_GAUGE.toString(gauge));
				sendPacket();
			}
		};

		plateButton = new Button(screen, 0 - 100, -24 + 1 * 30, GuiText.SELECTOR_PLATE_TYPE.toString(plate)) {
			@Override
			public void onClick(Hand hand) {
				plate = PlateType.values()[((plate.ordinal() + 1) % (PlateType.values().length))];
				plateButton.setText(GuiText.SELECTOR_PLATE_TYPE.toString(plate));
				pickerButton.setVisible(plate == PlateType.BOILER);
				sendPacket();
			}
		};

		pickerButton = new Button(screen, 0 - 100, -24 + 2 * 30, GuiText.SELECTOR_PLATE_BOILER.toString("")) {
			@Override
			public void onClick(Hand hand) {
				CraftPicker.showCraftPicker(screen, null, CraftingType.PLATE_BOILER, (ItemStack item) -> {
					if (item != null) {
						String defID = ItemDefinition.getID(item);
						ItemDefinition.setID(currentItem, defID);
						EntityRollingStockDefinition def = ItemDefinition.get(currentItem);
						if (def != null && !gauge.isModel() && gauge.value() != def.recommended_gauge.value()) {
							gauge = def.recommended_gauge;
							gaugeButton.setText(GuiText.SELECTOR_GAUGE.toString(gauge));
						}
						updatePickerButton();
						sendPacket();
					}
				});
			}
		};
		pickerButton.setVisible(plate == PlateType.BOILER);
		updatePickerButton();
	}

	@Override
	public void onEnterKey(IScreenBuilder builder) {
		sendPacket();
		builder.close();
	}

	@Override
	public void onClose() {
		sendPacket();
	}

	@Override
	public void draw(IScreenBuilder builder) {

	}

	private void sendPacket() {
		ItemGauge.set(currentItem, gauge);
		ItemPlateType.set(currentItem, plate);
    	switch (plate) {
		case BOILER:
			currentItem.setCount(1);
			break;
		case LARGE:
			currentItem.setCount(1);
			break;
		case MEDIUM:
			currentItem.setCount(4);
			break;
		case SMALL:
			currentItem.setCount(8);
			break;
		default:
			break;
    	}
		currentItem.setCount(Math.max(1, (int) Math.floor(currentItem.getCount()/gauge.scale())));
		tile.setCraftItem(currentItem);
    }
}
