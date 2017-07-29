package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.Freight;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class FreightContainerGui extends GuiContainer {
	
    private static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
	private int inventoryRows;

    public FreightContainerGui(Freight tileEntity, FreightContainer container) {
        super(container);
        this.inventoryRows = container.numRows;
        this.ySize = 114 + this.inventoryRows * 18;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.inventoryRows * 18 + 17);
        this.drawTexturedModalRect(i, j + this.inventoryRows * 18 + 17, 0, 126, this.xSize, 96);
    }
}