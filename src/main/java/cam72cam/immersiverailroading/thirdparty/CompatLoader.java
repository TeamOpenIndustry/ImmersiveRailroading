package cam72cam.immersiverailroading.thirdparty;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
	
	public static void setup() {
		invokeStatic("opencomputers", "cam72cam.immersiverailroading.thirdparty.opencomputers.Compat", "init");
		invokeStatic("immersiveengineering", "cam72cam.immersiverailroading.thirdparty.ImmersiveEngineering", "init");
		invokeStatic("computercraft", "cam72cam.immersiverailroading.thirdparty.ComputerCraft", "init");
	}

	public static void init() {
		invokeStatic("igwmod", "cam72cam.immersiverailroading.thirdparty.IGWMod", "init");
	}

    public static boolean openWiki() {
		if (Loader.isModLoaded("igwmod")) {
			// This is lousy code...
			try {
				Class<?> cls = Class.forName("igwmod.gui.GuiWiki");
				Object wiki = cls.newInstance();
				FMLCommonHandler.instance().showGuiScreen(wiki);
				Method scf = cls.getMethod("setCurrentFile", String.class, Object[].class);
				scf.invoke(wiki, "immersiverailroading:home", new Object[]{});
				return true;
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return false;
    }
}
