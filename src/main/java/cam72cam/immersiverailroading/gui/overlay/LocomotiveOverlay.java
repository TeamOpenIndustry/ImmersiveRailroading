package cam72cam.immersiverailroading.gui.overlay;

import cam72cam.immersiverailroading.Config;
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
	private static final int gaugeSpacer = 30;

	public LocomotiveOverlay() {
		mc = Minecraft.getMinecraft();
		ScaledResolution scaled = new ScaledResolution(mc);
		screenWidth = scaled.getScaledWidth();
		screenHeight = scaled.getScaledHeight();
		
		currPosX = (int) (screenWidth * (Config.GUIPositionHorizontal/100f));
		currPosY = (int) (screenHeight * ((Config.GUIPositionVertical-10)/100f));
	}

	public void drawGauge(int color, float liquidAmount, float tankCapacity, String units) {
		String amount = String.format("%.1f%s", liquidAmount, units);
		String capacity = String.format("%.1f%s", tankCapacity, units);
		drawRect(currPosX, currPosY, currPosX + gaugeWidth, currPosY + gaugeHeight, 0xFF4d4d4d);
		int quantHeight = (int)(gaugeHeight * (liquidAmount / tankCapacity));
		drawRect(currPosX, currPosY + (gaugeHeight - quantHeight), currPosX + gaugeWidth, currPosY + gaugeHeight, color);
		drawCenteredString(mc.fontRenderer, amount, currPosX + gaugeWidth/2, currPosY-12, 0xFFFFFF);
		drawCenteredString(mc.fontRenderer, capacity, currPosX + gaugeWidth/2, currPosY + gaugeHeight + 2, 0xFFFFFF);
		currPosX += gaugeWidth + gaugeSpacer;
	}
}
