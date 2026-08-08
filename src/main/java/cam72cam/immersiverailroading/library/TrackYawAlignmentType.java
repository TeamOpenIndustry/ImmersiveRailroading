package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;

import java.util.Locale;

public enum TrackYawAlignmentType {
	ANGLE_SEGMENTATION,
	ANGLE_SPECIFIED,
	;
	
	@Override
	public String toString() {
	    return TextUtil.translate("track.immersiverailroading:yaw_alignment." + super.toString().toLowerCase(Locale.ROOT));
	}
}
