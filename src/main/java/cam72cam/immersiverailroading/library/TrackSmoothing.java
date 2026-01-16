package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;

import java.util.Locale;

public enum TrackSmoothing {
	BOTH,
	NEAR,
	FAR,
	NEITHER,
	PITCH_LOCKED_CUBIC;

	@Override
	public String toString() {
	    return TextUtil.translate("track.immersiverailroading:smoothing." + super.toString().toLowerCase(Locale.ROOT));
	}
}
