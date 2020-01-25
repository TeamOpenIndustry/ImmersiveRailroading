package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;

public enum TrackSmoothing {
	BOTH,
	NEAR,
	FAR,
	NEITHER;

	@Override
	public String toString() {
	    return TextUtil.translate("track.immersiverailroading:smoothing." + super.toString().toLowerCase());
	}
}
