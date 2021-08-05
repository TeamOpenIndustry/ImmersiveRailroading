package cam72cam.immersiverailroading.library;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.util.ItemCastingCost;
import cam72cam.mod.text.TextUtil;

public enum ItemComponentType {
	FRAME(AssemblyStep.FRAME, CraftingType.CASTING, ModelComponentType.FRAME),
	
	// MALLET
	FRONT_FRAME(AssemblyStep.FRAME, CraftingType.CASTING, ModelComponentType.FRONT_FRAME),
	REAR_FRAME(AssemblyStep.FRAME, CraftingType.CASTING, ModelComponentType.REAR_FRAME),

	// STANDARD
	BOGEY_WHEEL(AssemblyStep.WHEELS, CraftingType.CASTING, ModelComponentType.BOGEY_POS_WHEEL_X),
	BOGEY(AssemblyStep.WHEELS, CraftingType.CASTING, ModelComponentType.BOGEY_POS),
	BOGEY_FRONT_WHEEL(AssemblyStep.WHEELS, CraftingType.CASTING, ModelComponentType.BOGEY_FRONT_WHEEL_X),
	BOGEY_FRONT(AssemblyStep.WHEELS, CraftingType.CASTING, ModelComponentType.BOGEY_FRONT),
	BOGEY_REAR_WHEEL(AssemblyStep.WHEELS, CraftingType.CASTING, ModelComponentType.BOGEY_REAR_WHEEL_X),
	BOGEY_REAR(AssemblyStep.WHEELS, CraftingType.CASTING, ModelComponentType.BOGEY_REAR),
	FRAME_WHEEL(AssemblyStep.WHEELS, CraftingType.CASTING, ModelComponentType.FRAME_WHEEL_X),
	
	SHELL(AssemblyStep.SHELL, CraftingType.PLATE_LARGE, ModelComponentType.SHELL),
	
	// LOCOMOTIVE
	CAB(AssemblyStep.SHELL, CraftingType.PLATE_LARGE, ModelComponentType.CAB),
	BELL(AssemblyStep.SHELL, CraftingType.PLATE_SMALL, ModelComponentType.BELL),
	WHISTLE(AssemblyStep.SHELL, CraftingType.PLATE_SMALL, ModelComponentType.WHISTLE),
	HORN(AssemblyStep.SHELL, CraftingType.PLATE_SMALL, ModelComponentType.HORN),
	
	// DIESEL
	FUEL_TANK(AssemblyStep.SHELL, CraftingType.PLATE_MEDIUM, ModelComponentType.FUEL_TANK),
	ALTERNATOR(AssemblyStep.SHELL, CraftingType.CASTING, ModelComponentType.ALTERNATOR),
	ENGINE_BLOCK(AssemblyStep.SHELL, CraftingType.CASTING, ModelComponentType.ENGINE_BLOCK),
	CRANKSHAFT(AssemblyStep.SHELL, CraftingType.CASTING, ModelComponentType.CRANKSHAFT),
	PISTON(AssemblyStep.SHELL, CraftingType.CASTING_HAMMER, ModelComponentType.PISTON_X),
	FAN(AssemblyStep.SHELL, CraftingType.PLATE_MEDIUM, ModelComponentType.FAN_X),
	DRIVE_SHAFT(AssemblyStep.SHELL, CraftingType.CASTING, ModelComponentType.DRIVE_SHAFT_X),
	GEARBOX(AssemblyStep.SHELL, CraftingType.CASTING, ModelComponentType.GEARBOX),
	FLUID_COUPLING(AssemblyStep.SHELL, CraftingType.PLATE_MEDIUM, ModelComponentType.FLUID_COUPLING),
	FINAL_DRIVE(AssemblyStep.SHELL, CraftingType.PLATE_MEDIUM, ModelComponentType.FINAL_DRIVE),
	TORQUE_CONVERTER(AssemblyStep.SHELL, CraftingType.CASTING, ModelComponentType.TORQUE_CONVERTER),
	
	//STEAM
	FIREBOX(AssemblyStep.BOILER, CraftingType.PLATE_LARGE, ModelComponentType.FIREBOX),
	SMOKEBOX(AssemblyStep.BOILER, CraftingType.PLATE_LARGE, ModelComponentType.SMOKEBOX),
	STEAM_CHEST(AssemblyStep.FRAME, CraftingType.CASTING, ModelComponentType.STEAM_CHEST),
	STEAM_CHEST_POS(AssemblyStep.FRAME, CraftingType.CASTING, ModelComponentType.STEAM_CHEST_POS),
	BOILER_SEGMENT(AssemblyStep.BOILER, CraftingType.PLATE_BOILER, ModelComponentType.BOILER_SEGMENT_X),
	PIPING(AssemblyStep.BOILER, CraftingType.PLATE_LARGE, ModelComponentType.PIPING),
	
	// WALCHERTS
	WHEEL_DRIVER(AssemblyStep.WHEELS, CraftingType.CASTING, ModelComponentType.WHEEL_DRIVER_X),
	WHEEL_DRIVER_POS(AssemblyStep.WHEELS, CraftingType.CASTING, ModelComponentType.WHEEL_DRIVER_POS_X), // MALLET

