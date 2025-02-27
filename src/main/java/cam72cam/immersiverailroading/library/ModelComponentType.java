package cam72cam.immersiverailroading.library;

public enum ModelComponentType {
	// STANDARD
	BOGEY_POS_WHEEL_X("BOGEY_#POS#_WHEEL_#ID#"),
	BOGEY_POS("BOGEY_#POS#"),
	BOGEY_FRONT_WHEEL_X("BOGEY_FRONT_WHEEL_#ID#"),
	BOGEY_FRONT("BOGEY_FRONT"),
	BOGEY_REAR_WHEEL_X("BOGEY_REAR_WHEEL_#ID#"),
	BOGEY_REAR("BOGEY_REAR"),
	FRAME("FRAME"),
	FRAME_WHEEL_X("FRAME_WHEEL_#ID#"),
	
	SHELL("SHELL"),

	// LOCOMOTIVE
	CAB("CAB"),
	BELL("BELL"),
	WHISTLE("WHISTLE"),
	HORN("HORN"),
	
	// DIESEL
	FUEL_TANK("FUEL_TANK"),
	ALTERNATOR("ALTERNATOR"),
	ENGINE_BLOCK("ENGINE_BLOCK"),
	CRANKSHAFT("CRANKSHAFT"),
	PISTON_X("PISTON_#ID#"),
	FAN_X("FAN_#ID#"),
	DRIVE_SHAFT_X("DRIVE_SHAFT_#ID#"),
	GEARBOX("GEARBOX"),
	FLUID_COUPLING("FLUID_COUPLING"),
	FINAL_DRIVE("FINAL_DRIVE"),
	TORQUE_CONVERTER("TORQUE_CONVERTER"),

	
	//STEAM
	FIREBOX("FIREBOX"),
	SMOKEBOX("SMOKEBOX"),
	STEAM_CHEST("STEAM_CHEST"),
	STEAM_CHEST_POS("STEAM_CHEST_#POS#"),
	BOILER_SEGMENT_X("BOILER_SEG[E]*MENT_#ID#"),
	PIPING("PIPING"),
	
	
	// WALCHERTS
	WHEEL_DRIVER_X("WHEEL_DRIVER_#ID#"),
	WHEEL_DRIVER_POS_X("WHEEL_DRIVER_#POS#_#ID#"), // MALLET

	CYLINDER_SIDE("CYLINDER_#SIDE#"),
	SIDE_ROD_SIDE("(DRIVE|CONNECTING|SIDE)_ROD_#SIDE#"),
	MAIN_ROD_SIDE("(DRIVING|MAIN)_ROD_#SIDE#"),
	PISTON_ROD_SIDE("PISTON_ROD_#SIDE#"),
	
	UNION_LINK_SIDE("(UNION_LINK|CROSS_HEAD)_#SIDE#"),
	COMBINATION_LEVER_SIDE("COMBINATION_LEVER_#SIDE#"),
	VALVE_STEM_SIDE("VALVE_STEM_#SIDE#"),
	RADIUS_BAR_SIDE("RADIUS_(ROD|BAR)_#SIDE#"),
	EXPANSION_LINK_SIDE("(EXPANSION|SLOTTED)_LINK_#SIDE#"),
	ECCENTRIC_ROD_SIDE("(ECCENTRIC|RETURN_CRANK)_ROD_#SIDE#"),
	ECCENTRIC_CRANK_SIDE("(ECCENTRIC|RETURN)_CRANK_#SIDE#"),
	REVERSING_ARM_SIDE("REVERSING_ARM_#SIDE#"),
	LIFTING_LINK_SIDE("LIFTING_LINK_#SIDE#"),
	REACH_ROD_SIDE("REACH_ROD_#SIDE#"),
	VALVE_PART_SIDE_ID("VALVE_(PART|COMPONENT|ROD|GEAR)_#SIDE#_#ID#"),

	
	// MALLET
	FRONT_FRAME("FRONT_(LOCOMOTIVE|FRAME)"),
	REAR_FRAME("REAR_(LOCOMOTIVE|FRAME)"),
	FRONT_SHELL("FRONT_SHELL"),
	REAR_SHELL("REAR_SHELL"),

	
	// PARTICLES
	PARTICLE_CHIMNEY_X("CHIM[I]*NEY_#ID#", false),
	PRESSURE_VALVE_X("PRESSURE_VALVE_#ID#", false),
	DIESEL_EXHAUST_X("EXHAUST_#ID#", false),
	CYLINDER_DRAIN_SIDE("(CYLINDER|DRAIN)_(COCK|EXHAUST)_#SIDE#", false),

	// Cargo
	CARGO_FILL_X("CARGO_FILL_#ID#", false),
	CARGO_FILL_POS_X("CARGO_FILL_#POS#_#ID#", false),
	CARGO_ITEMS_X("CARGO_ITEMS_#ID#", false),
    CARGO_UNLOAD_X("CARGO_UNLOAD_#ID#", false),

	// Lights
	HEADLIGHT_X("HEADLIGHT_#ID#", false),

