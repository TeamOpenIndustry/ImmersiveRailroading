package cam72cam.mod.gui.container;

import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.ItemStackHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import static cam72cam.immersiverailroading.gui.GUIHelpers.CHEST_GUI_TEXTURE;

public class ClientContainerBuilder extends GuiContainer implements IContainerBuilder {
    private final ServerContainerBuilder server;
    private int centerX;
    private int centerY;

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

    public ClientContainerBuilder(ServerContainerBuilder serverContainer) {
        super(serverContainer);
        this.server = serverContainer;
        this.xSize = paddingRight + serverContainer.slotsX * slotSize + paddingLeft;
        this.ySize = 114 + serverContainer.slotsY * slotSize;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
        this.centerX = (this.width - this.xSize) / 2;
        this.centerY = (this.height - this.ySize) / 2;
        server.draw.accept(this);
    }

    /* IContainerBuilder */

    @Override
    public int drawTopBar(int x, int y, int slots) {
        super.drawTexturedModalRect(centerX + x,  centerY + y, 0, 0, paddingLeft, topOffset);
        // Top Bar
        for (int k = 1; k <= slots; k++) {
            super.drawTexturedModalRect(centerX + x + paddingLeft + (k-1) * slotSize, centerY + y, paddingLeft, 0, slotSize, topOffset);
        }
        // Top Right Corner
        super.drawTexturedModalRect(centerX + x + paddingLeft + slots * slotSize, centerY + y, paddingLeft + stdUiHorizSlots * slotSize, 0, paddingRight, topOffset);

        return y + topOffset;
    }

    @Override
    public void drawSlot(ItemStackHandler handler, int slotID, int x, int y) {
        x += paddingLeft;
        if (handler != null && handler.getSlotCount() > slotID) {
            super.drawTexturedModalRect(centerX + x, centerY + y, paddingLeft, topOffset, slotSize, slotSize);
        } else {
            Gui.drawRect(centerX + x, centerY + y, centerX + x + slotSize, centerY + y + slotSize, 0xFF444444);
        }
    }

    @Override
    public int drawSlotRow(ItemStackHandler handler, int start, int cols, int x, int y) {
        // Left Side
        super.drawTexturedModalRect(centerX + x, centerY + y, 0, topOffset, paddingLeft, slotSize);
        // Middle Slots
        for (int slotID = start; slotID < start + cols; slotID++) {
            int slotOff = (slotID - start);
            drawSlot(handler, slotID, x + slotOff * slotSize, y);
        }
        GL11.glColor4f(1, 1, 1, 1);
        // Right Side
        super.drawTexturedModalRect(centerX + x + paddingLeft + cols * slotSize, centerY + y, paddingLeft + stdUiHorizSlots * slotSize, topOffset, paddingRight, slotSize);
        return y + slotSize;
    }

    @Override
    public int drawSlotBlock(ItemStackHandler handler, int start, int cols, int x, int y) {
        if (cols < server.slotsX) {
            x += (server.slotsX - cols) * slotSize / 2;
        }

        for (int slotID = start; slotID < handler.getSlotCount(); slotID += cols) {
            y = drawSlotRow(handler, slotID, cols, x, y);
        }
        return y;
    }

    @Override
    public int drawBottomBar(int x, int y, int slots) {
        // Left Bottom
        super.drawTexturedModalRect(centerX + x, centerY + y, 0, textureHeight - bottomOffset, paddingLeft, bottomOffset);
        // Middle Bottom
        for (int k = 1; k <= slots; k++) {
            super.drawTexturedModalRect(centerX + x + paddingLeft + (k-1) * slotSize, centerY + y, paddingLeft, textureHeight - bottomOffset, slotSize, bottomOffset);
        }
        // Right Bottom
        super.drawTexturedModalRect(centerX + x + paddingLeft + slots * slotSize, centerY + y, paddingLeft + 9 * slotSize, textureHeight - bottomOffset, paddingRight, bottomOffset);

        return y + bottomOffset;
    }

    @Override
    public int drawPlayerTopBar(int x, int y) {
        super.drawTexturedModalRect(centerX + x, centerY + y, 0, 0, playerXSize, bottomOffset);
        return y + bottomOffset;
    }

    @Override
    public int drawPlayerMidBar(int x, int y) {
        super.drawTexturedModalRect(centerX + x, centerY + y, 0, midBarOffset, playerXSize, midBarHeight);
        return y + midBarHeight;
    }

    @Override
    public int drawPlayerInventory(int y, int horizSlots) {
        int normInvOffset = (horizSlots - stdUiHorizSlots) * slotSize / 2 + paddingLeft - 7;
        super.drawTexturedModalRect(centerX + normInvOffset, centerY + y, 0, 126+4, playerXSize, 96);
        return y+96;
    }

    @Override
    public int drawPlayerInventoryConnector(int x, int y, int horizSlots) {
        int normInvOffset = (horizSlots - stdUiHorizSlots) * slotSize / 2 + paddingLeft - 7;
        if (horizSlots < server.slotsX) {
            x += (server.slotsX - horizSlots) * slotSize / 2;
        }

        if (horizSlots > 9) {
            return drawBottomBar(x, y, horizSlots);
        } else if (horizSlots < 9){
            return drawPlayerTopBar(x + normInvOffset, y);
        } else {
            return drawPlayerMidBar(x + normInvOffset, y);
        }
    }

    public void drawFluid(Fluid fluid, int x, int y, int width, int height, int scale) {
        TextureAtlasSprite sprite = mc.getTextureMapBlocks().getAtlasSprite(fluid.internal.getStill().toString());
        if(sprite != null)
        {
            drawSprite(sprite, fluid.internal.getColor(), x, y, width, height, scale);
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

    @Override
    public void drawTankBlock(int x, int y, int horizSlots, int inventoryRows, Fluid fluid, float percentFull) {
        x += paddingLeft + centerX;
        y += centerY;

        int width = horizSlots * slotSize;
        int height = inventoryRows * slotSize;
        Gui.drawRect(x, y, x+width, y+height, 0xFF000000);

        if (percentFull > 0 && fluid != null) {
            int fullHeight = Math.max(1, (int) (height * percentFull));
            drawFluid(fluid, x, y + height - fullHeight, width, fullHeight, 2);
        }
        GlStateManager.color(1, 1, 1, 1);
    }

    @Override
    public void drawCenteredString(String text, int x, int y) {
        super.drawCenteredString(this.fontRenderer, text, x + centerX + this.xSize/2, y + centerY, 14737632);
        this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
    }

    @Override
    public void drawSlotOverlay(ItemStack stack, int x, int y) {
        x += centerX + 1 + paddingLeft;
        y += centerY + 1;

        this.mc.getRenderItem().renderItemIntoGUI(stack.internal, x, y);
        this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);

        GlStateManager.enableAlpha();
        GlStateManager.disableDepth();
        Gui.drawRect(x, y, x + 16, y + 16, -2130706433);
        GlStateManager.enableDepth();

        GL11.glColor4f(1,1,1,1);
    }
}
