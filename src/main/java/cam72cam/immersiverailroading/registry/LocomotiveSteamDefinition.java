package cam72cam.immersiverailroading.registry;

import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.library.ValveGearType;
import cam72cam.immersiverailroading.util.FluidQuantity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LocomotiveSteamDefinition extends LocomotiveDefinition {
	private FluidQuantity tankCapacity;
	private int maxPSI;
	private ValveGearType valveGear;
	private int numSlots;
	private int width;
	
	public LocomotiveSteamDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
		JsonObject properties = data.get("properties").getAsJsonObject();
		tankCapacity = FluidQuantity.FromLiters(properties.get("water_capacity_l").getAsInt());
		maxPSI = properties.get("max_psi").getAsInt();
		valveGear = ValveGearType.valueOf(properties.get("valve_gear").getAsString().toUpperCase());
		JsonObject firebox = data.get("firebox").getAsJsonObject();
		this.numSlots = firebox.get("slots").getAsInt();
		this.width = firebox.get("width").getAsInt();
	}

	@Override
	public EntityRollingStock spawn(World world, Vec3d pos, EnumFacing facing) {
		LocomotiveSteam loco = new LocomotiveSteam(world, defID);

		loco.setPosition(pos.x, pos.y, pos.z);
		loco.prevRotationYaw = facing.getHorizontalAngle();
		loco.rotationYaw = facing.getHorizontalAngle();
		world.spawnEntity(loco);

		return loco;
	}

	public FluidQuantity getTankCapacity() {
		return this.tankCapacity;
	}
	
	public int getMaxPSI() {
		return this.maxPSI;
	}
	public ValveGearType getValveGear() {
		return valveGear;
	}
	
	public int getInventorySize() {
		return numSlots;
	}

	public int getInventoryWidth() {
		return width;
	}
}
