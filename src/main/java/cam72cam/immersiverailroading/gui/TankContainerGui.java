package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.CarTank;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;

public class TankContainerGui extends GuiContainer {
	
    private static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
	private int inventoryRows;
	private int horizSlots;
	private CarTank stock;
    
    public static final int slotSize = 18;
    public static final int topOffset = 17;
    public static final int bottomOffset = 7;
    public static final int textureHeight = 222;
    public static final int paddingRight = 7;
    public static final int paddingLeft = 7;

    public TankContainerGui(CarTank stock, TankContainer container) {
        super(container);
        this.stock = stock;
        this.inventoryRows = container.numRows;
        this.horizSlots = 2;
        this.xSize = paddingRight + horizSlots * slotSize + paddingLeft;
        this.ySize = 114 + this.inventoryRows * slotSize;
    }
    
    private void drawFluid(Fluid fluid, int x, int y, int width, int height, int scale) {
    	this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		TextureAtlasSprite sprite = mc.getTextureMapBlocks().getAtlasSprite(fluid.getStill().toString());
		if(sprite != null)
		{
			int col = fluid.getColor();
			GlStateManager.color((col>>16&255)/255.0f,(col>>8&255)/255.0f,(col&255)/255.0f, 1);
			int iW = sprite.getIconWidth()*scale;
			int iH = sprite.getIconHeight()*scale;
			
	        float minU = sprite.getMinU();
	        float minV = sprite.getMinV();
	        
	        Tessellator tessellator = Tessellator.getInstance();
	        BufferBuilder buffer = tessellator.getBuffer();
	        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);	        
	        for (int offY = 0; offY < height; offY += iH) {
	        	int curHeight = Math.min(iH, height - offY);
	        	float maxVScaled = sprite.getInterpolatedV(16.0 * curHeight / iH);
		        for (int offX = 0; offX < width; offX += iW) {
		        	int curWidth = Math.min(iW, width - offX);
		        	float maxUScaled = sprite.getInterpolatedU(16.0 * curWidth / iW);  
			        buffer.pos(x+offX, y+offY, this.zLevel).tex(minU, maxVScaled).endVertex();
			        buffer.pos(x+offX, y+offY+curHeight, this.zLevel).tex(minU, minV).endVertex();
			        buffer.pos(x+offX+curWidth, y+offY+curHeight, this.zLevel).tex(maxUScaled, minV).endVertex();
			        buffer.pos(x+offX+curWidth, y+offY, this.zLevel).tex(maxUScaled, maxVScaled).endVertex();
		        }
	        }
	        tessellator.draw();
		}
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
    	int playerXSize = paddingRight + 9 * slotSize + paddingLeft;
    	
    	TankContainerGui.drawRect(i, j-100, i+300, j, 0xFF000000);
    	if (stock.getClientLiquidAmount() > 0 && stock.getClientLiquid() != null) {
    		Fluid fluid = stock.getClientLiquid();
    		drawFluid(fluid, i, j-100, 300, 100, 2);
    	}
    	GlStateManager.color(1, 1, 1, 1);

        this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
        
        // Top Left Corner
        this.drawTexturedModalRect(i, j, 0, 0, paddingLeft, topOffset);
        // Top Bar
        for (int k = 1; k <= horizSlots; k++) {
        	this.drawTexturedModalRect(i + paddingLeft + (k-1) * slotSize, j, paddingLeft, 0, slotSize, topOffset);
        }
        // Top Right Corner
    	this.drawTexturedModalRect(i + paddingLeft + horizSlots * slotSize, j, paddingLeft + 9 * slotSize, 0, paddingRight, topOffset);
    	
    	for (int l = 0; l < inventoryRows; l++) {
	    	// Left Side
	        this.drawTexturedModalRect(i, j + topOffset + l * slotSize, 0, topOffset, paddingLeft, slotSize);
	        // Middle Slots
	        for (int k = 1; k <= horizSlots; k++) {
	        	this.drawTexturedModalRect(i + paddingLeft + (k-1) * slotSize, j + topOffset + l * slotSize, paddingLeft, topOffset, slotSize, slotSize);
	        }
	        // Right Side
	    	this.drawTexturedModalRect(i + paddingLeft + horizSlots * slotSize, j + topOffset + l * slotSize, paddingLeft + 9 * slotSize, topOffset, paddingRight, slotSize);
    	}
    	if (horizSlots > 9) {
	    	// Left Bottom
	        this.drawTexturedModalRect(i, j + topOffset + inventoryRows * slotSize, 0, textureHeight - bottomOffset, paddingLeft, bottomOffset);
	        // Middle Bottom
	        for (int k = 1; k <= horizSlots; k++) {
	        	this.drawTexturedModalRect(i + paddingLeft + (k-1) * slotSize, j + topOffset + inventoryRows * slotSize, paddingLeft, textureHeight - bottomOffset, slotSize, bottomOffset);
	        }
	        // Right Bottom
	    	this.drawTexturedModalRect(i + paddingLeft + horizSlots * slotSize, j + topOffset + inventoryRows * slotSize, paddingLeft + 9 * slotSize, textureHeight - bottomOffset, paddingRight, bottomOffset);
    	} else if (horizSlots < 9){
            this.drawTexturedModalRect((this.width - playerXSize) / 2, j + topOffset + inventoryRows * slotSize, 0, 0, playerXSize, bottomOffset);
    	} else {
    		this.drawTexturedModalRect((this.width - playerXSize) / 2, j + topOffset + this.inventoryRows * slotSize, 0, 4, playerXSize, 4);
    	}
        
        this.drawTexturedModalRect((this.width - playerXSize) / 2, j + this.inventoryRows * 18 + 17+4, 0, 126+4, playerXSize, 96);
    }
}