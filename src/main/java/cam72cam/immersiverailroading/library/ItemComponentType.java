package cam72cam.immersiverailroading.library;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.model.RenderComponent;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.util.ItemCastingCost;
import cam72cam.immersiverailroading.util.TextUtil;

public enum ItemComponentType {
	FRAME(AssemblyStep.FRAME, CraftingType.CASTING, RenderComponentType.FRAME), //TODO
	
	// MALLET
	FRONT_FRAME(AssemblyStep.FRAME, CraftingType.CASTING, RenderComponentType.FRONT_LOCOMOTIVE),
	
	// STANDARD
	BOGEY_WHEEL(AssemblyStep.WHEELS, CraftingType.CASTING, RenderComponentType.BOGEY_POS_WHEEL_X),
	BOGEY(AssemblyStep.WHEELS, CraftingType.CASTING, RenderComponentType.BOGEY_POS),
	BOGEY_FRONT_WHEEL(AssemblyStep.WHEELS, CraftingType.CASTING, RenderComponentType.BOGEY_FRONT_WHEEL_X),
	BOGEY_FRONT(AssemblyStep.WHEELS, CraftingType.CASTING, RenderComponentType.BOGEY_FRONT),
	BOGEY_REAR_WHEEL(AssemblyStep.WHEELS, CraftingType.CASTING, RenderComponentType.BOGEY_REAR_WHEEL_X),
	BOGEY_REAR(AssemblyStep.WHEELS, CraftingType.CASTING, RenderComponentType.BOGEY_REAR),
	FRAME_WHEEL(AssemblyStep.WHEELS, CraftingType.CASTING, RenderComponentType.FRAME_WHEEL_X),
	
	SHELL(AssemblyStep.SHELL, CraftingType.PLATE_LARGE, RenderComponentType.SHELL),
	
	// LOCOMOTIVE
	CAB(AssemblyStep.SHELL, CraftingType.PLATE_LARGE, RenderComponentType.CAB),
	BELL(AssemblyStep.SHELL, CraftingType.PLATE_SMALL, RenderComponentType.BELL),
	WHISTLE(AssemblyStep.SHELL, CraftingType.PLATE_SMALL, RenderComponentType.WHISTLE),
	HORN(AssemblyStep.SHELL, CraftingType.PLATE_SMALL, RenderComponentType.HORN),
	
	// DIESEL
	FUEL_TANK(AssemblyStep.SHELL, CraftingType.PLATE_MEDIUM, RenderComponentType.FUEL_TANK),
	ALTERNATOR(AssemblyStep.SHELL, CraftingType.CASTING, RenderComponentType.ALTERNATOR),
	ENGINE_BLOCK(AssemblyStep.SHELL, CraftingType.CASTING, RenderComponentType.ENGINE_BLOCK),
	CRANKSHAFT(AssemblyStep.SHELL, CraftingType.CASTING, RenderComponentType.CRANKSHAFT),
	PISTON(AssemblyStep.SHELL, CraftingType.CASTING_HAMMER, RenderComponentType.PISTON_X),
	FAN(AssemblyStep.SHELL, CraftingType.PLATE_MEDIUM, RenderComponentType.FAN_X),
	DRIVE_SHAFT(AssemblyStep.SHELL, CraftingType.CASTING, RenderComponentType.DRIVE_SHAFT_X),
	GEARBOX(AssemblyStep.SHELL, CraftingType.CASTING, RenderComponentType.GEARBOX),
	FLUID_COUPLING(AssemblyStep.SHELL, CraftingType.PLATE_MEDIUM, RenderComponentType.FLUID_COUPLING),
	FINAL_DRIVE(AssemblyStep.SHELL, CraftingType.PLATE_MEDIUM, RenderComponentType.FINAL_DRIVE),
	TORQUE_CONVERTER(AssemblyStep.SHELL, CraftingType.CASTING, RenderComponentType.TORQUE_CONVERTER),
	
	//STEAM
	FIREBOX(AssemblyStep.BOILER, CraftingType.PLATE_LARGE, RenderComponentType.FIREBOX),
	SMOKEBOX(AssemblyStep.BOILER, CraftingType.PLATE_LARGE, RenderComponentType.SMOKEBOX),
	STEAM_CHEST(AssemblyStep.FRAME, CraftingType.CASTING, RenderComponentType.STEAM_CHEST),
	STEAM_CHEST_FRONT(AssemblyStep.FRAME, CraftingType.CASTING, RenderComponentType.STEAM_CHEST_FRONT),
	STEAM_CHEST_REAR(AssemblyStep.FRAME, CraftingType.CASTING, RenderComponentType.STEAM_CHEST_REAR),
	BOILER_SEGMENT(AssemblyStep.BOILER, CraftingType.PLATE_BOILER, RenderComponentType.BOILER_SEGMENT_X),
	PIPING(AssemblyStep.BOILER, CraftingType.PLATE_LARGE, RenderComponentType.PIPING),
	
