package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;

import java.util.Locale;

public enum TrackPositionType {
	FIXED,
	PIXELS,
	PIXELS_LOCKED,
	SMOOTH,
	SMOOTH_LOCKED,
	;
	//TODO:
	// new value:
	//     snapping_direction(which allows going around the nearest track path),
	//     snapping_pos(which locks xyz of nearest point),
	//     snapping_all(lock pos and direction to nearest point)
	//   and for pixel we can have an config option to make (or store it in item and config in gui would be better):
	//     pixels2,pixels4,pixels8,pixels32,pixels64, ...like LittleTiles
	//   TrackGui:add a button besides posType，to determine pixel level/snapping type mentioned above
	// others：
	//   make a gui to display pos and yaw when holding itemTrackBlueprint
	//   for superelevation, first we need a method fits segmentation, and we need a option to determine whether to offset height with superelevation
	//   do we need to implement free interpolation? id so what method should we use? bezier?
	
	@Override
	public String toString() {
	    return TextUtil.translate("track.immersiverailroading:position." + super.toString().toLowerCase(Locale.ROOT));
	}
}
