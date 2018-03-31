package cam72cam.immersiverailroading.registry;

import java.util.List;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.Tender;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import net.minecraft.util.math.MathHelper;
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
		this.numSlots = (int)Math.ceil(tender.get("slots").getAsInt() * internal_inv_scale);
		this.width = (int)Math.ceil(tender.get("width").getAsInt() * internal_inv_scale);
	}
	
	@Override
	public List<String> getTooltip(Gauge gauge) {
		List<String> tips = super.getTooltip(gauge);
		tips.add(GuiText.FREIGHT_CAPACITY_TOOLTIP.toString(this.getInventorySize(gauge)));
		return tips;
	}
	
	@Override
	public EntityRollingStock instance(World world) {
		return new Tender(world, defID);
	}
	
	public int getInventorySize(Gauge gauge) {
		return MathHelper.ceil(numSlots * gauge.scale());
	}

	public int getInventoryWidth(Gauge gauge) {
		return MathHelper.ceil(width * gauge.scale());
	}
}
