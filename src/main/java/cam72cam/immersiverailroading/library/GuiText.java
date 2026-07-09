package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;

public enum GuiText {
	LABEL_BRAKE("label.brake"),
	LABEL_THROTTLE("label.throttle"),
	LABEL_REVERSER("label.reverser"),
	SLIDER_ZOOM("slider.zoom"), //Need colon
	SELECTOR_PAGE("selector.page"),
	SELECTOR_TYPE("selector.type"),
	SELECTOR_QUARTERS("selector.quarters"),
	SELECTOR_CURVOSITY("selector.curvosity"),
	SELECTOR_RAIL_BED("selector.rail_bed"),
	SELECTOR_RAIL_BED_FILL("selector.rail_bed_fill"),
	SELECTOR_POSITION("selector.position"),
	SELECTOR_SMOOTHING("selector.smoothing"),
	SELECTOR_DIRECTION("selector.direction"),
	SELECTOR_PLACE_BLUEPRINT("selector.place_blueprint"),
	SELECTOR_GRADE_CROSSING("selector.grade_crossing"),
	SELECTOR_GAUGE("selector.gauge"),
	SELECTOR_TRACK("selector.track"),
	SELECTOR_TRANSFER_TABLE_ENTRY_COUNT("selector.transfer_table_entry_count"),
	SELECTOR_TRANSFER_TABLE_ENTRY_SPACING("selector.transfer_table_entry_spacing"),
	SELECTOR_TILE_TILT("selector.tile_tilt"),
	SELECTOR_ROLL_EFFECT_TILE("selector.roll_effect_tile"),
	SELECTOR_DEGREE_MODE("selector.degree_mode"),
	SELECTOR_OFFSET_VERT_BY_NORMAL_MODE("selector.offset_vert_by_normal_mode"),
	SELECTOR_AUGMENT_DETECT("selector.augment.detect"),
	SELECTOR_AUGMENT_REDSTONE("selector.augment.redstone"),
	SELECTOR_AUGMENT_PUSHPULL("selector.augment.pushpull"),
	SELECTOR_AUGMENT_COUPLER("selector.augment.coupler"),
	SELECTOR_AUGMENT_CONTROL("selector.augment.control"),
	SELECTOR_PLATE_TYPE("selector.plate_type"),
	SELECTOR_PLATE_BOILER("selector.plate_boiler"),
	SELECTOR_CAST_SINGLE("selector.cast_single"),
	SELECTOR_CAST_REPEAT("selector.cast_repeat"),
	SELECTOR_PAINTBRUSH_RANDOM("selector.paintbrush_random"),
	SELECTOR_PAINTBRUSH_TO_STOCK("selector.paintbrush_apply_to_stock"),
	SELECTOR_PAINTBRUSH_TO_TRAIN("selector.paintbrush_apply_to_train"),

	TRACK_TYPE("track.type"),
	TRACK_LENGTH("track.length"),
	TRACK_GAUGE("track.gauge"),
	TRACK_QUARTERS("track.quarters"),
	TRACK_CURVOSITY("track.curvosity"),
	TRACK_RAIL_BED("track.rail_bed"),
	TRACK_RAIL_BED_FILL("track.rail_bed_fill"),
	TRACK_POSITION("track.position"),
	TRACK_SMOOTHING("track.smoothing"),
	TRACK_DIRECTION("track.direction"),
	TRACK_PLACE_BLUEPRINT_TRUE("track.place_blueprint_true"),
	TRACK_PLACE_BLUEPRINT_FALSE("track.place_blueprint_false"),

	TRACK_EXTRA_ARC_LEN_FACTOR("track.arc_len_factor"),
	TRACK_EXTRA_ROLL_OFFSET_TYPE("track.roll_offset_type"),
	TRACK_EXTRA_INSERT_POINT("track.extra_insert_point"),
	TRACK_EXTRA_DELETE_POINT("track.extra_delete_point"),
	TRACK_EXTRA_POINT_VALUE_M("track.extra_point_value_m"),
	TRACK_EXTRA_POINT_VALUE_CM_DEGREE("track.extra_point_value_cm_degree"),
	TRACK_EXTRA_POINT_SLOPE("track.extra_point_slope"),
	TRACK_EXTRA_POINT_WEIGHT("track.extra_point_weight"),
	TRACK_EXTRA_RESET("track.extra_reset"),
	TRACK_EXTRA_EDIT_LEFT("track.extra_edit_left"),
	TRACK_EXTRA_EDIT_RIGHT("track.extra_edit_right"),

	TRACK_EXTRA_TO_MAIN("track.extra_trackgui"),
	TRACK_MAIN_TO_EXTRA("track.extragui"),
	
	LOCO_WORKS("loco.works"),
//	LOCO_HORSE_POWER("loco.horse_power"),
	LOCO_POWER("loco.power"),
	LOCO_TRACTION("loco.tractive_effort"),
	LOCO_MAX_SPEED("loco.max_speed"),
	GAUGE_TOOLTIP("stock.gauge"),
	RAW_CAST_TOOLTIP("cast.raw"),
	TANK_CAPACITY_TOOLTIP("stock.tank_capacity"),
	FREIGHT_CAPACITY_TOOLTIP("stock.freight_capacity"),
	WEIGHT_TOOLTIP("stock.weight"),
	TEXTURE_TOOLTIP("stock.texture"),
	SWITCH_KEY_TOOLTIP("item.switch_key"),
	SWITCH_KEY_DATA_TOOLTIP("item.switch_key.data"),
	RADIO_CARD_LINKED_TO("item.radio_card.linked_to"),
	RADIO_CARD_NOT_LINKED("item.radio_card.not_linked"),
	MODELER_TOOLTIP("stock.modeler"),
	PACK_TOOLTIP("stock.pack"),
	TAG_TOOLTIP("stock.tag"),
	TRACK_SWITCHER_TOOLTIP("item.track_exchanger"),
	PAINT_BRUSH_MODE_TOOLTIP("item.paint_brush.mode"),
	PAINT_BRUSH_DESCRIPTION_TOOLTIP("item.paint_brush.description"),

	LABEL_CURRENT_AUGMENT("label.current_augment"),
	LABEL_ACTUATOR_FILTER("label.actuator_filter"),
	LABEL_INCLUDED_TAG("label.included_tag"),
	LABEL_EXCLUDED_TAG("label.excluded_tag"),

	ON("overlay.on"),
	OFF("overlay.off"),
	NONE("none"),
	;

	private final String value;
  
	GuiText(String value) {
		this.value = value;
	}
	
	public String getRaw() {
		return "gui.immersiverailroading:" + value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return TextUtil.translate(getRaw());
	}
	
	public String toString(Object...objects) {
		return TextUtil.translate(getRaw(), objects);
	}
}
