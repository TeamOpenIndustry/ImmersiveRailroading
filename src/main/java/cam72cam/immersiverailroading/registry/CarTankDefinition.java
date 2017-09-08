package cam72cam.immersiverailroading.registry;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.CarTank;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

public class CarTankDefinition extends EntityRollingStockDefinition {

	private List<Fluid> fluidFilter = null; // null == no filter
	private FluidQuantity capacity;

	public CarTankDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
		JsonObject tank = data.get("tank").getAsJsonObject();
		capacity = FluidQuantity.FromLiters(tank.get("capacity_l").getAsInt());
		if (tank.has("whitelist")) {
			fluidFilter = new ArrayList<Fluid>();
			for(JsonElement allowed : tank.get("whitelist").getAsJsonArray()) {
				Fluid allowedFluid = FluidRegistry.getFluid(allowed.getAsString());
				if (allowedFluid == null) {
					ImmersiveRailroading.logger.warn("Skipping unknown whitelisted fluid: " + allowed.getAsString());
					continue;
				}
				fluidFilter.add(allowedFluid);
			}
		}
	}
	
	@Override
	public EntityRollingStock spawn(World world, BlockPos pos, EnumFacing facing) {
		CarTank loco = new CarTank(world, defID);

		loco.setPosition(pos.getX(), pos.getY(), pos.getZ());
		loco.prevRotationYaw = facing.getHorizontalAngle();
		loco.rotationYaw = facing.getHorizontalAngle();
		world.spawnEntity(loco);

		return loco;
	}

	public FluidQuantity getTankCapaity() {
		return this.capacity;
	}

	public List<Fluid> getFluidFilter() {
		return this.fluidFilter;
	}
}
