package cam72cam.immersiverailroading.gui.overlay;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.library.GuiText;
import net.minecraft.entity.Entity;
import net.minecraftforge.fluids.Fluid;

public class SteamLocomotiveOverlay extends LocomotiveOverlay {
	
	public void draw() {
		Entity riding = mc.player.getRidingEntity();
		if (riding == null) {
			return;
		}
		if (!(riding instanceof LocomotiveSteam)) {
			return;
		}
		LocomotiveSteam loco = (LocomotiveSteam) riding;
		drawGauge(0xAA0F0FFF, ((float)loco.getLiquidAmount())/Fluid.BUCKET_VOLUME, loco.getTankCapacity().Buckets(), "B");
		drawGauge(0x99DDDDDD, loco.getBoilerPressure(), loco.getDefinition().getMaxPSI(loco.gauge), "PSI");
		
		int boilerColor = 0x99d1c715;
		if (loco.getBoilerTemperature() > 95) {
			boilerColor = 0x99d16c15;
		}
		if (loco.getBoilerTemperature() > 105) {
			boilerColor = 0x99a21010;
		}
		
		drawGauge(boilerColor, loco.getBoilerTemperature(), 150, "C");
		
		drawScalar(GuiText.LABEL_BRAKE.toString(), loco.getAirBrake()*10, 0, 10);
		drawScalar(GuiText.LABEL_THROTTLE.toString(), loco.getThrottle()*10, -10, 10);
		
		double speed = loco.getCurrentSpeed().metric();
		if (speed < 0) {
			speed = speed * (-1);
		}
		if (ConfigGraphics.speedUnit == "km/h") {
			drawSpeedText(String.format("%.2f km/h", speed));
		} else if (ConfigGraphics.speedUnit == "mph") {
			speed = speed * 0.621371;
			drawSpeedText(String.format("%.2f mph", speed));
		} else if (ConfigGraphics.speedUnit == "m/s") {
			speed = speed /3.6;
			drawSpeedText(String.format("%.2f m/s", speed));
		}
	}
}