	// WALCHERTS
	WHEEL_DRIVER(AssemblyStep.WHEELS, CraftingType.CASTING, RenderComponentType.WHEEL_DRIVER_X),
	WHEEL_DRIVER_FRONT(AssemblyStep.WHEELS, CraftingType.CASTING, RenderComponentType.WHEEL_DRIVER_FRONT_X), // MALLET
	WHEEL_DRIVER_REAR(AssemblyStep.WHEELS, CraftingType.CASTING, RenderComponentType.WHEEL_DRIVER_REAR_X), // MALLET
	
	CYLINDER(AssemblyStep.FRAME, CraftingType.CASTING_HAMMER, RenderComponentType.CYLINDER_SIDE),
	SIDE_ROD(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, RenderComponentType.SIDE_ROD_SIDE),
	MAIN_ROD(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, RenderComponentType.MAIN_ROD_SIDE),
	PISTON_ROD(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, RenderComponentType.PISTON_ROD_SIDE),
	
	UNION_LINK(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, RenderComponentType.UNION_LINK_SIDE),
	COMBINATION_LEVER(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, RenderComponentType.COMBINATION_LEVER_SIDE),
	VALVE_STEM(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, RenderComponentType.VALVE_STEM_SIDE),
	RADIUS_BAR(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, RenderComponentType.RADIUS_BAR_SIDE),
	EXPANSION_LINK(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, RenderComponentType.EXPANSION_LINK_SIDE),
	ECCENTRIC_ROD(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, RenderComponentType.ECCENTRIC_ROD_SIDE),
	ECCENTRIC_CRANK(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, RenderComponentType.ECCENTRIC_CRANK_SIDE),
	REVERSING_ARM(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, RenderComponentType.REVERSING_ARM_SIDE),
	LIFTING_LINK(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, RenderComponentType.LIFTING_LINK_SIDE),
	REACH_ROD(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, RenderComponentType.REACH_ROD_SIDE),
	
	// LEGACY, how do we depricate this??
	WALCHERTS_LINKAGE(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, 
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
	public final CraftingType crafting;

	ItemComponentType(AssemblyStep step, CraftingType crafting, RenderComponentType ... render) {
		this.crafting = crafting;
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

	@Override
	public String toString() {
		return TextUtil.translate("part.immersiverailroading:component." + super.toString().toLowerCase());
	}

	public int getPlateCost(Gauge gauge, EntityRollingStockDefinition definition) {
		RenderComponent comp = definition.getComponent(this.render.get(0), gauge);
		
		double mult = 0;
		switch(this.crafting) {
		case PLATE_LARGE:
			mult = 0.25;
			break;
		case PLATE_MEDIUM:
			mult = 0.5;
			break;
		case PLATE_SMALL:
			mult = 1;
			break;
		default:
			return 0;
		}
		
		// Approximate
		double size = comp.width() * comp.height() * 2 + comp.length() * comp.height() * 2 + comp.width() * comp.height() * 2;
		size /= 4;
		
		return (int) Math.ceil(size * mult);
	}

	public PlateType getPlateType() {
		switch (this.crafting) {
		case PLATE_LARGE:
			return PlateType.LARGE;
		case PLATE_MEDIUM:
			return PlateType.MEDIUM;
		case PLATE_SMALL:
			return PlateType.SMALL;
		default:
			return null;
		}
	}

	public int getCastCost(EntityRollingStockDefinition definition, Gauge gauge) {
		if (definition == null) {
			return ItemCastingCost.BAD_CAST_COST;
		}
		RenderComponent comp = definition.getComponent(this.render.get(0), gauge);
		double densityGues = 0.6;
		return (int) Math.ceil(comp.width() * comp.height() * comp.length() * densityGues);
	}

	public int getWoodCost(Gauge gauge, EntityRollingStockDefinition definition) {
		RenderComponent comp = definition.getComponent(this.render.get(0), gauge);
		double densityGues = 4;
		return (int) Math.ceil(comp.width() * comp.height() * comp.length() * densityGues);
	}
	
	public boolean isWooden(EntityRollingStockDefinition definition) {
		RenderComponent component = definition.getComponent(this.render.get(0), Gauge.from(Gauge.STANDARD));
		return component.isWooden();
	}
}
