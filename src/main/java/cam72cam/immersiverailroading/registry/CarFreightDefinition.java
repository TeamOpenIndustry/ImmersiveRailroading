package cam72cam.immersiverailroading.registry;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.entity.CarFreight;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class CarFreightDefinition extends EntityRollingStockDefinition {

	private int numSlots;
	private int width;
	private List<String> validCargo;
	private boolean showCurrentLoadOnly;
	
	public CarFreightDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
		
		// Handle null data
		if (validCargo == null) {
			validCargo = new ArrayList<String>();
		}
	}
	
	@Override
	public void parseJson(JsonObject data) throws Exception {
		super.parseJson(data);
		JsonObject freight = data.get("freight").getAsJsonObject();
		this.numSlots = (int)Math.ceil(freight.get("slots").getAsInt() * internal_inv_scale);
		this.width = (int)Math.ceil(freight.get("width").getAsInt() * internal_inv_scale);
		this.validCargo = new ArrayList<String>();
		for (JsonElement el : freight.get("cargo").getAsJsonArray()) {
			validCargo.add(el.getAsString());
		}
		this.showCurrentLoadOnly = false;
		if (freight.has("show_current_load_only")) {
			this.showCurrentLoadOnly = freight.get("show_current_load_only").getAsBoolean();
		}
	}
	
	@Override
	public List<String> getTooltip(Gauge gauge) {
		List<String> tips = super.getTooltip(gauge);
		tips.add(GuiText.FREIGHT_CAPACITY_TOOLTIP.toString(this.getInventorySize(gauge)));
		return tips;
	}
	
	@Override
	public EntityRollingStock instance(World world) {
		return new CarFreight(world, defID);
	}
	
	public int getInventorySize(Gauge gauge) {
		return MathHelper.ceil(numSlots * gauge.scale());
	}

	public int getInventoryWidth(Gauge gauge) {
		return MathHelper.ceil(width * gauge.scale());
	}
	
	public boolean shouldShowCurrentLoadOnly () {
		return this.showCurrentLoadOnly;
	}
	
	@Override
	public boolean acceptsLivestock() {
		return true;
	}	
}
