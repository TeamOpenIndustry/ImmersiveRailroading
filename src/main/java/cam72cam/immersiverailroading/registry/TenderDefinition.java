package cam72cam.immersiverailroading.registry;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.Tender;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TenderDefinition extends CarTankDefinition {
	private int numSlots;
	private int width;

	public TenderDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
		JsonObject tender = data.get("tender").getAsJsonObject();
		this.numSlots = tender.get("slots").getAsInt();
		this.width = tender.get("width").getAsInt();
	}
	
	@Override
	public EntityRollingStock spawn(World world, BlockPos pos, EnumFacing facing) {
		Tender loco = new Tender(world, defID);

		loco.setPosition(pos.getX(), pos.getY(), pos.getZ());
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
