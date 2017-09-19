package cam72cam.immersiverailroading.registry;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.Tender;
import net.minecraft.world.World;

public class TenderDefinition extends CarTankDefinition {
	private int numSlots;
	private int width;

	public TenderDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
	}
	
	@Override
	public void parseJson(JsonObject data) throws Exception {
		super.parseJson(data);
		
		JsonObject tender = data.get("tender").getAsJsonObject();
		this.numSlots = tender.get("slots").getAsInt();
		this.width = tender.get("width").getAsInt();
	}
	
	@Override
	public EntityRollingStock instance(World world) {
		return new Tender(world, defID);
	}
	
	public int getInventorySize() {
		return numSlots;
	}

	public int getInventoryWidth() {
		return width;
	}
}
