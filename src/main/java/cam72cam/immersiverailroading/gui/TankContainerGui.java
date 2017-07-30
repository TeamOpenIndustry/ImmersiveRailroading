package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.CarTank;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;

public class TankContainerGui extends ContainerGuiBase {
	
    private static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
	private int inventoryRows;
	private int horizSlots;
	private CarTank stock;

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
    	int currY = j;
    	
    	TankContainerGui.drawRect(i, j-100, i+300, j, 0xFF000000);
    	if (stock.getClientLiquidAmount() > 0 && stock.getClientLiquid() != null) {
    		Fluid fluid = stock.getClientLiquid();
    		drawFluid(fluid, i, j-100, 300, 100, 2);
    	}
    	GlStateManager.color(1, 1, 1, 1);

        this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
        
        currY = drawTopBar(i, currY, horizSlots);
    	currY = drawSlotBlock(i, currY, horizSlots, inventoryRows);
    	currY = drawPlayerInventoryConnector(i, currY, width, horizSlots);
    	currY = drawPlayerInventory((width - playerXSize) / 2, currY);
    }
}