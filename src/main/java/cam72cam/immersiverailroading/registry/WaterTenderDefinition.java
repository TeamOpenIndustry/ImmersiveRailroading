package cam72cam.immersiverailroading.registry;

import java.util.List;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.WaterTender;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class WaterTenderDefinition extends CarTankDefinition {

	public WaterTenderDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
	}
	
	@Override
	public void parseJson(JsonObject data) throws Exception {
		super.parseJson(data);
		
		JsonObject tender = data.get("watertender").getAsJsonObject();
	}
	
	@Override
	public List<String> getTooltip(Gauge gauge) {
		List<String> tips = super.getTooltip(gauge);
		return tips;
	}
	
	@Override
	public EntityRollingStock instance(World world) {
		return new WaterTender(world, defID);
	}
}
