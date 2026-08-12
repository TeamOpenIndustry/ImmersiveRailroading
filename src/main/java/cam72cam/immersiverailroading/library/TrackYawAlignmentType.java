package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;

import java.util.Locale;

/**
 * Defines the yaw alignment behavior for track placement.
 * <p>
 * This enum controls how the yaw (horizontal rotation) of a track endpoint is determined.
 * It can either be snapped to predefined angle segments or use a custom specified angle.
 * </p>
 */
public enum TrackYawAlignmentType {
	/**
	 * Yaw will be aligned according to {@code Config.AnglePlacementSegmentation}.
	 * The angle will be rounded to the nearest valid segment.
	 */
	ANGLE_SEGMENTATION,

	/**
	 * Yaw will be aligned according to the value specified in {@code EndPointData.posYaw}.
	 * The exact angle provided by the user will be used.
	 */
	ANGLE_SPECIFIED;

	@Override
	public String toString() {
		return TextUtil.translate("track.immersiverailroading:yaw_alignment." + super.toString().toLowerCase(Locale.ROOT));
	}
}