package cam72cam.immersiverailroading.gui.overlay;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ConfigGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

public class LocomotiveOverlay extends Gui {
	
	protected Minecraft mc;
	private int screenWidth;
	private int screenHeight;
	private int currPosX;
	private int currPosY;
	
	private static final int gaugeWidth = 10;
	private static final int gaugeHeight = 50;
	private static final int gaugeSpacer = 10;
	
	private static final int scalarWidth = 10;
	private static final int scalarHeight = 50;
	private static final int scalarSpacer = 10;

	public LocomotiveOverlay() {
		mc = Minecraft.getMinecraft();
		ScaledResolution scaled = new ScaledResolution(mc);
		screenWidth = scaled.getScaledWidth();
		screenHeight = scaled.getScaledHeight();
		
		currPosX = (int) (screenWidth * (ConfigGraphics.GUIPositionHorizontal/100f));
		currPosY = (int) (screenHeight * ((ConfigGraphics.GUIPositionVertical-10)/100f));
	}

	public void drawGauge(int color, float liquidAmount, float tankCapacity, String units) {
		String amount = String.format("%.1f%s", liquidAmount, units);
		String capacity = String.format("%.1f%s", tankCapacity, units);
		drawRect(currPosX, currPosY, currPosX + gaugeWidth, currPosY + gaugeHeight, 0xFF4d4d4d);
		int quantHeight = (int)(gaugeHeight * (liquidAmount / tankCapacity));
		drawRect(currPosX, currPosY + (gaugeHeight - quantHeight), currPosX + gaugeWidth, currPosY + gaugeHeight, color);
		GL11.glPushMatrix();
		{
			GL11.glTranslated(currPosX + gaugeWidth/2, currPosY-6, 0);
			double scale = 0.5;
			GL11.glScaled(scale, scale, scale);
			drawCenteredString(mc.fontRenderer, amount, 0, 0, 0xFFFFFF);
		}
		GL11.glPopMatrix();
		GL11.glPushMatrix();
		{
			GL11.glTranslated(currPosX + gaugeWidth/2, currPosY + gaugeHeight + 2, 0);
			double scale = 0.5;
			GL11.glScaled(scale, scale, scale);
			drawCenteredString(mc.fontRenderer, capacity, 0, 0, 0xFFFFFF);
		}
		GL11.glPopMatrix();
		currPosX += gaugeWidth + gaugeSpacer;
	}

	public void drawScalar(String string, float val, float min, float max) {
		//String minStr = String.format("%.1f", min);
		//String maxStr = String.format("%.1f", max);
		drawRect(currPosX+ scalarWidth/2, currPosY, currPosX + scalarWidth/4, currPosY + scalarHeight, 0xFF4d4d4d);
		int quantHeight = scalarHeight - (int)((scalarHeight-5) * ((val-min) / (max-min))) - 5;
		drawRect(currPosX, currPosY + quantHeight, currPosX + scalarWidth, currPosY + quantHeight + 5, 0xFF999999);
		//drawCenteredString(mc.fontRenderer, minStr, currPosX + scalarWidth/2, currPosY + scalarHeight + 2, 0xFFFFFF);
		//drawCenteredString(mc.fontRenderer, maxStr, currPosX + scalarWidth/2, currPosY-12, 0xFFFFFF);
		GL11.glPushMatrix();
		{
			GL11.glTranslated(currPosX + scalarWidth/2, currPosY-6, 0);
			double scale = 0.5;
			GL11.glScaled(scale, scale, scale);
			drawCenteredString(mc.fontRenderer, string, 0, 0, 0xFFFFFF);
		}
		GL11.glPopMatrix();
		currPosX += scalarWidth + scalarSpacer;
	}
}
