package cam72cam.immersiverailroading.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.entity.EntityBuildableRollingStock;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock.PosRot;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.model.RenderComponent;
import cam72cam.immersiverailroading.model.MultiRenderComponent;
import cam72cam.immersiverailroading.model.obj.OBJModel;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveSteamDefinition;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class StockModel extends OBJRender {
	private static final int MALLET_ANGLE_REAR = -45;

	private static Map<OBJModel, StockModel> cache = new HashMap<OBJModel, StockModel>();
	
	public static StockModel get(OBJModel model) {
		if (!cache.containsKey(model)) {
			cache.put(model, new StockModel(model));
		}
		return cache.get(model);
	}


	private StockModel(OBJModel objModel) {
		super(objModel);
	}

	private boolean isBuilt;
	private List<RenderComponentType> availComponents;

	private double distanceTraveled;
	private void initComponents(EntityBuildableRollingStock stock) {
		this.isBuilt = stock.isBuilt();
		
		if (!isBuilt) {
			this.availComponents = new ArrayList<RenderComponentType>();
			for (ItemComponentType item : stock.getItemComponents()) {
				this.availComponents.addAll(item.render);
			}
		}
	}
	
	private void drawComponent(RenderComponent component) {
		if (component != null) {
			if (!isBuilt) {
				if (!availComponents.contains(component.type)) {
					// MISSING COMPONENT
					return;
				}
				// remove first
				availComponents.remove(component.type);
			}
			
			drawGroups(component.modelIDs);
		}
	}
	
	private void drawComponents(List<RenderComponent> components) {
		if (components == null) {
			return;
		}
		for (RenderComponent component : components) {
			this.drawComponent(component);
		}
	}

	public void draw(EntityRollingStock stock, float partialTicks) {
		GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, model.tex != null);
		
		
		if (stock instanceof EntityMoveableRollingStock) {
			EntityMoveableRollingStock mstock = (EntityMoveableRollingStock) stock;
			this.distanceTraveled = mstock.distanceTraveled + mstock.getCurrentSpeed().minecraft() * mstock.clientTicksPerServerTick * partialTicks * 1.1; 
		} else {
			this.distanceTraveled = 0;
		}

		if (model.tex != null) {
			Minecraft.getMinecraft().getTextureManager().bindTexture(model.tex);
		}
		
		if (stock instanceof LocomotiveSteam) {
			drawSteamLocomotive((LocomotiveSteam) stock);
		} else if (stock instanceof LocomotiveDiesel) {
			drawDieselLocomotive((LocomotiveDiesel)stock);
		} else if (stock instanceof EntityMoveableRollingStock) {
			drawStandardStock((EntityMoveableRollingStock) stock);
		} else {
			draw();
		}
		
		tex.restore();
	}

	private void drawStandardStock(EntityMoveableRollingStock stock) {
		EntityRollingStockDefinition def = stock.getDefinition();
		
		initComponents(stock);
		
		drawComponent(def.getComponent(RenderComponentType.FRAME));
		drawComponent(def.getComponent(RenderComponentType.SHELL));
		List<RenderComponent> wheels = def.getComponents(RenderComponentType.FRAME_WHEEL_X);
		if (wheels != null) {
			for (RenderComponent wheel : wheels) {
				double circumference = wheel.height() * (float) Math.PI;
				double relDist = distanceTraveled % circumference;
				Vec3d wheelPos = wheel.center();
				GlStateManager.pushMatrix();
				GlStateManager.translate(wheelPos.x, wheelPos.y, wheelPos.z);
				GlStateManager.rotate((float) (360 * relDist / circumference), 0, 0, 1);
				GlStateManager.translate(-wheelPos.x, -wheelPos.y, -wheelPos.z);
				drawComponent(wheel);
				GlStateManager.popMatrix();
			}
		}
		

		if (def.getComponent(RenderComponentType.BOGEY_POS, "FRONT") != null) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(-def.getBogeyFront(), 0, 0);
			GlStateManager.rotate(180 - stock.getFrontYaw(), 0, 1, 0);		
			GlStateManager.rotate(-(180 - stock.rotationYaw), 0, 1, 0);
			GlStateManager.translate(def.getBogeyFront(), 0, 0);
			drawComponent(def.getComponent(RenderComponentType.BOGEY_POS, "FRONT"));
			wheels = def.getComponents(RenderComponentType.BOGEY_POS_WHEEL_X, "FRONT");
			if (wheels != null) {
				for (RenderComponent wheel : wheels) {
					double circumference = wheel.height() * (float) Math.PI;
					double relDist = distanceTraveled % circumference;
					Vec3d wheelPos = wheel.center();
					GlStateManager.pushMatrix();
					GlStateManager.translate(wheelPos.x, wheelPos.y, wheelPos.z);
					GlStateManager.rotate((float) (360 * relDist / circumference), 0, 0, 1);
					GlStateManager.translate(-wheelPos.x, -wheelPos.y, -wheelPos.z);
					drawComponent(wheel);
					GlStateManager.popMatrix();
				}
			}
			GlStateManager.popMatrix();
		}
		
		if (def.getComponent(RenderComponentType.BOGEY_POS) != null) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(-def.getBogeyRear(), 0, 0);
			GlStateManager.rotate(180 - stock.getRearYaw(), 0, 1, 0);
			GlStateManager.rotate(-(180 - stock.rotationYaw), 0, 1, 0);
			GlStateManager.translate(def.getBogeyRear(), 0, 0);
			drawComponent(def.getComponent(RenderComponentType.BOGEY_POS, "REAR"));
			wheels = def.getComponents(RenderComponentType.BOGEY_POS_WHEEL_X, "REAR");
			if (wheels != null) {
				for (RenderComponent wheel : wheels) {
					double circumference = wheel.height() * (float) Math.PI;
					double relDist = distanceTraveled % circumference;
					Vec3d wheelPos = wheel.center();
					GlStateManager.pushMatrix();
					GlStateManager.translate(wheelPos.x, wheelPos.y, wheelPos.z);
					GlStateManager.rotate((float) (360 * relDist / circumference), 0, 0, 1);
					GlStateManager.translate(-wheelPos.x, -wheelPos.y, -wheelPos.z);
					drawComponent(wheel);
					GlStateManager.popMatrix();
				}
			}
			GlStateManager.popMatrix();
		}

		if (this.isBuilt) {
			drawComponent(def.getComponent(RenderComponentType.REMAINING));
		}
	}

	private void drawDieselLocomotive(EntityMoveableRollingStock stock) {
		EntityRollingStockDefinition def = stock.getDefinition();
		
		drawStandardStock(stock);

		drawComponent(def.getComponent(RenderComponentType.FUEL_TANK));
		drawComponent(def.getComponent(RenderComponentType.ALTERNATOR));
		drawComponent(def.getComponent(RenderComponentType.ENGINE_BLOCK));
		
		drawComponents(def.getComponents(RenderComponentType.CAB));
		drawComponents(def.getComponents(RenderComponentType.WHISTLE));
		drawComponents(def.getComponents(RenderComponentType.BELL));
		drawComponents(def.getComponents(RenderComponentType.HORN));
	}


	private void drawSteamLocomotive(LocomotiveSteam stock) {
		LocomotiveSteamDefinition def = stock.getDefinition();
		
		initComponents(stock);

		drawBogies(stock);

		switch (def.getValveGear()) {
		case WALSCHAERTS:
			{
				List<RenderComponent> wheels = def.getComponents(RenderComponentType.WHEEL_DRIVER_X);
				drawDrivingWheels(stock, wheels);
				RenderComponent center = new MultiRenderComponent(wheels);
				RenderComponent wheel = wheels.get(wheels.size() / 2);
				drawWalschaerts(stock, "LEFT", 0, wheel.height(), center.center(), wheel.center());
				drawWalschaerts(stock, "RIGHT", -90, wheel.height(), center.center(), wheel.center());
			}
			break;
		case MALLET_WALSCHAERTS:
			{
				GL11.glPushMatrix();
				
				RenderComponent frontLocomotive = def.getComponent(RenderComponentType.FRONT_LOCOMOTIVE);
				Vec3d frontVec = frontLocomotive.center();
				PosRot frontPos = stock.predictFrontBogeyPosition((float) (-frontVec.x - def.getBogeyFront()));
				Vec3d frontPosActual = VecUtil.rotateYaw(frontPos, 180 - stock.rotationYaw);
				
				GlStateManager.translate(frontPosActual.x, frontPosActual.y, frontPosActual.z);
				GlStateManager.rotate(-(180 - stock.rotationYaw + frontPos.getRotation()) + 180, 0, 1, 0);
				GlStateManager.translate(-frontVec.x, 0, 0);
				
				List<RenderComponent> wheels = def.getComponents(RenderComponentType.WHEEL_DRIVER_FRONT_X);
				RenderComponent center = new MultiRenderComponent(wheels);
				drawComponent(def.getComponent(RenderComponentType.STEAM_CHEST_FRONT));
				drawComponent(frontLocomotive);
				drawDrivingWheels(stock, wheels);
				RenderComponent wheel = wheels.get(wheels.size() / 2);
				drawWalschaerts(stock, "LEFT_FRONT", 0, wheel.height(), center.center(), wheel.center());
				drawWalschaerts(stock, "RIGHT_FRONT", -90, wheel.height(), center.center(), wheel.center());
				GL11.glPopMatrix();
			}
			{
				List<RenderComponent> wheels = def.getComponents(RenderComponentType.WHEEL_DRIVER_REAR_X);
				RenderComponent center = new MultiRenderComponent(wheels);
				drawDrivingWheels(stock, wheels);
				RenderComponent wheel = wheels.get(wheels.size() / 2);
				drawWalschaerts(stock, "LEFT_REAR", 0 + MALLET_ANGLE_REAR, center.height(), center.center(), wheel.center());
				drawWalschaerts(stock, "RIGHT_REAR", -90 + MALLET_ANGLE_REAR,  center.height(), center.center(), wheel.center());
			}
			break;
		case CLIMAX:
			break;
		case SHAY:
			break;
		}
		
		// Draw remaining groups

		drawComponent(def.getComponent(RenderComponentType.FRAME));
		drawComponent(def.getComponent(RenderComponentType.SHELL));
		
		drawComponents(def.getComponents(RenderComponentType.BOILER_SEGMENT_X));
		drawComponent(def.getComponent(RenderComponentType.FIREBOX));
		drawComponent(def.getComponent(RenderComponentType.STEAM_CHEST));
		drawComponent(def.getComponent(RenderComponentType.STEAM_CHEST_REAR));
		drawComponent(def.getComponent(RenderComponentType.PIPING));
		drawComponents(def.getComponents(RenderComponentType.CYLINDER_SIDE));
		
		drawComponents(def.getComponents(RenderComponentType.CAB));
		drawComponents(def.getComponents(RenderComponentType.WHISTLE));
		drawComponents(def.getComponents(RenderComponentType.BELL));
		drawComponents(def.getComponents(RenderComponentType.HORN));
		
		if (stock.isBuilt()) {
			drawComponent(def.getComponent(RenderComponentType.REMAINING));
		}
	}


	private void drawDrivingWheels(LocomotiveSteam stock, List<RenderComponent> wheels) {
		for (RenderComponent wheel : wheels) {
			double circumference = wheel.height() * (float) Math.PI;
			double relDist = distanceTraveled % circumference;
			double wheelAngle = 360 * relDist / circumference;
			if (wheel.side.contains("REAR")) {
				//MALLET HACK
				wheelAngle += MALLET_ANGLE_REAR;
			}
			Vec3d wheelPos = wheel.center();
			GlStateManager.pushMatrix();
			GlStateManager.translate(wheelPos.x, wheelPos.y, wheelPos.z);
			GlStateManager.rotate((float) wheelAngle, 0, 0, 1);
			GlStateManager.translate(-wheelPos.x, -wheelPos.y, -wheelPos.z);
			drawComponent(wheel);
			GlStateManager.popMatrix();
		}
	}


	private void drawBogies(EntityMoveableRollingStock stock) {
		EntityRollingStockDefinition def = stock.getDefinition();
		
		RenderComponent frontBogey = def.getComponent(RenderComponentType.BOGEY_FRONT);
		List<RenderComponent> frontBogeyWheels = def.getComponents(RenderComponentType.BOGEY_FRONT_WHEEL_X);
		RenderComponent rearBogey = def.getComponent(RenderComponentType.BOGEY_REAR);
		List<RenderComponent> rearBogeyWheels = def.getComponents(RenderComponentType.BOGEY_REAR_WHEEL_X);

		if (frontBogey != null) {

			Vec3d frontVec = frontBogey.center();
			PosRot frontPos = stock.predictFrontBogeyPosition((float) (-frontVec.x - def.getBogeyFront()));

			GlStateManager.pushMatrix();

			Vec3d frontPosActual = VecUtil.rotateYaw(frontPos, 180 - stock.rotationYaw);
			GlStateManager.translate(frontPosActual.x, frontPosActual.y, frontPosActual.z);

			GlStateManager.rotate(-(180 - stock.rotationYaw + frontPos.getRotation()), 0, 1, 0);
			GlStateManager.translate(-frontVec.x, 0, 0);
			drawComponent(frontBogey);
			if (frontBogeyWheels != null) {
				for (RenderComponent wheel : frontBogeyWheels) {
					double circumference = wheel.height() * (float) Math.PI;
					double relDist = distanceTraveled % circumference;
					Vec3d wheelPos = wheel.center();
					GlStateManager.pushMatrix();
					GlStateManager.translate(wheelPos.x, wheelPos.y, wheelPos.z);
					GlStateManager.rotate((float) (-360 * relDist / circumference), 0, 0, 1);
					GlStateManager.translate(-wheelPos.x, -wheelPos.y, -wheelPos.z);
					drawComponent(wheel);
					GlStateManager.popMatrix();
				}
			}
			GlStateManager.popMatrix();
		}
		if (rearBogey != null)
		{
			Vec3d rearVec = rearBogey.center();
			PosRot rearPos = stock.predictRearBogeyPosition((float) (rearVec.x + def.getBogeyRear()));
			
			GlStateManager.pushMatrix();

			Vec3d rearPosActual = VecUtil.rotateYaw(rearPos, 180 - stock.rotationYaw);
			GlStateManager.translate(rearPosActual.x, rearPosActual.y, rearPosActual.z);

			GlStateManager.rotate(-(180 - stock.rotationYaw + rearPos.getRotation()), 0, 1, 0);
			GlStateManager.translate(-rearVec.x, 0, 0);
			drawComponent(rearBogey);
			if (rearBogeyWheels != null) {
				for (RenderComponent wheel : rearBogeyWheels) {
					double circumference = wheel.height() * (float) Math.PI;
					double relDist = distanceTraveled % circumference;
					Vec3d wheelPos = wheel.center();
					GlStateManager.pushMatrix();
					GlStateManager.translate(wheelPos.x, wheelPos.y, wheelPos.z);
					GlStateManager.rotate((float) (360 * relDist / circumference), 0, 0, 1);
					GlStateManager.translate(-wheelPos.x, -wheelPos.y, -wheelPos.z);
					drawComponent(wheel);
					GlStateManager.popMatrix();
				}
			}
			GlStateManager.popMatrix();
		}
	}

	private void drawWalschaerts(LocomotiveSteam stock, String side, int wheelAngleOffset, double diameter, Vec3d wheelCenter, Vec3d wheelPos) {
		LocomotiveSteamDefinition def = stock.getDefinition();
		
		double circumference = diameter * (float) Math.PI;
		double relDist = distanceTraveled % circumference;
		double wheelAngle = 360 * relDist / circumference + wheelAngleOffset;
		
		RenderComponent connectingRod = def.getComponent(RenderComponentType.SIDE_ROD_SIDE, side);
		RenderComponent drivingRod = def.getComponent(RenderComponentType.MAIN_ROD_SIDE, side);
		RenderComponent pistonRod = def.getComponent(RenderComponentType.PISTON_ROD_SIDE, side);

		Vec3d connRodPos = connectingRod.center();
		double connRodOffset = connRodPos.x - wheelCenter.x;
		Vec3d drivingRodMin = drivingRod.min();
		Vec3d drivingRodMax = drivingRod.max();
		double drivingRodHeight = drivingRodMax.y - drivingRodMin.y;
		double drivingRodLength = drivingRodMax.x - drivingRodMin.x;
		double drivingRodCenterLength = drivingRodLength - drivingRodHeight;

		Vec3d connRodMovment = VecUtil.fromYaw(connRodOffset, (float) wheelAngle);
		double drivingRodHoriz = Math.sqrt(drivingRodCenterLength * drivingRodCenterLength - connRodMovment.z * connRodMovment.z);

		double pistonDelta = connRodMovment.x - 0.3;

		RenderComponent crossHead = def.getComponent(RenderComponentType.UNION_LINK_SIDE, side);
		RenderComponent combinationLever = def.getComponent(RenderComponentType.COMBINATION_LEVER_SIDE, side);
		RenderComponent returnCrank = def.getComponent(RenderComponentType.ECCENTRIC_CRANK_SIDE, side);
		RenderComponent returnCrankRod = def.getComponent(RenderComponentType.ECCENTRIC_ROD_SIDE, side);
		RenderComponent slottedLink = def.getComponent(RenderComponentType.EXPANSION_LINK_SIDE, side);
		
		double returnCrankHeight = returnCrank.height();
		double returnCrankLength = returnCrank.length();
		Vec3d returnCrankPos = returnCrank.center();
		float returnCrankAngle = 180 - 60;

		double returnCrankRodHeight = returnCrankRod.height();
		double returnCrankRodLength = returnCrankRod.length();
		Vec3d returnCrankRodCenter = returnCrankRod.center();
		Vec3d crankOffset = VecUtil.fromYaw(returnCrankLength - returnCrankHeight, (float) (90 + wheelAngle + returnCrankAngle));

		Vec3d slottedLinkMin = slottedLink.min();
		double slottedLinkWidth = slottedLink.length();
		Vec3d slottedLinkCenter = slottedLink.center();
		
		Vec3d returnCrankRodPos = new Vec3d(connRodMovment.x, connRodMovment.z, 0);
		returnCrankRodPos = returnCrankRodPos.addVector(wheelPos.x, wheelPos.y, returnCrankRodCenter.z);
		returnCrankRodPos = returnCrankRodPos.addVector(crankOffset.x, crankOffset.z, 0);
		// This line is not taking into account the fact that it is attached
		// to a swing arm. Therefore this and the following lines are "close
		// enough", but not quite left
		Vec3d returnCrankRodOffset = new Vec3d(returnCrankRodPos.x - slottedLinkMin.x,
				returnCrankRodPos.y - slottedLinkMin.y - slottedLinkWidth / 2, 0);
		float returnCrankRodAngle = (float) Math.toDegrees(MathHelper.atan2(returnCrankRodOffset.y, returnCrankRodOffset.x));
		Vec3d returnCrankRodActual = VecUtil.fromYaw(returnCrankRodLength - returnCrankHeight, returnCrankRodAngle);
		returnCrankRodActual = new Vec3d(returnCrankRodPos.x - returnCrankRodActual.z,
				returnCrankRodPos.y + returnCrankRodActual.x, 0);
		float slottedLinkAngle = (float) Math
				.toDegrees(MathHelper.atan2(-slottedLinkCenter.x + returnCrankRodActual.x, slottedLinkCenter.y - returnCrankRodActual.y));

		// CONNECTING_ROD_LEFT
		// DRIVING_ROD_LEFT
		GlStateManager.pushMatrix();
		{
			GlStateManager.translate(-connRodOffset, 0, 0);
			GlStateManager.translate(connRodMovment.x, connRodMovment.z, 0);
			drawComponent(connectingRod);

			GlStateManager.pushMatrix();
			GlStateManager.translate(connRodPos.x, connRodPos.y, connRodPos.z);
			GlStateManager.rotate((float) Math.toDegrees(MathHelper.atan2(connRodMovment.z, drivingRodHoriz)), 0, 0, 1);
			GlStateManager.translate(-connRodPos.x, -connRodPos.y, -connRodPos.z);
			drawComponent(drivingRod);
			GlStateManager.popMatrix();
		}
		GlStateManager.popMatrix();
		// RETURN_CRANK_LEFT
		GlStateManager.pushMatrix();
		{
			GlStateManager.translate(connRodMovment.x, connRodMovment.z, 0);
			GlStateManager.translate(wheelPos.x, wheelPos.y, returnCrankPos.z);
			GlStateManager.rotate((float) (wheelAngle + returnCrankAngle), 0, 0, 1);
			GlStateManager.translate(-returnCrankLength / 2 + returnCrankHeight / 2, 0, 0);
			GlStateManager.translate(-returnCrankPos.x, -returnCrankPos.y, -returnCrankPos.z);
			drawComponent(returnCrank);
		}
		GlStateManager.popMatrix();
		// RETURN_CRANK_ROD_LEFT
		GlStateManager.pushMatrix();
		{
			GlStateManager.translate(returnCrankRodPos.x, returnCrankRodPos.y, returnCrankRodPos.z);
			GlStateManager.rotate(returnCrankRodAngle, 0, 0, 1);
			GlStateManager.translate(-returnCrankRodLength / 2 + returnCrankRodHeight / 2, 0, 0);
			GlStateManager.translate(-returnCrankRodCenter.x, -returnCrankRodCenter.y, -returnCrankRodCenter.z);
			drawComponent(returnCrankRod);
		}
		GlStateManager.popMatrix();
		// SLOTTED_LINK_LEFT
		GlStateManager.pushMatrix();
		{
			GlStateManager.translate(slottedLinkCenter.x, slottedLinkCenter.y, slottedLinkCenter.z);
			GlStateManager.rotate(slottedLinkAngle, 0, 0, 1);
			GlStateManager.translate(-slottedLinkCenter.x, -slottedLinkCenter.y, -slottedLinkCenter.z);
			drawComponent(slottedLink);
		}
		GlStateManager.popMatrix();
		// PISTON_LEFT
		GlStateManager.pushMatrix();
		{
			GlStateManager.translate(pistonDelta, 0, 0);
			drawComponent(pistonRod);
			drawComponent(crossHead);
			// TODO rotate combination lever
			drawComponent(combinationLever);
		}
		GlStateManager.popMatrix();
		
	}
}