	// Controls
	THROTTLE_X("THROTTLE_#ID#"),
	REVERSER_X("REVERSER_#ID#"),
	TRAIN_BRAKE_X("TRAIN_BRAKE_#ID#"),
	INDEPENDENT_BRAKE_X("(INDEPENDENT|IND)_BRAKE_#ID#"),
	THROTTLE_BRAKE_X("THROTTLE_BRAKE_#ID#"),
	DOOR_X("DOOR_#ID#"),
	SEAT_X("SEAT_#ID#"),
	WINDOW_X("WINDOW_#ID#"),
	WIDGET_X("WIDGET_#ID#"),
	BELL_CONTROL_X("BELL_CONTROL_#ID#"),
	WHISTLE_CONTROL_X("WHISTLE_CONTROL_#ID#"),
	HORN_CONTROL_X("HORN_CONTROL_#ID#"),
	ENGINE_START_X("ENGINE_START_#ID#"),
	COUPLER_ENGAGED_X("COUPLER_ENGAGED_#ID#"),
	CYLINDER_DRAIN_CONTROL_X("(CYLINDER|DRAIN)_(COCK|EXHAUST)_CONTROL_#ID#"),

	// Gauges
	GAUGE_LIQUID_X("GAUGE_LIQUID_#ID#"),
	GAUGE_SPEED_X("GAUGE_SPEED_#ID#"),
	GAUGE_TEMPERATURE_X("GAUGE_TEMPERATURE_#ID#"),
	GAUGE_BOILER_PRESSURE_X("GAUGE_BOILER_PRESSURE_#ID#"),
	GAUGE_THROTTLE_X("GAUGE_THROTTLE_#ID#"),
	GAUGE_REVERSER_X("GAUGE_REVERSER_#ID#"),
	GAUGE_TRAIN_BRAKE_X("GAUGE_TRAIN_BRAKE_#ID#"),
	GAUGE_INDEPENDENT_BRAKE_X("GAUGE_(INDEPENDENT|IND)_BRAKE_#ID#"),
	BRAKE_PRESSURE_X("BRAKE_PRESSURE_#ID#"),
	COUPLED_X("COUPLED_#ID#"),

    //Multiblocks
    FLUID_HANDLER("FLUID_HANDLER_#ID#"),
    ITEM_OUTPUT("ITEM_OUTPUT"),

	// REST
	IMMERSIVERAILROADING_BASE_COMPONENT("IMMERSIVERAILROADING_BASE_COMPNOENT"),
	REMAINING(""),
	;

    public final String regex;
	public final boolean collisionsEnabled;
	
	ModelComponentType(String regex) {
		this(regex, true);
	}
	ModelComponentType(String regex, boolean collide) {
		this.regex = ".*" + regex + ".*";
		this.collisionsEnabled = collide;
	}

	public static boolean shouldRender(String group) {
		return group.contains("CHIMNEY_")
                || group.contains("CHIMINEY_")
                || group.contains("PRESSURE_VALVE_")
                || group.contains("EXHAUST_")
                || group.contains("CARGO_ITEMS")
                || group.contains("CARGO_UNLOAD")
                || group.contains("FLUID_HANDLER")
                || group.contains("ITEM_OUTPUT");
	}

    public static class ModelPosition {
		private static final ModelPosition INNER = new ModelPosition("INNER");
		public static final ModelPosition LEFT = new ModelPosition("LEFT");
		public static final ModelPosition INNER_LEFT = INNER.and(LEFT);
		public static final ModelPosition CENTER = new ModelPosition("CENTER");
		public static final ModelPosition RIGHT = new ModelPosition("RIGHT");
		public static final ModelPosition INNER_RIGHT = INNER.and(RIGHT);

		private static final ModelPosition BOGEY = new ModelPosition("BOGEY");
		private static final ModelPosition LOCOMOTIVE = new ModelPosition("LOCOMOTIVE");
		public static final ModelPosition FRONT = new ModelPosition("FRONT");
		public static final ModelPosition REAR = new ModelPosition("REAR");
		public static final ModelPosition BOGEY_FRONT = BOGEY.and(FRONT);
		public static final ModelPosition BOGEY_REAR = BOGEY.and(REAR);
		public static final ModelPosition FRONT_LOCOMOTIVE = FRONT.and(LOCOMOTIVE);
		public static final ModelPosition REAR_LOCOMOTIVE = REAR.and(LOCOMOTIVE);

		public static final ModelPosition A = new ModelPosition("A");
		public static final ModelPosition B = new ModelPosition("B");

		private final String pos;

		public ModelPosition(String pos) {
			this.pos = pos;
		}

		public ModelPosition and(ModelPosition other) {
			return other == null ? this : new ModelPosition(this.pos + "_" + other.pos);
		}

		public boolean contains(ModelPosition other) {
			return pos.contains(other.pos);
		}

		@Override
		public String toString() {
			return pos;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof ModelPosition && pos.equals(((ModelPosition) o).pos);
		}

		@Override
		public int hashCode() {
			return pos.hashCode();
		}
	}
}
