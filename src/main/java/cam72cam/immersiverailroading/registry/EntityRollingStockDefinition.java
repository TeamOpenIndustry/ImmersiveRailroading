package cam72cam.immersiverailroading.registry;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.render.obj.OBJModel;
import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.util.RealBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import util.Matrix4;

public abstract class EntityRollingStockDefinition {
	public abstract EntityRollingStock spawn(World world, BlockPos pos, EnumFacing facing);

	protected String defID;
	private String name;
	private OBJModel model;
	private Vec3d passengerCenter;
	private float bogeyFront;
	private float bogeyRear;

	private Matrix4 defaultTransform = new Matrix4();

	private double frontBounds;
	private double rearBounds;
	private double heightBounds;
	private double widthBounds;
	private double passengerCompartmentLength;
	private double passengerCompartmentWidth;
	private int weight;

	public EntityRollingStockDefinition(String defID, JsonObject data) throws Exception {
		this.defID = defID;

		name = data.get("name").getAsString();
		// model = (OBJModel) OBJLoader.INSTANCE.loadModel(new
		// ResourceLocation(data.get("model").getAsString()));
		model = new OBJModel(new ResourceLocation(data.get("model").getAsString()));
		JsonObject passenger = data.get("passenger").getAsJsonObject();
		passengerCenter = new Vec3d(passenger.get("center_x").getAsDouble(), passenger.get("center_y").getAsDouble(), 0);
		passengerCompartmentLength = passenger.get("length").getAsDouble();
		passengerCompartmentWidth = passenger.get("width").getAsDouble();

		bogeyFront = data.get("trucks").getAsJsonObject().get("front").getAsFloat();
		bogeyRear = data.get("trucks").getAsJsonObject().get("rear").getAsFloat();

		JsonObject rotations = data.get("rotate").getAsJsonObject();
		if (rotations.has("x")) {
			defaultTransform.rotate(Math.toRadians(rotations.get("x").getAsFloat()), 1, 0, 0);
		}
		if (rotations.has("y")) {
			defaultTransform.rotate(Math.toRadians(rotations.get("y").getAsFloat()), 0, 1, 0);
		}
		if (rotations.has("z")) {
			defaultTransform.rotate(Math.toRadians(rotations.get("z").getAsFloat()), 0, 0, 1);
		}

		JsonObject boundsData = data.get("bounds").getAsJsonObject();
		frontBounds = boundsData.get("front").getAsDouble();
		rearBounds = boundsData.get("rear").getAsDouble();
		widthBounds = boundsData.get("width").getAsDouble();
		heightBounds = boundsData.get("height").getAsDouble();
		
		
		//TODO
		weight = 250000; //lbs
	}

	public Vec3d getPassengerCenter() {
		return this.passengerCenter;
	}
	public Vec3d correctPassengerBounds(Vec3d pos) {
		if (pos.x > this.passengerCompartmentLength) {
			pos = new Vec3d(this.passengerCompartmentLength, pos.y, pos.z);
		}
		
		if (pos.x < -this.passengerCompartmentLength) {
			pos = new Vec3d(-this.passengerCompartmentLength, pos.y, pos.z);
		}
		
		if (Math.abs(pos.z) > this.passengerCompartmentWidth/2) {
			pos = new Vec3d(pos.x, pos.y, Math.copySign(this.passengerCompartmentWidth/2, pos.z));
		}
		
		return pos;
	}

	public float getBogeyFront() {
		return this.bogeyFront;
	}

	public float getBogeyRear() {
		return this.bogeyRear;
	}
	
	public double getCouplerPosition(CouplerType coupler) {
		switch(coupler) {
		case FRONT:
			return this.frontBounds + Config.couplerRange;
		case BACK:
			return this.rearBounds + Config.couplerRange;
		default:
			return 0;
		}
	}

	public AxisAlignedBB getBounds(EntityMoveableRollingStock stock) {
		return new RealBB(frontBounds, rearBounds, widthBounds, heightBounds, stock.rotationYaw).offset(stock.getPositionVector());
	}

	public List<String> getTooltip() {
		List<String> tips = new ArrayList<String>();
		return tips;
	}

	public double getPassengerCompartmentWidth() {
		return this.passengerCompartmentWidth;
	}

	public OBJModel getModel() {
		return model;
	}

	public Matrix4 getDefaultTransformation() {
		return defaultTransform;
	}

	/**
	 * @return Stock Weight in KG
	 */
	public int getWeight() {
		return this.weight;
	}
}
