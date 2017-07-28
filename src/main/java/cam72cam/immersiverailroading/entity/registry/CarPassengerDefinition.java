package cam72cam.immersiverailroading.entity.registry;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.CarPassenger;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CarPassengerDefinition extends EntityRollingStockDefinition {

	public CarPassengerDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
	}
	
	@Override
	public EntityRollingStock spawn(World world, BlockPos pos, EnumFacing facing) {
		CarPassenger loco = new CarPassenger(world, defID);

		loco.setPosition(pos.getX(), pos.getY(), pos.getZ());
		loco.prevRotationYaw = facing.getHorizontalAngle();
		loco.rotationYaw = facing.getHorizontalAngle();
		world.spawnEntity(loco);

		return loco;
	}
}
