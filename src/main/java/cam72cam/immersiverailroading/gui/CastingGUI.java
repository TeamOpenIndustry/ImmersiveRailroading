package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.Config.ConfigBalance;
import cam72cam.immersiverailroading.items.nbt.ItemDefinition;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.items.nbt.ItemRawCast;
import cam72cam.immersiverailroading.library.CraftingMachineMode;
import cam72cam.immersiverailroading.library.CraftingType;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.multiblock.CastingMultiblock;
import cam72cam.immersiverailroading.multiblock.CastingMultiblock.CastingInstance;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.util.ItemCastingCost;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.gui.Button;
import cam72cam.mod.gui.IScreen;
import cam72cam.mod.gui.IScreenBuilder;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.Hand;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class CastingGUI implements IScreen {
    public static final Identifier CASTING_GUI_TEXTURE = new Identifier("immersiverailroading:gui/casting_gui.png");
    
	private Button gaugeButton;
	private Gauge gauge;

	private Button pickerButton;

	private Button singleCastButton;
	private Button repeatCastButton;
	
	private TileMultiblock tile;
	private ItemStack currentItem;
	
	public CastingGUI(TileMultiblock te) {
		this.tile = te;
		currentItem = ((CastingInstance) te.getMultiblock()).getCraftItem();
		
		gauge = ItemGauge.get(currentItem);
	}
	
	private void updatePickerButton() {
		if (currentItem.isEmpty()) {
			pickerButton.setText(GuiText.SELECTOR_TYPE.toString(""));
		} else {
			pickerButton.setText(currentItem.getDisplayName());
		}
	}

	@Override
	public void init(IScreenBuilder screen) {
		pickerButton = screen.addButton(new Button(screen, -100, -20 - 10, GuiText.SELECTOR_TYPE.toString("")) {
			@Override
			public void onClick(Hand hand) {
				CraftPicker.showCraftPicker(screen, currentItem, CraftingType.CASTING, (ItemStack item) -> {
					if (item != null) {
						currentItem = item;
						EntityRollingStockDefinition def = ItemDefinition.get(currentItem);
						if (def != null && !gauge.isModel() && gauge.value() != def.recommended_gauge.value()) {
							gauge = def.recommended_gauge;
							gaugeButton.setText(GuiText.SELECTOR_GAUGE.toString(gauge));
						}
						updatePickerButton();
						sendItemPacket();
					}
				});
			}
		});
		updatePickerButton();

		gaugeButton = screen.addButton(new Button(screen, 0, -10, 100, 20, GuiText.SELECTOR_GAUGE.toString(gauge)) {
			@Override
			public void onClick(Hand hand) {
				if(!currentItem.isEmpty()) {
					EntityRollingStockDefinition def = ItemDefinition.get(currentItem);
					if (def != null && ConfigBalance.DesignGaugeLock) {
						List<Gauge> validGauges = new ArrayList<Gauge>();
						validGauges.add(Gauge.from(def.recommended_gauge.value()));
						gauge = gauge.next(validGauges);
					} else {
						gauge = gauge.next();
					}
				}
				gaugeButton.setText(GuiText.SELECTOR_GAUGE.toString(gauge));
				sendItemPacket();
			}
		});
		singleCastButton = screen.addButton(new Button(screen, 0, +20 - 10, 100, 20, GuiText.SELECTOR_CAST_SINGLE.toString()) {
			@Override
			public void onClick(Hand hand) {
				if (tile.getCraftMode() != CraftingMachineMode.SINGLE) {
					tile.setCraftMode(CraftingMachineMode.SINGLE);
				} else {
					tile.setCraftMode(CraftingMachineMode.STOPPED);
				}
			}
			@Override
			public void onUpdate() {
				singleCastButton.setTextColor(tile.getCraftMode() == CraftingMachineMode.SINGLE ? 0xcc4334 : 0);
			}
		});
		repeatCastButton = screen.addButton(new Button(screen, 0, +40 - 10, 100, 20, GuiText.SELECTOR_CAST_REPEAT.toString()) {
			@Override
			public void onClick(Hand hand) {
				if (tile.getCraftMode() != CraftingMachineMode.REPEAT) {
					tile.setCraftMode(CraftingMachineMode.REPEAT);
				} else {
					tile.setCraftMode(CraftingMachineMode.STOPPED);
				}
			}
			@Override
			public void onUpdate() {
				repeatCastButton.setTextColor(tile.getCraftMode() == CraftingMachineMode.REPEAT ? 0xcc4334 : 0);
			}
		});
	}

	@Override
	public void onEnterKey(IScreenBuilder b) {
	}

	@Override
	public void onClose() {

	}

	@Override
	public void draw(IScreenBuilder builder) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		double fluidPercent = ((CastingInstance) tile.getMultiblock()).getSteelLevel();
		int progress = this.tile.getCraftProgress();
		float cost = ItemCastingCost.getCastCost(currentItem);
		if(cost == ItemCastingCost.BAD_CAST_COST) {
			cost = 0;
		}

		builder.drawImage(CASTING_GUI_TEXTURE, -100, 0, 200, 100);

		builder.drawTank(- 94.5, 3, 56.7, 60, Fluid.LAVA, (float) fluidPercent, false, 0x99fb7e15);
		builder.drawTank(- 28.5, 67, 125.2, 30, Fluid.LAVA, progress/cost, false, 0x998c1919);
		
		String fillStr = String.format("%s/%s", (int)(fluidPercent * CastingMultiblock.max_volume), (int)CastingMultiblock.max_volume);
		String castStr = String.format("%s/%s", progress, (int)cost);
		builder.drawCenteredString(fillStr, - 94 + 27, 3 + 25, 14737632);
		builder.drawCenteredString(castStr, - 28 + 60, 67 + 10, 14737632);
	}
	
	private void sendItemPacket() {
		ItemGauge.set(currentItem, gauge);
		currentItem.setCount(1);
		ItemRawCast.set(currentItem, true);
		tile.setCraftItem(currentItem);
    }
}