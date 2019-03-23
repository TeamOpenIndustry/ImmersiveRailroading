package cam72cam.immersiverailroading.gui;

import java.io.IOException;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.CarArtillery;
import cam72cam.immersiverailroading.entity.CarFreight;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.library.TrackPositionType;
import cam72cam.immersiverailroading.net.CarArtilleryUpdatePacket;
import cam72cam.immersiverailroading.net.ItemRailUpdatePacket;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import net.minecraftforge.fml.client.config.GuiSlider;
import scala.Int;

public class ArtilleryGUI extends ContainerGuiBase {
	
	private GuiButton aimButton;
	private GuiButton fireButton;
	private GuiTextField targetXInput;
	private GuiTextField targetZInput;
	
    private static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
	private int inventoryRows;
	private int horizSlots;
	private int numSlots;

	CarArtillery stock;
	private BlockPos targetPos = BlockPos.ORIGIN;
	
	private final Predicate<String> integerFilter = new Predicate<String>() {
		@Override
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
			return val > Int.MinValue() && val < Int.MaxValue();
		}
	};
	
    public ArtilleryGUI(CarArtillery stock, FreightContainer container) {
        super(container);
        this.stock = stock;
        targetPos = stock.getAimPoint();
        this.inventoryRows = container.numRows;
        this.horizSlots = stock.getInventoryWidth();
        this.numSlots = container.numSlots;
        this.xSize = paddingRight + horizSlots * slotSize + paddingLeft;
        this.ySize = 114 + this.inventoryRows * slotSize;
    }

	@Override
	public void initGui() {
		super.initGui();
		int buttonID = -1;

		fireButton = new GuiButton(++buttonID, this.width / 2 - 100, this.height / 8 - 24 + (buttonID/2) * 22, 100, 20,"Fire");
		this.buttonList.add(fireButton);
		
		aimButton = new GuiButton(++buttonID, this.width / 2 + 50, this.height / 8 - 24 + (buttonID/2) * 22, 100, 20, "Aim");
		this.buttonList.add(aimButton);
		
		this.targetXInput = new GuiTextField(++buttonID, this.fontRenderer, this.width / 2 - 100, this.height / 8 - 24 + (buttonID/2) * 22, 100, 20);
		this.targetXInput.setText("" + targetPos.getX());
		this.targetXInput.setMaxStringLength(16);
		this.targetXInput.setValidator(this.integerFilter);
		this.targetXInput.setFocused(false);
		
		this.targetZInput = new GuiTextField(++buttonID, this.fontRenderer, this.width / 2 + 50, this.height / 8 - 24 + (buttonID/2) * 22, 100, 20);
		this.targetZInput.setText("" + targetPos.getZ());
		this.targetZInput.setMaxStringLength(16);
		this.targetZInput.setValidator(this.integerFilter);
		this.targetZInput.setFocused(false);
	}
    
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		this.targetXInput.drawTextBox();
		this.targetZInput.drawTextBox();
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void updateScreen()
    {
		super.updateScreen();
		this.targetXInput.updateCursorCounter();
        this.targetZInput.updateCursorCounter();
    }
	
	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button == fireButton) {
			ImmersiveRailroading.net.sendToServer(new CarArtilleryUpdatePacket(stock));
		}
		else if (button == aimButton) {
			ImmersiveRailroading.net.sendToServer(new CarArtilleryUpdatePacket(stock, new BlockPos(Integer.parseInt(targetXInput.getText()), 63, Integer.parseInt(targetZInput.getText()))));
		}
	}
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.targetXInput.isFocused()) this.targetXInput.textboxKeyTyped(typedChar, keyCode);
        if (this.targetZInput.isFocused()) this.targetZInput.textboxKeyTyped(typedChar, keyCode);
        // Enter or ESC
        if (keyCode == 1 || keyCode == 28 || keyCode == 156) {
			this.mc.displayGuiScreen(null);
			if (this.mc.currentScreen == null)
				this.mc.setIngameFocus();
        }
        else super.keyTyped(typedChar, keyCode);
	}
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.targetXInput.mouseClicked(mouseX, mouseY, mouseButton);
        this.targetZInput.mouseClicked(mouseX, mouseY, mouseButton);
    }
	
    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
    	
    	int currY = j;
        
    	currY = drawTopBar(i, currY, horizSlots);
    	currY = drawSlotBlock(i, currY, horizSlots, inventoryRows, numSlots);
    	currY = drawPlayerInventoryConnector(i, currY, width, horizSlots);
    	currY = drawPlayerInventory((width - playerXSize) / 2, currY);
    }
}