package cam72cam.immersiverailroading.proxy;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

public class IRWorldSaveData extends WorldSavedData {
	
	private static final String TOTAL_TICKS_NAME = ImmersiveRailroading.MODID + ".totalTicks";
	private long TOTAL_TICKS = 0;
	
	public IRWorldSaveData() {
		super(TOTAL_TICKS_NAME);
	}
	
	public IRWorldSaveData(String s) {
		super(s);
	}
	
	public static IRWorldSaveData get(World world) {
		MapStorage storage = world.getMapStorage();
		IRWorldSaveData instance = (IRWorldSaveData) storage.getOrLoadData(IRWorldSaveData.class, TOTAL_TICKS_NAME);
		
		if (instance == null) {
			instance = new IRWorldSaveData();
			storage.setData(TOTAL_TICKS_NAME, instance);
		}
		return instance;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		TOTAL_TICKS = nbt.getLong(TOTAL_TICKS_NAME);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setLong(TOTAL_TICKS_NAME, TOTAL_TICKS);
		return null;
	}
	
	public void incrementTick() {
		TOTAL_TICKS++;
	}
	
	public long getTotalTicks() {
		return TOTAL_TICKS;
	}
}
