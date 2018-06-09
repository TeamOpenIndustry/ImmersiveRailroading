package cam72cam.immersiverailroading.gui.overlay;

import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.library.GuiText;
import net.minecraft.entity.Entity;
import net.minecraftforge.fluids.Fluid;

public class DieselLocomotiveOverlay extends LocomotiveOverlay {
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
		
		drawScalar(GuiText.LABEL_BRAKE.toString(), loco.getAirBrake()*10, 0, 10);
		drawScalar(GuiText.LABEL_THROTTLE.toString(), loco.getThrottle()*10, -10, 10);
		
		double speed = loco.getCurrentSpeed().metric();
		if (speed < 0) {
			speed = speed * (-1);
		}
		drawSpeedText(String.format("%.2f km/h", speed));
	}
}
