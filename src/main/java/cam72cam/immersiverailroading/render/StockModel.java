package cam72cam.immersiverailroading.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.lwjgl.util.vector.Vector3f;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock.PosRot;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.render.obj.OBJModel;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

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
		if (stock.frontYaw == null || stock.rearYaw == null) {
			draw();
			return;
		}

		EntityRollingStockDefinition def = stock.getDefinition();

		List<String> main = new ArrayList<String>();
		List<String> frontBogey = new ArrayList<String>();
		Map<String, List<String>> frontBogeyWheels = new HashMap<String, List<String>>();
		List<String> rearBogey = new ArrayList<String>();
		Map<String, List<String>> rearBogeyWheels = new HashMap<String, List<String>>();
		Map<String, List<String>> drivingWheels = new HashMap<String, List<String>>();
		
		List<String> connectingRodLeft = new ArrayList<String>();
		List<String> connectingRodRight = new ArrayList<String>();
		List<String> drivingRodLeft = new ArrayList<String>();
		List<String> drivingRodRight = new ArrayList<String>();
		List<String> pistonRodLeft = new ArrayList<String>();
		List<String> pistonRodRight = new ArrayList<String>();

		for (String group : groups()) {
			if (group.contains("BOGEY_FRONT")) {
				if (group.contains("WHEEL")) {
					String groupName = group.split("[_" + Pattern.quote(".") + "]")[3];
					if (!frontBogeyWheels.containsKey(groupName)) {
						List<String> names = new ArrayList<String>();
						names.add(group);
						frontBogeyWheels.put(groupName, names);
					} else {
						frontBogeyWheels.get(groupName).add(group);
					}
				} else {
					frontBogey.add(group);
				}
			} else if (group.contains("BOGEY_REAR")) {
				if (group.contains("WHEEL")) {
					String groupName = group.split("[_" + Pattern.quote(".") + "]")[3];
					if (!rearBogeyWheels.containsKey(groupName)) {
						List<String> names = new ArrayList<String>();
						names.add(group);
						rearBogeyWheels.put(groupName, names);
					} else {
						rearBogeyWheels.get(groupName).add(group);
					}
				} else {
					rearBogey.add(group);
				}
			} else if (group.contains("WHEEL_DRIVER")) {
				String groupName = group.split("[_" + Pattern.quote(".") + "]")[2];
				if (!drivingWheels.containsKey(groupName)) {
					List<String> names = new ArrayList<String>();
					names.add(group);
					drivingWheels.put(groupName, names);
				} else {
					drivingWheels.get(groupName).add(group);
				}
			} else if (group.contains("CONNECTING_ROD")) {
				if (group.contains("LEFT")) {
					connectingRodLeft.add(group);
				}
				if (group.contains("RIGHT")) {
					connectingRodRight.add(group);
				}
			} else if (group.contains("DRIVING_ROD")) {
				if (group.contains("LEFT")) {
					drivingRodLeft.add(group);
				}
				if (group.contains("RIGHT")) {
					drivingRodRight.add(group);
				}
			} else if (group.contains("PISTON_ROD")) {
				if (group.contains("LEFT")) {
					pistonRodLeft.add(group);
				}
				if (group.contains("RIGHT")) {
					pistonRodRight.add(group);
				}
			} else {
				main.add(group);
			}
		}

		drawGroups(main);

		
		if (frontBogey.size() != 0 && rearBogey.size() != 0) {

			Vector3f frontVec = centerOfGroups(frontBogey);
			Vector3f rearVec = centerOfGroups(rearBogey);
			
			PosRot frontPos = stock.predictFrontBogeyPosition(-frontVec.x - def.getBogeyFront());
			PosRot rearPos = stock.predictRearBogeyPosition(rearVec.x + def.getBogeyRear());
	
			GlStateManager.pushMatrix();
			
			Vec3d frontPosActual = VecUtil.rotateYaw(frontPos, 180 - stock.rotationYaw);
			GlStateManager.translate(frontPosActual.x, frontPosActual.y, frontPosActual.z);
			
			GlStateManager.rotate(-(180 - stock.rotationYaw + frontPos.getRotation()), 0, 1, 0);
			GlStateManager.translate(-frontVec.x, 0, 0);
			drawGroups(frontBogey);
			for (List<String> wheel : frontBogeyWheels.values()) {
				float circumference = heightOfGroups(wheel) * (float)Math.PI;
				float relDist = distance % circumference;
				Vector3f wheelPos = centerOfGroups(wheel);
				GlStateManager.pushMatrix();
				GlStateManager.translate(wheelPos.x, wheelPos.y, wheelPos.z);
				GlStateManager.rotate(-360 * relDist / circumference, 0, 0, 1);
				GlStateManager.translate(-wheelPos.x, -wheelPos.y, -wheelPos.z);
				drawGroups(wheel);
				GlStateManager.popMatrix();
			}
			GlStateManager.popMatrix();
			
			GlStateManager.pushMatrix();
			
			Vec3d rearPosActual = VecUtil.rotateYaw(rearPos, 180 - stock.rotationYaw);
			GlStateManager.translate(rearPosActual.x, rearPosActual.y, rearPosActual.z);
			
			GlStateManager.rotate(-(180 - stock.rotationYaw + rearPos.getRotation()), 0, 1, 0);
			GlStateManager.translate(-rearVec.x, 0, 0);
			drawGroups(rearBogey);
			for (List<String> wheel : rearBogeyWheels.values()) {
				float circumference = heightOfGroups(wheel) * (float)Math.PI;
				float relDist = distance % circumference;
				Vector3f wheelPos = centerOfGroups(wheel);
				GlStateManager.pushMatrix();
				GlStateManager.translate(wheelPos.x, wheelPos.y, wheelPos.z);
				GlStateManager.rotate(-360 * relDist / circumference, 0, 0, 1);
				GlStateManager.translate(-wheelPos.x, -wheelPos.y, -wheelPos.z);
				drawGroups(wheel);
				GlStateManager.popMatrix();
			}
			GlStateManager.popMatrix();
		}
		
		if (lastTick != stock.ticksExisted) {
			lastTick = stock.ticksExisted;
			distance += stock.getCurrentSpeed().minecraft()  * (stock.isReverse ? -1 : 1);
		}
		
		for (List<String> wheel : drivingWheels.values()) {
			float circumference = heightOfGroups(wheel) * (float)Math.PI;
			float relDist = distance % circumference;
			float wheelAngle = 360 * relDist / circumference;
			Vector3f wheelPos = centerOfGroups(wheel);
			GlStateManager.pushMatrix();
			GlStateManager.translate(wheelPos.x, wheelPos.y, wheelPos.z);
			GlStateManager.rotate(wheelAngle, 0, 0, 1);
			GlStateManager.translate(-wheelPos.x, -wheelPos.y, -wheelPos.z);
			drawGroups(wheel);
			GlStateManager.popMatrix();
		}
		
		// left is offset by -90
		// Assumes rods are in identical positions
		{
			List<List<String>> wheels = new ArrayList<List<String>>();
			wheels.addAll(drivingWheels.values());
			List<String> wheel = wheels.get(wheels.size()/2);
			float circumference = heightOfGroups(wheel) * (float)Math.PI;
			float relDist = distance % circumference;
			float wheelAngle = 360 * relDist / circumference;

			Vector3f wheelPos = centerOfGroups(wheel);
			Vector3f connRodPos =  centerOfGroups(connectingRodRight);
			float connRodOffset = connRodPos.x - wheelPos.x;
			Vector3f drivingRodMin = minOfGroup(drivingRodRight);
			Vector3f drivingRodMax = maxOfGroup(drivingRodRight);
			float drivingRodHeight = drivingRodMax.y - drivingRodMin.y;
			float drivingRodLength = drivingRodMax.x - drivingRodMin.x;
			float drivingRodCenterLength = drivingRodLength - drivingRodHeight;  
			
			Vec3d rightConnRodMovment = VecUtil.fromYaw(connRodOffset, wheelAngle - 90);
			double drivingRodHorizRight = Math.sqrt(drivingRodCenterLength * drivingRodCenterLength - rightConnRodMovment.z * rightConnRodMovment.z);
			Vec3d leftConnRodMovment = VecUtil.fromYaw(connRodOffset, wheelAngle);
			double drivingRodHorizLeft = Math.sqrt(drivingRodCenterLength * drivingRodCenterLength - leftConnRodMovment.z * leftConnRodMovment.z);
			
			Vector3f pistonMax = maxOfGroup(pistonRodRight);
			double pistonDeltaRight = -pistonMax.x - drivingRodHorizRight + rightConnRodMovment.x;
			double pistonDeltaLeft = -pistonMax.x - drivingRodHorizLeft + leftConnRodMovment.x;
			
			GlStateManager.pushMatrix();
			{
				GlStateManager.translate(-connRodOffset, 0, 0);
				GlStateManager.translate(rightConnRodMovment.x, rightConnRodMovment.z, 0);
				drawGroups(connectingRodRight);
				
				GlStateManager.pushMatrix();
				GlStateManager.translate(connRodPos.x, connRodPos.y, connRodPos.z);
				GlStateManager.rotate((float) Math.toDegrees(Math.atan2(rightConnRodMovment.z, drivingRodHorizRight)), 0, 0, 1);
				GlStateManager.translate(-connRodPos.x, -connRodPos.y, -connRodPos.z);
				drawGroups(drivingRodRight);
				GlStateManager.popMatrix();
				
			}
			GlStateManager.popMatrix();
			GlStateManager.pushMatrix();
			{
				GlStateManager.translate(pistonDeltaRight, 0, 0);
				drawGroups(pistonRodRight);
			}
			GlStateManager.popMatrix();
			
			GlStateManager.pushMatrix();
			{
				GlStateManager.translate(-connRodOffset, 0, 0);
				GlStateManager.translate(leftConnRodMovment.x, leftConnRodMovment.z, 0);
				drawGroups(connectingRodLeft);
				
				GlStateManager.pushMatrix();
				GlStateManager.translate(connRodPos.x, connRodPos.y, connRodPos.z);
				GlStateManager.rotate((float) Math.toDegrees(Math.atan2(leftConnRodMovment.z, drivingRodHorizLeft)), 0, 0, 1);
				GlStateManager.translate(-connRodPos.x, -connRodPos.y, -connRodPos.z);
				drawGroups(drivingRodLeft);
				GlStateManager.popMatrix();
				
			}
			GlStateManager.popMatrix();
			GlStateManager.pushMatrix();
			{
				GlStateManager.translate(pistonDeltaLeft, 0, 0);
				drawGroups(pistonRodLeft);
			}
			GlStateManager.popMatrix();
		}
		
	}
	int lastTick = 0;
	float distance = 0;
}
