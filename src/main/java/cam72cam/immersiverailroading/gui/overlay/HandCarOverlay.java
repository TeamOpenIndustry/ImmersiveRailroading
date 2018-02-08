package cam72cam.immersiverailroading.gui.overlay;

import cam72cam.immersiverailroading.entity.HandCar;
import cam72cam.immersiverailroading.library.GuiText;
import net.minecraft.entity.Entity;

public class HandCarOverlay extends LocomotiveOverlay {
	public void draw() {
		Entity riding = mc.player.getRidingEntity();
		if (riding == null) {
			return;
		}
		if (!(riding instanceof HandCar)) {
			return;
		}
		HandCar loco = (HandCar) riding;
		drawScalar(GuiText.LABEL_BRAKE.toString(), loco.getAirBrake()*10, 0, 10);
		drawScalar(GuiText.LABEL_THROTTLE.toString(), loco.getThrottle()*10, -10, 10);
	}
}
