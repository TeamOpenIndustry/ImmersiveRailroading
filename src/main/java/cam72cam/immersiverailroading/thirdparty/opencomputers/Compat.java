package cam72cam.immersiverailroading.thirdparty.opencomputers;

import li.cil.oc.api.API;

public class Compat {
	public static void init() {
		API.driver.add(new AugmentDriver());
	}
}
