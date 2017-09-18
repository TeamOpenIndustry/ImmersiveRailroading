package cam72cam.immersiverailroading.registry;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.CarFreight;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class CarFreightDefinition extends EntityRollingStockDefinition {

	private int numSlots;
	private int width;
	private List<String> validCargo;

	public CarFreightDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
	}
	
	@Override
	public void parseJson(JsonObject data) throws Exception {
		super.parseJson(data);
		JsonObject freight = data.get("freight").getAsJsonObject();
		this.numSlots = freight.get("slots").getAsInt();
		this.width = freight.get("width").getAsInt();
		this.validCargo = new ArrayList<String>();
		for (JsonElement el : freight.get("cargo").getAsJsonArray()) {
			validCargo.add(el.getAsString());
		}
	}
	
	@Override
	public EntityRollingStock spawn(World world, Vec3d pos, EnumFacing facing) {
		CarFreight loco = new CarFreight(world, defID);

		loco.setPosition(pos.x, pos.y, pos.z);
		loco.prevRotationYaw = facing.getHorizontalAngle();
		loco.rotationYaw = facing.getHorizontalAngle();
		world.spawnEntity(loco);

		return loco;
	}
	
	public int getInventorySize() {
		return numSlots;
	}

	public int getInventoryWidth() {
		return width;
	}
}
