package cam72cam.immersiverailroading.gui.overlay;

import cam72cam.immersiverailroading.entity.LocomotiveSteam;
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
		drawGauge(0x99DDDDDD, (int) loco.getBoilerPressure(), loco.getDefinition().getMaxPSI(), "PSI");
		
		int boilerColor = 0x99d1c715;
		if (loco.getBoilerTemperature() > 95) {
			boilerColor = 0x99d16c15;
		}
		if (loco.getBoilerTemperature() > 105) {
			boilerColor = 0x99a21010;
		}
		
		drawGauge(boilerColor, (int) loco.getBoilerTemperature(), 150, "C");
	}
}
