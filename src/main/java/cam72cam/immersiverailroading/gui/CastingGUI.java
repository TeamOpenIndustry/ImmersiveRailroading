package cam72cam.immersiverailroading.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.Config;
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
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidRegistry;

public class CastingGUI extends GuiScreen {
    public static final ResourceLocation CASTING_GUI_TEXTURE = new ResourceLocation("immersiverailroading:gui/casting_gui.png");
    
	private GuiButton gaugeButton;
	private Gauge gauge;

	private GuiButton pickerButton;
	private CraftPicker picker;
	
	private GuiButton singleCastButton;
	private GuiButton repeatCastButton;
	
	private TileMultiblock tile;
	private ItemStack currentItem;
	
	public CastingGUI(TileMultiblock te) {
		this.tile = te;
		currentItem = ((CastingInstance) te.getMultiblock()).getCraftItem();
		
		gauge = ItemGauge.get(currentItem);
		picker = new CraftPicker(currentItem, CraftingType.CASTING, (ItemStack item) -> {
        	this.mc.displayGuiScreen(this);
        	
        	if (item != null) {
        		currentItem = item;
				EntityRollingStockDefinition def = ItemDefinition.get(currentItem);
				if (def != null && !gauge.isModel() && gauge.value() != def.recommended_gauge.value()) {
					gauge = def.recommended_gauge;
					gaugeButton.displayString = GuiText.SELECTOR_GAUGE.toString(gauge);
				}
        		updatePickerButton();
    			sendItemPacket();
        	}
        });
	}
	
	private void updatePickerButton() {
		if (currentItem.isEmpty()) {
			pickerButton.displayString = GuiText.SELECTOR_TYPE.toString("");
		} else {
			pickerButton.displayString = currentItem.getDisplayName();
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
	
	@Override
	public void initGui() {
		int buttonID = 0;
		
		pickerButton = new GuiButton(buttonID++, this.width / 2 - 100, this.height / 4 - 20-10, GuiText.SELECTOR_TYPE.toString(""));
		updatePickerButton();
		this.buttonList.add(pickerButton);

		gaugeButton = new GuiButton(buttonID++, this.width / 2, this.height / 4-10, 100, 20, GuiText.SELECTOR_GAUGE.toString(gauge));
		this.buttonList.add(gaugeButton);
		
		singleCastButton = new GuiButton(buttonID++, this.width / 2, this.height / 4 + 20-10, 100, 20, GuiText.SELECTOR_CAST_SINGLE.toString());
		this.buttonList.add(singleCastButton);
		
		repeatCastButton = new GuiButton(buttonID++, this.width / 2, this.height / 4 + 40-10, 100, 20, GuiText.SELECTOR_CAST_REPEAT.toString());
		this.buttonList.add(repeatCastButton);
		
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		singleCastButton.packedFGColour = 0;
		repeatCastButton.packedFGColour = 0;
		switch (tile.getCraftMode()) {
		case SINGLE:
			singleCastButton.packedFGColour = 0xcc4334;
			break;
		case REPEAT:
			repeatCastButton.packedFGColour = 0xcc4334;
			break;
		case STOPPED:
			// no highlighting
			break;
		}
		
		super.drawScreen(mouseX, mouseY, partialTicks);
		
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		double fluidPercent = ((CastingInstance) tile.getMultiblock()).getSteelLevel();
		int progress = this.tile.getCraftProgress();
		float cost = ItemCastingCost.getCastCost(currentItem);
		if(cost == ItemCastingCost.BAD_CAST_COST) {
			cost = 0;
		}
    	
    	this.mc.getTextureManager().bindTexture(CASTING_GUI_TEXTURE);

        GUIHelpers.texturedRect(this.width / 2 - 100, this.height / 4, 200, 100);

		GUIHelpers.drawTankBlock(this.width / 2 - 94.5, this.height / 4 + 3, 56.7, 60, FluidRegistry.LAVA, (float) fluidPercent, false, 0x99fb7e15);
		GUIHelpers.drawTankBlock(this.width / 2 - 28.5, this.height / 4 + 67, 125.2, 30, FluidRegistry.LAVA, progress/cost, false, 0x998c1919);
		
		String fillStr = String.format("%s/%s", (int)(fluidPercent * CastingMultiblock.max_volume), (int)CastingMultiblock.max_volume);
		String castStr = String.format("%s/%s", progress, (int)cost);
		this.drawCenteredString(this.fontRenderer, fillStr, this.width / 2 - 94 + 27, this.height / 4 + 3 + 25, 14737632);
		this.drawCenteredString(this.fontRenderer, castStr, this.width / 2 - 28 + 60, this.height / 4 + 67 + 10, 14737632);
	}
	
	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button == gaugeButton) {
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
			gaugeButton.displayString = GuiText.SELECTOR_GAUGE.toString(gauge);
			sendItemPacket();
		}
		if (button == pickerButton) {
			this.mc.displayGuiScreen(picker);
		}
		if (button == singleCastButton) {
			if (tile.getCraftMode() != CraftingMachineMode.SINGLE) {
				tile.setCraftMode(CraftingMachineMode.SINGLE);
			} else {
				tile.setCraftMode(CraftingMachineMode.STOPPED);
			}
		}
		if (button == repeatCastButton) {
			if (tile.getCraftMode() != CraftingMachineMode.REPEAT) {
				tile.setCraftMode(CraftingMachineMode.REPEAT);
			} else {
				tile.setCraftMode(CraftingMachineMode.STOPPED);
			}
		}
	}
	
	private void sendItemPacket() {
		ItemGauge.set(currentItem, gauge);
		currentItem.setCount(1);
		ItemRawCast.set(currentItem, true);
		tile.setCraftItem(currentItem);
    }
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Enter or ESC
        if (keyCode == 1 || keyCode == 28 || keyCode == 156) {
			this.mc.displayGuiScreen(null);
			if (this.mc.currentScreen == null)
				this.mc.setIngameFocus();
        }
	}
}