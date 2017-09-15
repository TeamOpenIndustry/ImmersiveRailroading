package cam72cam.immersiverailroading.gui;

import java.io.IOException;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemRail;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.library.TrackPositionType;
import cam72cam.immersiverailroading.net.ItemRailUpdatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.client.config.GuiSlider;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;

public class TrackGui extends GuiScreen {
	private GuiButton typeButton;
	private GuiTextField lengthInput;
	private GuiSlider quartersSlider;

	private int slot;
	private int length;
	private int quarters;
	private TrackItems type;
	private TrackPositionType posType;
	private GuiButton posTypeButton;

	private final Predicate<String> integerFilter = new Predicate<String>() {
		public boolean apply(@Nullable String inputString) {
			if (StringUtils.isNullOrEmpty(inputString)) {
				return true;
			}
			int val;
			try {
				val = Integer.parseInt(inputString);
			} catch (NumberFormatException e) {
				return false;
			}
			return val > 0 && val <= 100;
		}
	};

	public TrackGui() {
		slot = Minecraft.getMinecraft().player.inventory.currentItem;
		ItemStack stack = Minecraft.getMinecraft().player.getHeldItemMainhand();
		length = ItemRail.getLength(stack);
		quarters = ItemRail.getQuarters(stack);
		type = TrackItems.fromMeta(stack.getMetadata());
		posType = ItemRail.getPosType(stack);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
        this.lengthInput.drawTextBox();
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void updateScreen()
    {
        this.lengthInput.updateCursorCounter();
    }

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	public void initGui() {
		int buttonID = 0;

		typeButton = new GuiButton(buttonID++, this.width / 2 - 100, this.height / 4 - 24 + buttonID * 30, "Type: " + type.getName());
		this.buttonList.add(typeButton);

		this.lengthInput = new GuiTextField(buttonID++, this.fontRenderer, this.width / 2 - 100, this.height / 4 - 24 + buttonID * 30, 200, 20);
		this.lengthInput.setText("" + length);
		this.lengthInput.setMaxStringLength(3);
		this.lengthInput.setValidator(this.integerFilter);
		this.lengthInput.setFocused(true);

		this.quartersSlider = new GuiSlider(buttonID++, this.width / 2 - 75, this.height / 4 - 24 + buttonID * 30, "Quarters: ", 1, 4, quarters,
				null);
		quartersSlider.showDecimal = false;

		// this.buttonList.add(lengthSlider);
		this.buttonList.add(quartersSlider);

		quartersSlider.visible = type == TrackItems.SWITCH || type == TrackItems.TURN;

		posTypeButton = new GuiButton(buttonID++, this.width / 2 - 100, this.height / 4 - 24 + buttonID * 30, "Position: " + posType.name());
		this.buttonList.add(posTypeButton);
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button == typeButton) {
			type = TrackItems.fromMeta((type.ordinal() + 1) % (TrackItems.values().length));
			typeButton.displayString = "Type: " + type.getName();
			quartersSlider.visible = type == TrackItems.SWITCH || type == TrackItems.TURN;
		}
		if (button == posTypeButton) {
			posType = TrackPositionType.values()[((posType.ordinal() + 1) % (TrackPositionType.values().length))];
			posTypeButton.displayString = "Position: " + posType.name();
		}
	}
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
        this.lengthInput.textboxKeyTyped(typedChar, keyCode);
        // Enter or ESC
        if (keyCode == 1 || keyCode == 28 || keyCode == 156) {
        	if (!this.lengthInput.getText().isEmpty()) {
				ImmersiveRailroading.net.sendToServer(
						new ItemRailUpdatePacket(slot, Integer.parseInt(lengthInput.getText()), quartersSlider.getValueInt(), type, posType));
        	}
			this.mc.displayGuiScreen(null);
			if (this.mc.currentScreen == null)
				this.mc.setIngameFocus();
        }
	}
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.lengthInput.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
