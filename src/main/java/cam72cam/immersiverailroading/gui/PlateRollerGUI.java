package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.Config.ConfigBalance;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.ItemPlate;
import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.library.CraftingType;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.PlateType;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.screen.Button;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static cam72cam.immersiverailroading.gui.ClickListHelper.next;

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

		ItemPlate.Data data = new ItemPlate.Data(currentItem);
		gauge = data.gauge;
		plate = data.type;
	}
	
	private void updatePickerButton() {
		EntityRollingStockDefinition def = new ItemPlate.Data(currentItem.copy()).def;
		if (def != null) {
			pickerButton.setText(GuiText.SELECTOR_PLATE_BOILER.toString(def.name()));
		}
	}

	@Override
	public void init(IScreenBuilder screen) {
		gaugeButton = new Button(screen, 0 - 100, -24 + 0 * 30, GuiText.SELECTOR_GAUGE.toString(gauge)) {
			@Override
			public void onClick(Player.Hand hand) {
				if(!currentItem.isEmpty()) {
					EntityRollingStockDefinition def = new ItemPlate.Data(currentItem).def;
					if (def != null && plate == PlateType.BOILER && ConfigBalance.DesignGaugeLock) {
						List<Gauge> validGauges = Collections.singletonList(Gauge.from(def.recommended_gauge.value()));
						gauge = next(validGauges, gauge, hand);
					} else {
						gauge = next(Gauge.values(), gauge, hand);
					}
				}
				gaugeButton.setText(GuiText.SELECTOR_GAUGE.toString(gauge));
				sendPacket();
			}
		};

		plateButton = new Button(screen, 0 - 100, -24 + 1 * 30, GuiText.SELECTOR_PLATE_TYPE.toString(plate)) {
			@Override
			public void onClick(Player.Hand hand) {
				plate = next(plate, hand);
				plateButton.setText(GuiText.SELECTOR_PLATE_TYPE.toString(plate));
				pickerButton.setVisible(plate == PlateType.BOILER);
				sendPacket();
			}
		};

		pickerButton = new Button(screen, 0 - 100, -24 + 2 * 30, GuiText.SELECTOR_PLATE_BOILER.toString("")) {
			@Override
			public void onClick(Player.Hand hand) {
				CraftPicker.showCraftPicker(screen, null, CraftingType.PLATE_BOILER, (ItemStack item) -> {
					if (item != null) {
						ItemRollingStock.Data rs = new ItemRollingStock.Data(item);
						ItemPlate.Data data = new ItemPlate.Data(currentItem);
						data.def = rs.def;
						EntityRollingStockDefinition def = rs.def;
						if (def != null && !gauge.isModel() && gauge.value() != def.recommended_gauge.value()) {
							gauge = def.recommended_gauge;
							gaugeButton.setText(GuiText.SELECTOR_GAUGE.toString(gauge));
						}
						data.write();
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
		ItemPlate.Data data = new ItemPlate.Data(currentItem);
		data.gauge = gauge;
		data.type = plate;
		data.write();
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
