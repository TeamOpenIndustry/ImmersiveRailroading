package cam72cam.immersiverailroading.gui.overlay;

import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.library.GuiText;
import net.minecraft.entity.Entity;
import net.minecraftforge.fluids.Fluid;

public class DieselLocomotiveOverlay extends LocomotiveOverlay {
	private static int cold = 0x992a81af;
	private static int warm = 0x992a933a;
	private static int hot = 0x99d16c15;
	private static int danger = 0x99a21010;
	
	
	
	public void draw() {
		Entity riding = mc.player.getRidingEntity();
		if (riding == null) {
			return;
		}
		if (!(riding instanceof LocomotiveDiesel)) {
			return;
		}
		LocomotiveDiesel loco = (LocomotiveDiesel) riding;
		drawGauge(0xAA79650c, ((float)loco.getLiquidAmount())/Fluid.BUCKET_VOLUME, loco.getTankCapacity().Buckets(), "B");
		int heatColor = cold;
		if (loco.getEngineTemperature() > 75) {
			heatColor = warm;
		}
		if (loco.getEngineTemperature() > 100) {
			heatColor = hot;
		}
		if (loco.getEngineTemperature() > 140) {
			heatColor = danger;
		}
		drawGauge(heatColor, loco.getEngineTemperature(), 150, "C");
		
		drawScalar(GuiText.LABEL_BRAKE.toString(), loco.getAirBrake()*10, 0, 10);
		drawScalar(GuiText.LABEL_THROTTLE.toString(), loco.getThrottle()*10, -10, 10);
		
		double speed = loco.getCurrentSpeed().metric();
		if (speed < 0) {
			speed = speed * (-1);
		}
		drawSpeedText(String.format("%.2f km/h", speed));
	}
}
