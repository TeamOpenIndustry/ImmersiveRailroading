package cam72cam.immersiverailroading.library;

import java.util.ArrayList;
import java.util.List;

public enum ItemComponentType {
	FRAME(AssemblyStep.FRAME, RenderComponentType.FRAME), //TODO
	
	// MALLET
	FRONT_FRAME(AssemblyStep.FRAME, RenderComponentType.FRONT_LOCOMOTIVE),
	
	// STANDARD
	BOGEY_FRONT_WHEEL(AssemblyStep.WHEELS, RenderComponentType.BOGEY_FRONT_WHEEL_X),
	BOGEY_FRONT(AssemblyStep.WHEELS, RenderComponentType.BOGEY_FRONT),
	BOGEY_REAR_WHEEL(AssemblyStep.WHEELS, RenderComponentType.BOGEY_REAR_WHEEL_X),
	BOGEY_REAR(AssemblyStep.WHEELS, RenderComponentType.BOGEY_REAR),
	FRAME_WHEEL(AssemblyStep.WHEELS, RenderComponentType.FRAME_WHEEL_X),
	
	SHELL(AssemblyStep.SHELL, RenderComponentType.SHELL),
	
	// LOCOMOTIVE
	CAB(AssemblyStep.SHELL, RenderComponentType.CAB),
	BELL(AssemblyStep.SHELL, RenderComponentType.BELL),
	WHISTLE(AssemblyStep.SHELL, RenderComponentType.WHISTLE),
	HORN(AssemblyStep.SHELL, RenderComponentType.HORN),
	
	// DIESEL
	FUEL_TANK(AssemblyStep.SHELL, RenderComponentType.FUEL_TANK),
	ALTERNATOR(AssemblyStep.SHELL, RenderComponentType.ALTERNATOR),
	ENGINE_BLOCK(AssemblyStep.SHELL, RenderComponentType.ENGINE_BLOCK),
	PISTON(AssemblyStep.SHELL, RenderComponentType.PISTON_X),
	
	//STEAM
	FIREBOX(AssemblyStep.BOILER, RenderComponentType.FIREBOX),
	STEAM_CHEST(AssemblyStep.FRAME, RenderComponentType.STEAM_CHEST),
	BOILER_SEGMENT(AssemblyStep.BOILER, RenderComponentType.BOILER_SEGMENT_X),
	PIPING(AssemblyStep.BOILER, RenderComponentType.PIPING),
	
	// WALCHERTS
	WHEEL_DRIVER(AssemblyStep.WHEELS, RenderComponentType.WHEEL_DRIVER_X),
	WHEEL_DRIVER_FRONT(AssemblyStep.WHEELS, RenderComponentType.WHEEL_DRIVER_FRONT_X), // MALLET
	WHEEL_DRIVER_REAR(AssemblyStep.WHEELS, RenderComponentType.WHEEL_DRIVER_REAR_X), // MALLET
	
	CYLINDER(AssemblyStep.FRAME, RenderComponentType.CYLINDER_SIDE),
	SIDE_ROD(AssemblyStep.VALVE_GEAR, RenderComponentType.SIDE_ROD_SIDE),
	MAIN_ROD(AssemblyStep.VALVE_GEAR, RenderComponentType.MAIN_ROD_SIDE),
	PISTON_ROD(AssemblyStep.VALVE_GEAR, RenderComponentType.PISTON_ROD_SIDE),
	
	UNION_LINK(AssemblyStep.VALVE_GEAR, RenderComponentType.UNION_LINK_SIDE),
	COMBINATION_LEVER(AssemblyStep.VALVE_GEAR, RenderComponentType.COMBINATION_LEVER_SIDE),
	VALVE_STEM(AssemblyStep.VALVE_GEAR, RenderComponentType.VALVE_STEM_SIDE),
	RADIUS_BAR(AssemblyStep.VALVE_GEAR, RenderComponentType.RADIUS_BAR_SIDE),
	EXPANSION_LINK(AssemblyStep.VALVE_GEAR, RenderComponentType.EXPANSION_LINK_SIDE),
	ECCENTRIC_ROD(AssemblyStep.VALVE_GEAR, RenderComponentType.ECCENTRIC_ROD_SIDE),
	ECCENTRIC_CRANK(AssemblyStep.VALVE_GEAR, RenderComponentType.ECCENTRIC_CRANK_SIDE),
	REVERSING_ARM(AssemblyStep.VALVE_GEAR, RenderComponentType.REVERSING_ARM_SIDE),
	LIFTING_LINK(AssemblyStep.VALVE_GEAR, RenderComponentType.LIFTING_LINK_SIDE),
	REACH_ROD(AssemblyStep.VALVE_GEAR, RenderComponentType.REACH_ROD_SIDE),
	
	WALCHERTS_LINKAGE(AssemblyStep.VALVE_GEAR, 
			RenderComponentType.UNION_LINK_SIDE,
			RenderComponentType.COMBINATION_LEVER_SIDE,
			RenderComponentType.VALVE_STEM_SIDE,
			RenderComponentType.RADIUS_BAR_SIDE,
			RenderComponentType.EXPANSION_LINK_SIDE,
			RenderComponentType.ECCENTRIC_ROD_SIDE,
			RenderComponentType.ECCENTRIC_CRANK_SIDE,
			RenderComponentType.REVERSING_ARM_SIDE,
			RenderComponentType.LIFTING_LINK_SIDE,
			RenderComponentType.REACH_ROD_SIDE
	),
	;
	
	public final AssemblyStep step;
	public final List<RenderComponentType> render;

	ItemComponentType(AssemblyStep step, RenderComponentType ... render) {
		this.step = step;
		this.render = new ArrayList<RenderComponentType>();
		
		for (RenderComponentType r : render) {
			this.render.add(r);
		}
	}
	
	public boolean isWheelPart() {
		switch (this) {
		case BOGEY_FRONT_WHEEL:
		case BOGEY_FRONT:
		case BOGEY_REAR_WHEEL:
		case BOGEY_REAR:
		case WHEEL_DRIVER:
		case WHEEL_DRIVER_FRONT:
		case WHEEL_DRIVER_REAR:
		case FRAME_WHEEL:
			return true;
		default:
			return false;
		}
	}

	public static ItemComponentType from(RenderComponentType renderComponent) {
		for (ItemComponentType item : values()) {
			for (RenderComponentType render : item.render) {
				if (render == renderComponent) {
					return item;
				}
			}
		}
		
		return null;
	}

	public String prettyString() {
		String result = "";
		String join = "";
		String[] parts = this.toString().split("_");
		for (String part : parts) {
			result += join + part.substring(0, 1) + part.substring(1, part.length()).toLowerCase(); 
			join = " ";
		}
		return result;
	}
}
