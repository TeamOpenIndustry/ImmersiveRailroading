package cam72cam.immersiverailroading.util;

import cam72cam.mod.resource.Identifier;

import java.io.IOException;

public class FileUtil {
	public static Identifier loadOrDefault(Identifier identifier, Identifier fallback){
		try {
			identifier.getResourceStream();
			return identifier;
		} catch (IOException e){
			return fallback;
		}
	}

	public static boolean canLoad(Identifier identifier) {
		try {
			identifier.getResourceStream();
			return true;
		} catch (IOException e){
			return false;
		}
	}
}
