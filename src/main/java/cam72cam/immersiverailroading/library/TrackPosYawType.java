package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;

import java.util.Locale;

public enum TrackPosYawType {
	ANGLE_SEGMENTATION,
	ANGLE_SPECIFIED,
//	ANGLE_FREE
	;
	
	@Override
	public String toString() {
	    return TextUtil.translate("track.immersiverailroading:position_yaw." + super.toString().toLowerCase(Locale.ROOT));
	}
}
