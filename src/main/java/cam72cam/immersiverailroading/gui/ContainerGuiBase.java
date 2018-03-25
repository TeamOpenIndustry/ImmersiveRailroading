package cam72cam.immersiverailroading.gui;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;

public abstract class ContainerGuiBase extends GuiContainer {
    public static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");

	public ContainerGuiBase(Container inventorySlotsIn) {
		super(inventorySlotsIn);
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
	}

	public static final int slotSize = 18;
    public static final int topOffset = 17;
    public static final int bottomOffset = 7;
    public static final int textureHeight = 222;
    public static final int paddingRight = 7;
    public static final int paddingLeft = 7;
    public static final int stdUiHorizSlots = 9;
    public static final int playerXSize = paddingRight + stdUiHorizSlots * slotSize + paddingLeft;
	private static final int midBarOffset = 4;
	private static final int midBarHeight = 4;
    
    public int drawTopBar(int x, int y, int slots) {
    	this.drawTexturedModalRect(x, y, 0, 0, paddingLeft, topOffset);
        // Top Bar
        for (int k = 1; k <= slots; k++) {
        	this.drawTexturedModalRect(x + paddingLeft + (k-1) * slotSize, y, paddingLeft, 0, slotSize, topOffset);
        }
        // Top Right Corner
    	this.drawTexturedModalRect(x + paddingLeft + slots * slotSize, y, paddingLeft + stdUiHorizSlots * slotSize, 0, paddingRight, topOffset);
    	
    	return y + topOffset;
    }
    
    public void drawSlot(int x, int y) {
    	this.drawTexturedModalRect(x, y, paddingLeft, topOffset, slotSize, slotSize);
    }
    
    public int drawSlotRow(int x, int y, int slots, int numSlots) {
    	// Left Side
        this.drawTexturedModalRect(x, y, 0, topOffset, paddingLeft, slotSize);
        // Middle Slots
        for (int k = 1; k <= slots; k++) {
        	if (k <= numSlots) {
        		drawSlot(x + paddingLeft + (k-1) * slotSize, y);
        	} else {
        		Gui.drawRect(x + paddingLeft + (k-1) * slotSize, y, x + paddingLeft + (k-1) * slotSize + slotSize, y + slotSize, 0xFF444444);
        	}
        }
		GL11.glColor4f(1, 1, 1, 1);
        // Right Side
    	this.drawTexturedModalRect(x + paddingLeft + slots * slotSize, y, paddingLeft + stdUiHorizSlots * slotSize, topOffset, paddingRight, slotSize);
    	return y + slotSize;
    }

	public int drawSlotBlock(int x, int y, int slotX, int slotY, int numSlots) {
		for (int i = 0; i < slotY; i++) {
			y = drawSlotRow(x, y, slotX, numSlots);
			numSlots -= slotX;
		}
		return y;
	}

	public int drawBottomBar(int x, int y, int slots) { 
    	// Left Bottom
        this.drawTexturedModalRect(x, y, 0, textureHeight - bottomOffset, paddingLeft, bottomOffset);
        // Middle Bottom
        for (int k = 1; k <= slots; k++) {
        	this.drawTexturedModalRect(x + paddingLeft + (k-1) * slotSize, y, paddingLeft, textureHeight - bottomOffset, slotSize, bottomOffset);
        }
        // Right Bottom
    	this.drawTexturedModalRect(x + paddingLeft + slots * slotSize, y, paddingLeft + 9 * slotSize, textureHeight - bottomOffset, paddingRight, bottomOffset);
    	
    	return y + bottomOffset;
	}

	public int drawPlayerTopBar(int x, int y) {
        this.drawTexturedModalRect(x, y, 0, 0, playerXSize, bottomOffset);
		return y + bottomOffset;
	}

	public int drawPlayerMidBar(int x, int y) {
		this.drawTexturedModalRect(x, y, 0, midBarOffset, playerXSize, midBarHeight);
		return y + midBarHeight;
	}

	public int drawPlayerInventory(int x, int y) {        
        this.drawTexturedModalRect(x, y, 0, 126+4, playerXSize, 96);
		return y+96;
	}

	public int drawPlayerInventoryConnector(int x, int y, int aboveWidth, int horizSlots) {
    	if (horizSlots > 9) {
    		return drawBottomBar(x, y, horizSlots);
    	} else if (horizSlots < 9){
    		return drawPlayerTopBar((aboveWidth - playerXSize) / 2, y);
    	} else {
    		return drawPlayerMidBar((aboveWidth - playerXSize) / 2, y);
    	}
	}
    
    public void drawFluid(Fluid fluid, int x, int y, int width, int height, int scale) {
		TextureAtlasSprite sprite = mc.getTextureMapBlocks().getAtlasSprite(fluid.getStill().toString());
		if(sprite != null)
		{
			drawSprite(sprite, fluid.getColor(), x, y, width, height, scale);
		}
    }
    
    public void drawSprite(TextureAtlasSprite sprite, int col, int x, int y, int width, int height, int scale) {
    	this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

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
		        buffer.pos(x+offX, y+offY, this.zLevel).tex(minU, minV).endVertex();
		        buffer.pos(x+offX, y+offY+curHeight, this.zLevel).tex(minU, maxVScaled).endVertex();
		        buffer.pos(x+offX+curWidth, y+offY+curHeight, this.zLevel).tex(maxUScaled, maxVScaled).endVertex();
		        buffer.pos(x+offX+curWidth, y+offY, this.zLevel).tex(maxUScaled, minV).endVertex();
	        }
        }
        tessellator.draw();
        
		this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);	
    }

	public void drawTankBlock(int x, int y, int horizSlots, int inventoryRows, Fluid fluid, float percentFull) {
		int width = horizSlots * slotSize;
		int height = inventoryRows * slotSize;
		Gui.drawRect(x, y, x+width, y+height, 0xFF000000);
		
    	if (percentFull > 0 && fluid != null) {
    		int fullHeight = Math.max(1, (int) (height * percentFull));
    		drawFluid(fluid, x, y + height - fullHeight, width, fullHeight, 2);
    	}
    	GlStateManager.color(1, 1, 1, 1);
	}
}