	CYLINDER(AssemblyStep.FRAME, CraftingType.CASTING_HAMMER, ModelComponentType.CYLINDER_SIDE),
	SIDE_ROD(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, ModelComponentType.SIDE_ROD_SIDE),
	MAIN_ROD(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, ModelComponentType.MAIN_ROD_SIDE),
	PISTON_ROD(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, ModelComponentType.PISTON_ROD_SIDE),
	
	UNION_LINK(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, ModelComponentType.UNION_LINK_SIDE),
	COMBINATION_LEVER(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, ModelComponentType.COMBINATION_LEVER_SIDE),
	VALVE_STEM(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, ModelComponentType.VALVE_STEM_SIDE),
	RADIUS_BAR(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, ModelComponentType.RADIUS_BAR_SIDE),
	EXPANSION_LINK(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, ModelComponentType.EXPANSION_LINK_SIDE),
	ECCENTRIC_ROD(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, ModelComponentType.ECCENTRIC_ROD_SIDE),
	ECCENTRIC_CRANK(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, ModelComponentType.ECCENTRIC_CRANK_SIDE),
	REVERSING_ARM(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, ModelComponentType.REVERSING_ARM_SIDE),
	LIFTING_LINK(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, ModelComponentType.LIFTING_LINK_SIDE),
	REACH_ROD(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, ModelComponentType.REACH_ROD_SIDE),
	
	// LEGACY, how do we depricate this??
	WALCHERTS_LINKAGE(AssemblyStep.VALVE_GEAR, CraftingType.CASTING_HAMMER, 
			ModelComponentType.UNION_LINK_SIDE,
			ModelComponentType.COMBINATION_LEVER_SIDE,
			ModelComponentType.VALVE_STEM_SIDE,
			ModelComponentType.RADIUS_BAR_SIDE,
			ModelComponentType.EXPANSION_LINK_SIDE,
			ModelComponentType.ECCENTRIC_ROD_SIDE,
			ModelComponentType.ECCENTRIC_CRANK_SIDE,
			ModelComponentType.REVERSING_ARM_SIDE,
			ModelComponentType.LIFTING_LINK_SIDE,
			ModelComponentType.REACH_ROD_SIDE
	),

	FRONT_SHELL(AssemblyStep.SHELL, CraftingType.PLATE_LARGE, ModelComponentType.FRONT_SHELL),
	REAR_SHELL(AssemblyStep.SHELL, CraftingType.PLATE_LARGE, ModelComponentType.REAR_SHELL),
	;
	
	public final AssemblyStep step;
	public final List<ModelComponentType> render;
	public final CraftingType crafting;

	ItemComponentType(AssemblyStep step, CraftingType crafting, ModelComponentType... render) {
		this.crafting = crafting;
		this.step = step;
		this.render = Arrays.asList(render);
	}
	
	public boolean isWheelPart() {
		switch (this) {
		case BOGEY_FRONT_WHEEL:
		case BOGEY_FRONT:
		case BOGEY_REAR_WHEEL:
		case BOGEY_REAR:
		case WHEEL_DRIVER:
		case WHEEL_DRIVER_POS:
		case FRAME_WHEEL:
			return true;
		default:
			return false;
		}
	}

	public static ItemComponentType from(ModelComponentType renderComponent) {
		for (ItemComponentType item : values()) {
			for (ModelComponentType render : item.render) {
				if (render == renderComponent) {
					return item;
				}
			}
		}
		
		return null;
	}

	@Override
	public String toString() {
		return TextUtil.translate("part.immersiverailroading:component." + super.toString().toLowerCase(Locale.ROOT));
	}

	public int getPlateCost(Gauge gauge, EntityRollingStockDefinition definition) {
		ModelComponent comp = definition.getComponents(this.render.get(0)).get(0);
		
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
		size *= Math.pow(gauge.scale(), 3);
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

	// TODO average the component sizes instead of just looking at the first element
	public int getCastCost(EntityRollingStockDefinition definition, Gauge gauge) {
		if (definition == null) {
			return ItemCastingCost.BAD_CAST_COST;
		}
		List<ModelComponent> components = definition.getComponents(this.render.get(0));
		if (components == null) {
			return ItemCastingCost.BAD_CAST_COST;
		}
		ModelComponent comp = components.get(0);
		double densityGues = 0.6;
		return (int) Math.ceil(comp.width() * comp.height() * comp.length() * densityGues * Math.pow(gauge.scale(), 3));
	}

	public int getWoodCost(Gauge gauge, EntityRollingStockDefinition definition) {
		ModelComponent comp = definition.getComponents(this.render.get(0)).get(0);
		double densityGues = 4;
		return (int) Math.ceil(comp.width() * comp.height() * comp.length() * densityGues * Math.pow(gauge.scale(), 3));
	}
	
	public boolean isWooden(EntityRollingStockDefinition definition) {
		ModelComponent component = definition.getComponents(this.render.get(0)).get(0);
		return component.wooden;
	}
}
