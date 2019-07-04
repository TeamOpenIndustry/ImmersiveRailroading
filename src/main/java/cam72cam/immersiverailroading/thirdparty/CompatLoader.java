package cam72cam.immersiverailroading.thirdparty;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraftforge.fml.common.Loader;

public class CompatLoader {
	public static Object invokeStatic(String modID, String cname, String method, Object ...objects) {
		if (Loader.isModLoaded(modID)) {
			try {
				Class<?> cls = Class.forName(cname);
				return cls.getMethod(method).invoke(null, objects);
			} catch (Exception ex) {
				ImmersiveRailroading.catching(ex);
			}
		}
		return null;
	}
	
	public static void load() {
		invokeStatic("opencomputers", "cam72cam.immersiverailroading.thirdparty.opencomputers.Compat", "init");
		invokeStatic("immersiveengineering", "cam72cam.immersiverailroading.thirdparty.ImmersiveEngineering", "init");
		invokeStatic("computercraft", "cam72cam.immersiverailroading.thirdparty.ComputerCraft", "init");
	}
}
