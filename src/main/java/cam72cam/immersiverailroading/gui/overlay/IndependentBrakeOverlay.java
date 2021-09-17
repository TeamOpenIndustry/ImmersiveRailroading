package cam72cam.immersiverailroading.gui.overlay;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.HandCar;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Entity;

public class IndependentBrakeOverlay extends ControlsOverlay {
	public void draw() {
		Entity riding = MinecraftClient.getPlayer().getRiding();
		if (!(riding instanceof EntityMoveableRollingStock)) {
			return;
		}
		EntityMoveableRollingStock stock = (EntityMoveableRollingStock) riding;
		if (!stock.getDefinition().hasIndependentBrake() || stock instanceof Locomotive) {
			return;
		}
		drawBackground(stock);
		addSpace(10);

		drawScalar(GuiText.LABEL_BRAKE.toString(), stock.getIndependentBrake() * 10, 0, 10);
	}
}
