package cam72cam.immersiverailroading.library;

public enum AssemblyStep {
	FRAME,
	WHEELS,
	VALVE_GEAR,
	BOILER,
	SHELL,
	REMAINING,
	;

	public static AssemblyStep[] reverse() {
		AssemblyStep[] ret = new AssemblyStep[values().length];
		for (int i = 0; i < values().length; i++) {
			ret[i] = values()[values().length - 1 - i];
		}
		return ret;
	}
}