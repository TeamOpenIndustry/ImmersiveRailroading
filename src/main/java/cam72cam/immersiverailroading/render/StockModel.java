package cam72cam.immersiverailroading.render;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.render.obj.OBJModel;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class StockModel extends OBJModel {
	public StockModel(ResourceLocation modelLoc) throws Exception {
		super(modelLoc);
	}

	public void draw(EntityRollingStock stock) {
		if (stock instanceof LocomotiveSteam) {
			drawSteamLocomotive((LocomotiveSteam) stock);
		} else if (stock instanceof EntityMoveableRollingStock) {
			drawStandardStock((EntityMoveableRollingStock) stock);
		} else {
			draw();
		}
	}

	private void drawStandardStock(EntityMoveableRollingStock stock) {
		if (stock.frontYaw == null || stock.rearYaw == null) {
			draw();
			return;
		}

		EntityRollingStockDefinition def = stock.getDefinition();

		List<String> main = new ArrayList<String>();
		List<String> front = new ArrayList<String>();
		List<String> rear = new ArrayList<String>();

		for (String group : groups()) {
			if (group.contains("BOGEY_FRONT")) {
				front.add(group);
			} else if (group.contains("BOGEY_REAR")) {
				rear.add(group);
			} else {
				main.add(group);
			}
		}

		drawGroups(main);

		GlStateManager.pushMatrix();
		GlStateManager.translate(-def.getBogeyFront(), 0, 0);
		if (!stock.isReverse) {
			GlStateManager.rotate(180 - stock.frontYaw, 0, 1, 0);
		} else {
			GlStateManager.rotate(180 - stock.rearYaw, 0, 1, 0);
		}
		GlStateManager.rotate(-(180 - stock.rotationYaw), 0, 1, 0);
		GlStateManager.translate(def.getBogeyFront(), 0, 0);
		drawGroups(front);
		GlStateManager.popMatrix();

		GlStateManager.pushMatrix();
		GlStateManager.translate(-def.getBogeyRear(), 0, 0);
		if (!stock.isReverse) {
			GlStateManager.rotate(180 - stock.rearYaw, 0, 1, 0);
		} else {
			GlStateManager.rotate(180 - stock.frontYaw, 0, 1, 0);
		}
		GlStateManager.rotate(-(180 - stock.rotationYaw), 0, 1, 0);
		GlStateManager.translate(def.getBogeyRear(), 0, 0);
		drawGroups(rear);
		GlStateManager.popMatrix();
	}


	private void drawSteamLocomotive(LocomotiveSteam stock) {
		drawStandardStock(stock);
	}
}
